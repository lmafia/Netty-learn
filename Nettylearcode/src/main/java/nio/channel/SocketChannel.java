package nio.channel;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;

/**
 * @author L_MaFia
 * @classname SocketChannel.java
 * @description TODO
 * @date 2021/3/29
 */
public class SocketChannel {
    @Test
    public void test() throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.bind(new InetSocketAddress(8080));
        // 1.建立连接

        // 2.通信
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        while (true) {
            handle(channel.accept());
        }
    }


    public void handle(final java.nio.channels.SocketChannel socketChannel) throws IOException {
        // 2.通信
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                ByteBuffer buffer = ByteBuffer.allocate(8192);
                while (true) {
                    try {
                        buffer.clear();
                        socketChannel.read(buffer);
                        // 从buffer 当中读出来
                        buffer.flip();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);
                        String message = new String(bytes);
                        System.out.println(message);
                        // 写回去
                        buffer.rewind();
                        socketChannel.write(buffer);
                        if (message.trim().equals("exit")) {
                            break;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                try {
                    socketChannel.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
