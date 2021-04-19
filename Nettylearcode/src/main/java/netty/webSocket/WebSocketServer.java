package netty.webSocket;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;

/**
 * @author L_MaFia
 * @classname BroadcastServer.java
 * @description TODO
 * @date 2021/4/14
 */
public class WebSocketServer {

    private ByteBuf page;
    private ChannelGroup channels;
    {
        try {
            initialStaticPage();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openServer(int port) throws IOException, URISyntaxException, InterruptedException {
        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(16);
        ServerBootstrap group = bootstrap.group(boss, worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("decode",new HttpRequestDecoder())
                        .addLast("aggregate",new HttpObjectAggregator(65536))
                        .addLast("encode", new HttpResponseEncoder())
                        .addLast("http-servlet", new MyServletHandler())
                        .addLast("ws-codec", new WebSocketServerProtocolHandler("/ws"))
                        .addLast("ws-servlet", new MyWsServletHandler());
            }
        });
        ChannelFuture future = bootstrap.bind(port).sync();
        future.addListener(v -> System.out.println("绑定成功"));
        channels = new DefaultChannelGroup(future.channel().eventLoop());

    }

    private void initialStaticPage() throws URISyntaxException, IOException {
        URL location = WebSocketServer.class.getProtectionDomain().getCodeSource().getLocation();
        String path = location.toURI() + "Websocket.html";
        path = !path.contains("file:") ? path : path.substring(6);
        RandomAccessFile file = new RandomAccessFile(path, "r");
        ByteBuffer buffer = ByteBuffer.allocate((int) file.length());
        file.getChannel().read(buffer);
        buffer.flip();
        page = Unpooled.wrappedBuffer(buffer);
        file.close();
    }
    private class MyServletHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest req) throws Exception {
            //返回webSocket.html
            if (req.uri().equals("/")) {
                DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,HttpResponseStatus.OK);
                response.headers().set(HttpHeaderNames.CONTENT_TYPE,"text/html; charset=UTF-8")
                        .set(HttpHeaderNames.CONTENT_LENGTH, page.capacity());
                response.content().writeBytes(page.duplicate());
                ctx.channel().writeAndFlush(response);
                // 传递给下一个Inbound
            } else if (req.uri().equals("/ws")) {
                ctx.fireChannelRead(req.retain());
            }
        }
    }

    private class MyWsServletHandler extends SimpleChannelInboundHandler<TextWebSocketFrame> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, TextWebSocketFrame msg) throws Exception {
            System.out.println(msg.text());
            if (msg.text().equals("add")) {
                channels.add(ctx.channel());
            }
            channels.writeAndFlush(new TextWebSocketFrame(msg.text()));
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, URISyntaxException {
        WebSocketServer webSocketServer = new WebSocketServer();
        webSocketServer.openServer(8080);
    }

}
