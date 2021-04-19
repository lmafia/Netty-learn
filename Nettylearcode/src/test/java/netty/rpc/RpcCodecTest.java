package netty.rpc;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import netty.rpc.Request;
import netty.rpc.Response;
import netty.rpc.RpcCodec;
import netty.rpc.Transfer;
import org.junit.Test;

/**
 * @author L_MaFia
 * @classname RpcCodecTest.java
 * @description TODO
 * @date 2021/4/19
 */
public class RpcCodecTest {
    @Test
    public void invokerTest() {
        //定义一个ByteBuf
        ByteBuf out = Unpooled.buffer(1024, 1024 * 1024 * 10);// 最大10M
        // 1.对请求进行编码
        Request request = encodeRequest(out);
        // 2.把请求进行解码
        Transfer from = decodeRequest(out);
        out.clear();
        // 3.对响应结果进行编码
        encodeResponse(from, out);
        // 4.把响应进行解码
        Transfer to = decodeResponse(out);
        System.out.println(from.id);
        System.out.println(to.id);
        assert from.id == to.id;
    }

    // 写入请求对象
    Request encodeRequest(ByteBuf out) {
        RpcCodec codec = new RpcCodec();
        Transfer transfer=new Transfer(2L);
        transfer.request=true;
        transfer.serializableId=Transfer.SERIALIZABLE_JAVA;
        Request request = new Request( "UserService", "getUser");
        request.setArgs(new Object[]{1, "name"});
        transfer.target = request;
        codec.doEncode(transfer, out); // 编码请求
        return request;
    }

    Transfer decodeRequest(ByteBuf out) {
        RpcCodec codec = new RpcCodec();
        return codec.doDecode(out);
    }

    Response encodeResponse(Transfer transfer, ByteBuf out) {
        Transfer to=new Transfer(transfer.id);
        to.request=false;
        to.serializableId=Transfer.SERIALIZABLE_JAVA;
        RpcCodec codec = new RpcCodec();
        Response response = new Response();
        response.setResult("返回结果");
        to.target = response;
        codec.doEncode(to, out);
        return response;
    }

    Transfer decodeResponse(ByteBuf out) {
        RpcCodec codec = new RpcCodec();
        return codec.doDecode(out);
    }


}
