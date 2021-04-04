package echo;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author L_MaFia
 * @classname EchoClient.java
 * @description TODO
 * @date 2021/4/4
 */
public class EchoClient {

    @Test
    public void test() throws IOException, InterruptedException {
        SocketAddress address;
        SocketChannel socketChannel = SocketChannel.open();
        socketChannel.configureBlocking(false);
        Selector selector = Selector.open();
        socketChannel.register(selector, SelectionKey.OP_CONNECT);
        //
        socketChannel.connect(new InetSocketAddress("127.0.0.1",8080));
        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey next = iterator.next();
                iterator.remove();
                if (!next.isValid()) {
                    continue;
                }
                if (next.isConnectable()) {
                    SocketChannel channel = (SocketChannel) next.channel();
                    System.out.println("是否创建连接" + channel.isConnected());
                    //需要调用finishConnect之后SocketChannel才会把连接状态置为true
                    channel.finishConnect();
                    System.out.println("是否创建连接" + channel.isConnected());
                    //把状态变为可写
                    next.interestOps(SelectionKey.OP_WRITE);
                } else if(next.isWritable()) {
                    SocketChannel channel = (SocketChannel) next.channel();
                    channel.write(ByteBuffer.wrap("heartbeat".getBytes()));
                    next.interestOps(SelectionKey.OP_READ);
                } else if(next.isReadable()) {
                    SocketChannel channel = (SocketChannel) next.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(64);
                    channel.read(buffer);
                    buffer.flip();
                    System.out.println(new String(buffer.array()));
                    next.interestOps(SelectionKey.OP_WRITE);
                    //读到心跳一定要休眠一下再进行下一次心跳请求哦
                    Thread.sleep(5000);
                }
            }
        }
    }
}
