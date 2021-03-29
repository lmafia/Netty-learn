package echo;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * @author L_MaFia
 * @classname EchoServer.java
 * @description TODO
 * @date 2021/3/29
 */
public class EchoServer {
    // 需求：心跳服务，按服务
    @Test
    public void serverTest() throws IOException {
        ServerSocketChannel serverListener = ServerSocketChannel.open();
        serverListener.bind(new InetSocketAddress(8080));
        serverListener.configureBlocking(false);
        Selector selector = Selector.open();
        serverListener.register(selector, SelectionKey.OP_ACCEPT);
        try {
            dispatch(selector, serverListener);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
            System.out.println("关闭");
        }

    }

    private void dispatch(Selector selector, ServerSocketChannel serverListener) throws IOException {
        while (true) {
            int count = selector.select(1000);
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey key = iterator.next();
                iterator.remove();
                if (!key.isValid()) {
                    continue;
                } else if (key.isAcceptable()) {
                    SocketChannel socketChannel = serverListener.accept();
                    socketChannel.configureBlocking(false);
                    socketChannel.register(selector, SelectionKey.OP_READ);
                } else if (key.isReadable()) {
                    SocketChannel channel = (SocketChannel) key.channel();
                    ByteBuffer buffer = ByteBuffer.allocate(64);
                    channel.read(buffer);
                    if (buffer.hasRemaining() && buffer.get(0) == '4') {// 传输结束
                        channel.close();
                        System.out.println("关闭管道：" + channel.socket());
                        break;
                    }
                    buffer.put(String.valueOf(System.currentTimeMillis()).getBytes());
                    buffer.flip();
                    System.out.println(new String(buffer.array()));
                    channel.write(buffer);
                }
            }
        }


    }


}
