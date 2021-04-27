package netty.zerocopy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.junit.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * @author L_MaFia
 * @classname ZeroTest.java
 * @description TODO
 * @date 2021/4/27
 */
public class ZeroTest {

    /*
     ***********************NIO***************************************
     */

    @Test
    public void nioZeroCopyTest() throws IOException {
        String file_name = "/Users/tommy/git/coderead-netty/target/2.4.二级缓存定义与需求分析.mp4";
        String copy_name = "/Users/tommy/git/coderead-netty/target/copy.mp4";
        File file = new File(copy_name);
        file.createNewFile();
        FileChannel channel = new RandomAccessFile(file_name, "rw").getChannel();
        FileChannel copyChannel = new RandomAccessFile(copy_name, "rw").getChannel();
        MappedByteBuffer mapped = channel.map(FileChannel.MapMode.READ_WRITE, 0, channel.size());
        copyChannel.write(mapped);

//        copyChannel.transferFrom(channel, 0, channel.size());
        channel.close();
        copyChannel.close();
    }

    /*
    ***********************Netty***************************************
     */

    @Test
    public void testUploadTest() throws InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(new NioEventLoopGroup(1),new NioEventLoopGroup(8));
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new Upload());
            }
        });
        ChannelFuture future = bootstrap.bind(8080).sync();
        future.sync().channel().closeFuture().sync();

    }

    private class Upload extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            System.out.println(msg);
            String fileName = "E:\\1.Java-learn\\6.code-read\\noteBook\\Netty\\Nettylearcode\\src\\main\\java\\netty\\zerocopy\\msg.txt";
            RandomAccessFile file = new RandomAccessFile(fileName, "rw");
            FileRegion fileRegion = new DefaultFileRegion(file.getChannel(),0,file.length());
            ctx.writeAndFlush(fileRegion);
            MappedByteBuffer map = file.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            ByteBuf byteBuf = Unpooled.wrappedBuffer(map);
            ctx.writeAndFlush(byteBuf);
        }
    }

    @Test
    public void testByNetty() {
        //DirectBuffer
        ByteBuf directBuf = Unpooled.directBuffer(1024);// 声明堆外内存 mmap
        ByteBuf heapBuf = Unpooled.buffer(1024);//声明堆内存
    }
}
