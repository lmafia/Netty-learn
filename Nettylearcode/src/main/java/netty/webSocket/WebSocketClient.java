package netty.webSocket;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.SQLOutput;

/**
 * @author L_MaFia
 * @classname WebSocketClient.java
 * @description TODO
 * @date 2021/4/15
 */
//https://netty.io/4.0/xref/io/netty/example/http/websocketx/client/WebSocketClient.html
public class WebSocketClient {


    public void openClient(String ip, int port) throws InterruptedException, URISyntaxException, IOException {

        URI uri = new URI("ws://" + ip+ "/ws");
        MyWebClientHandler myWebClientHandler = new MyWebClientHandler(
                WebSocketClientHandshakerFactory.newHandshaker(
                        uri, WebSocketVersion.V13, null, false, new DefaultHttpHeaders()));
        final NioEventLoopGroup worker = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.group(worker);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast("codec", new HttpClientCodec())
                        .addLast("aggregate", new HttpObjectAggregator(65536))
                        .addLast("ws-client", myWebClientHandler);
            }
        });
        ChannelFuture connect = bootstrap.connect(ip, port).sync();
        Channel ch = connect.channel();
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        try {

            while (true) {
                String msg = console.readLine();
                if (msg == null) {
                    break;
                } else if ("bye".equals(msg.toLowerCase())) {
                    ch.writeAndFlush(new CloseWebSocketFrame());
                    ch.closeFuture().sync();
                    break;
                } else if ("ping".equals(msg.toLowerCase())) {
                    WebSocketFrame frame = new PingWebSocketFrame(Unpooled.wrappedBuffer(new byte[]{8, 1, 8, 1}));
                    ch.writeAndFlush(frame);
                } else {
                    WebSocketFrame frame = new TextWebSocketFrame(msg);
                    ch.writeAndFlush(frame);
                }
            }
        } finally {
            worker.shutdownGracefully();
        }

    }


    public class MyWebClientHandler extends SimpleChannelInboundHandler {
        private final WebSocketClientHandshaker handshaker;

        public MyWebClientHandler(final WebSocketClientHandshaker handshaker) {
            this.handshaker = handshaker;
        }

        @Override
        public void channelActive(final ChannelHandlerContext ctx) throws Exception {
            handshaker.handshake(ctx.channel());
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            Channel ch = ctx.channel();
            if (!handshaker.isHandshakeComplete()) {
                handshaker.finishHandshake(ch, (FullHttpResponse) msg);
                return;
            }
            if (msg instanceof FullHttpRequest) {
                return;
            }
            WebSocketFrame frame = (WebSocketFrame) msg;
            if (frame instanceof TextWebSocketFrame) {
                TextWebSocketFrame textFrame = (TextWebSocketFrame) msg;
                System.out.println("WebSocket Client received message: " + textFrame.text());
            } else if (frame instanceof PongWebSocketFrame) {
                System.out.println("WebSocket Client received pong");
                ch.writeAndFlush(new TextWebSocketFrame("heartbeat!"));
            } else if (frame instanceof CloseWebSocketFrame) {
                System.out.println("WebSocket Client received closing");
                ch.close();
            }
        }
    }

    public static void main(String[] args) {
        WebSocketClient webSocketClient = new WebSocketClient();
        try {
            webSocketClient.openClient("127.0.0.1", 8080);
        } catch (InterruptedException | IOException | URISyntaxException e) {
            e.printStackTrace();
        }
    }

}
