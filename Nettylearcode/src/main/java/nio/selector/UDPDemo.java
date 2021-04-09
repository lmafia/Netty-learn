package nio.selector;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;

/**
 * @author L_MaFia
 * @classname UDPDemo.java
 * @description TODO
 * @date 2021/3/30
 */
public class UDPDemo {

    @Test
    public void test() throws IOException {
        Selector selector = Selector.open();
        DatagramChannel channel = DatagramChannel.open();
        channel.bind(new InetSocketAddress(8080));
        channel.configureBlocking(false);// 设置非阻塞模式
        // 1.注册读取就续事件
        channel.register(selector, SelectionKey.OP_READ);
        while (true) {
            //2.刷新键集
            int count = selector.select();
            if (count > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                // 3.遍历就续集
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    //4.从就续集中移除
                    iterator.remove();
                    //5.处理该就续键
                    handle(key);

                }
            }
        }
    }

    public void handle(SelectionKey key) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192);
        DatagramChannel channel = (DatagramChannel) key.channel();
        channel.receive(buffer);// 读取消息并写入缓冲区
    }
}