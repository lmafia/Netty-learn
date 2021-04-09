package netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.Future;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * @author L_MaFia
 * @classname EventLoopTest.java
 * @description TODO
 * @date 2021/4/4
 */
public class EventLoopTest {
    @Test
    public void test() {
        //构建EventLoop
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //下面证明eventLoop是一个线程池,提交的任务的都是同一个线程处理的
        group.execute(() -> System.out.println("L_MaFia create the EventLoopGroup " + Thread.currentThread().getId()));
        Future<?> submit = group.submit(() -> System.out.println("submit task " + Thread.currentThread().getId()));
        submit.addListener(future -> System.out.println("完成任务啦"+Thread.currentThread().getId()));
        Scanner scanner = new Scanner(System.in);
        scanner.nextInt();
        group.shutdownGracefully();
    }

    @Test
    public void test1() throws InterruptedException {
        //1.创建NioEventGroup
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //2.创建Channel
        NioDatagramChannel channel = new NioDatagramChannel();
        //3.用EventLoopGroup 注册 一个Channel，相当于给group提交一个任务，返回一个Future作为结果回调.
        ChannelFuture register = group.register(channel);
        register.addListener(future -> System.out.println("完成注册"));
        //4.将Channel绑定端口
        ChannelFuture future = channel.bind(new InetSocketAddress(8080)).sync();
        future.addListener((future1)-> System.out.println("完成绑定"));
        //5.为Channel的pipeline 添加 Handler
        channel.pipeline().addLast(new SimpleChannelInboundHandler() {
            @Override
            protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof DatagramPacket) {
                    System.out.println(System.currentTimeMillis()+ "_:" + ((DatagramPacket) msg).content().toString(Charset.defaultCharset()));
                }
            }
        });
        Scanner scanner = new Scanner(System.in);
        scanner.nextInt();
        group.shutdownGracefully();
    }
    @Test
    public void test2() throws InterruptedException {
        //1.创建NioEventGroup
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //2.创建Channel
        NioServerSocketChannel channel = new NioServerSocketChannel();
        //3.用EventLoopGroup 注册 一个Channel，相当于给group提交一个任务，返回一个Future作为结果回调.
        ChannelFuture register = group.register(channel);
        register.addListener(future -> System.out.println("完成注册"));
        //4.将Channel绑定端口
        ChannelFuture future = channel.bind(new InetSocketAddress(8080)).sync();
        future.addListener((future1)-> System.out.println("完成绑定"));
        //5.为Channel的pipeline 添加 Handler
        channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
            @Override
            //当读取到客户端连接事件
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                //获取socket channel
                NioSocketChannel socketChannel = (NioSocketChannel)msg;
                //使用了同一个group来注册channel
                ChannelFuture register = group.register(socketChannel);
                register.addListener((future)-> System.out.println("完成注册"));
                //为Channel的pipeline 添加 Handler
                socketChannel.pipeline().addLast(new SimpleChannelInboundHandler() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                        if (msg instanceof ByteBuf) {
                            System.out.println(System.currentTimeMillis()+ "_:" + ((ByteBuf) msg).toString(Charset.defaultCharset()));
                            ChannelFuture channelFuture = ctx.channel().writeAndFlush(Unpooled.copiedBuffer(System.currentTimeMillis() + "_:" + ((ByteBuf) msg).toString(Charset.defaultCharset()),Charset.defaultCharset()));
                            channelFuture.addListener(future -> System.out.println("写出成功"));
                        }
                    }
                });

            }
        });
        Scanner scanner = new Scanner(System.in);
        scanner.nextInt();
        group.shutdownGracefully();
    }
}
