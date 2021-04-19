package netty.rpc;

import com.sun.istack.internal.ByteArrayDataSource;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author L_MaFia
 * @classname RpcCodec.java
 * @description TODO
 * @date 2021/4/18
 */
public class RpcCodec extends ByteToMessageCodec<Transfer> {
    protected final static int HEADER_LENGTH = 16;
    protected final static short MAGIC = 0x0ABC;
    protected final static ByteBuf MAGIC_BUF = Unpooled.copyShort(MAGIC);
    protected final static byte FLAG_REQUEST = (byte) 0x80;
    protected final static byte FLAG_TWO_WAY = (byte) 0x40;
    protected final static byte FLAG_EVENT = (byte) 0x20;
    protected final static byte SERIALIZATION_MASK = (byte) 0x1F;




    @Override
    protected void encode(ChannelHandlerContext ctx, Transfer msg, ByteBuf out) throws Exception {
        doEncode(msg, out);
    }
    public void doEncode(Transfer msg, ByteBuf out) {
        //1.写入魔数
        byte[] header = new byte[HEADER_LENGTH];
        header[0] = (byte) (MAGIC >>> 8);
        header[1] = (byte) MAGIC;
        //2.是否为请求
        if (msg.request) {
            header[2] |= FLAG_REQUEST;
        }
        //3.是否为双向的
        if (msg.twoWay) {
            header[2] |= FLAG_TWO_WAY;
        }
        //4.是否为一个心跳事件
        if (msg.heartbeat) {
            header[2] |= FLAG_EVENT;
        }
        //5.写入序列化id 低5位
        header[2] |= msg.serializableId;
        //6.写入status  当是response才有状态返回
        if (!msg.request) {
            header[3] = msg.status;
        }


        //7.写入id
        header[4] = (byte)(msg.id >>> 56);
        header[5] = (byte)(msg.id >>> 48);
        header[6] = (byte)(msg.id >>> 40);
        header[7] = (byte)(msg.id >>> 32);
        header[8] = (byte)(msg.id >>> 24);
        header[9] = (byte)(msg.id >>> 16);
        header[10] = (byte)(msg.id >>> 8);
        header[11] = (byte)(msg.id);
        //8.写入body length
        //先把target对象序列化，再求得其长度
        byte[] body = new byte[0];
        int len = 0;
        if (!msg.heartbeat) {
            body = serialize(msg.serializableId, msg.target);
            len = body.length;
        }
        header[12] = (byte)(len >>> 24);
        header[13] = (byte)(len >>> 16);
        header[14] = (byte)(len >>> 8);
        header[15] = (byte)(len);
        // 将header 和 body 写入 ByteBuf
        out.writeBytes(header);
        out.writeBytes(body);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        Transfer transfer = doDecode(in);
        if (transfer != null) {
            out.add(transfer);
        }
    }

    //解码
    public Transfer doDecode(ByteBuf in) {
        //Magic 所处位置的第一次字节的位置
        ByteBuf magicBuf = MAGIC_BUF;
        int index = ByteBufUtil.indexOf(MAGIC_BUF,in);
        // 不包含magic 所以还需要更多的字节
        if (index < 0) {
            return null;
        }
        //当且缓冲区包含等于或大于 (magic + 16) 因为magic不一定从0开始的
        if (!in.isReadable(index + HEADER_LENGTH)) {
            return null;
        }
        // 获取整个请求头
        byte[] header = new byte[HEADER_LENGTH];
        ByteBuf slice = in.slice();
        slice.readBytes(header);
        //在请求头里获得body length
        int length = ((header[12 + 3] & 255)) + ((header[12 + 2] & 255) << 8) + ((header[12 + 1] & 255) << 16) + (header[12] << 24);
        //如果buf 里 还有没有整个消息体
//        if (!in.isReadable(index+ HEADER_LENGTH + headerLength)) {
        if (!slice.isReadable(length)){
            return null;
        }
        long id = (((long)header[4 + 7] & 255L) << 0) + (((long)header[4 + 6] & 255L) << 8) + (((long)header[4 + 5] & 255L) << 16) + (((long)header[4 + 4] & 255L) << 24) + (((long)header[4 + 3] & 255L) << 32) + (((long)header[4 + 2] & 255L) << 40) + (((long)header[4 + 1] & 255L) << 48) + ((long)header[4 + 0] << 56);
        Transfer transfer = new Transfer(id);
        transfer.request = (header[2] & FLAG_REQUEST) != 0;
        transfer.twoWay = (header[2] & FLAG_TWO_WAY) != 0;
        transfer.heartbeat = (header[2] & FLAG_EVENT) != 0;
        transfer.serializableId = (byte)(header[2] & SERIALIZATION_MASK);
        transfer.status = header[3];
        if (!transfer.heartbeat) {
            byte[] body = new byte[length];
            slice.readBytes(body);
            transfer.target = deserialize(transfer.serializableId, body);
        }
        //已经(消费过的)读过的字节就跳过了,避免重复读
        in.skipBytes(index + HEADER_LENGTH + length);
        return transfer;
    }

    /**
     *  序列化
     * @param serializableId
     * @param target
     * @return
     */
    private byte[] serialize(byte serializableId, Object target) {
        //JAVA类型才能序列化
        if (serializableId == Transfer.SERIALIZABLE_JAVA) {
            ByteArrayOutputStream out = null;
            try {
                out = new ByteArrayOutputStream();
                ObjectOutputStream stream = new ObjectOutputStream(out);
                stream.writeObject(target);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return out.toByteArray();
        } else {
            throw new UnsupportedOperationException();
        }
    }


    /**
     *  反序列化
     * @param serializableId
     * @param bytes
     * @return
     */
    private Object deserialize(byte serializableId, byte[] bytes) {
        if (serializableId == Transfer.SERIALIZABLE_JAVA) {
            try {
                ObjectInputStream stream =
                        new ObjectInputStream(new ByteArrayInputStream(bytes));
                return stream.readObject();
            } catch (IOException | ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
