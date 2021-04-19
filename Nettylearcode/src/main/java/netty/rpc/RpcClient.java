package netty.rpc;


import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.Promise;
import jdk.internal.org.objectweb.asm.Type;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author L_MaFia
 * @classname RpcClient.java
 * @description TODO
 * @date 2021/4/18
 */
public class RpcClient {
    private AtomicLong id = new AtomicLong(1L);
    private Channel channel;
    private Map<Long, Promise<Response>> resultMap = new HashMap<>();
    private long getNextid() {
        return id.incrementAndGet();
    }

    public void init(String ip, int port) throws InterruptedException {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup(1));
        bootstrap.channel(NioSocketChannel.class);
        bootstrap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline()
                        .addLast("codec", new RpcCodec())
                        .addLast("resultSet", new ResultSetFill());
            }
        });
        ChannelFuture future = bootstrap.connect(ip, port).sync();
        System.out.println("连接成功");
        channel = future.channel();
        channel.eventLoop().scheduleWithFixedDelay(()->{
            Transfer transfer = new Transfer(getNextid());
            transfer.heartbeat = true;
            transfer.request = true;
            channel.writeAndFlush(transfer);
        }, 2000,2000, TimeUnit.MILLISECONDS);

    }

    public Response invokeRemote(Class serviceInterface, String methodDesc, Object[] args) throws InterruptedException, ExecutionException, TimeoutException {
        Request request = new Request(serviceInterface.getName(), methodDesc);
        request.setArgs(args);
        Transfer transfer = new Transfer(getNextid());
        transfer.request = true;
        transfer.heartbeat = false;
        transfer.serializableId = Transfer.SERIALIZABLE_JAVA;
        transfer.target = request;
        DefaultPromise<Response> promise = new DefaultPromise<>(channel.eventLoop());
        channel.writeAndFlush(transfer).addListener(
                future -> {
                    if (future.cause() != null) {
                        promise.setFailure(future.cause());
                    } else {
                        resultMap.put(transfer.id, promise);
                    }
                }
        );
        return promise.get(5000, TimeUnit.MILLISECONDS);
    }


    private class ResultSetFill extends SimpleChannelInboundHandler<Transfer> {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Transfer msg) {
            if (msg.heartbeat) {
                System.out.println(String.format("服务端心跳返回：%s",
                        ctx.channel().remoteAddress()));
            } else {
                Promise<Response> promise = resultMap.remove(msg.id);
                promise.setSuccess((Response) msg.target); // 填充结果
            }
        }
    }

    public <T> T getRemoteService(Class<T> serviceInterface) {
        assert serviceInterface.isInterface();
        Object instance = Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{serviceInterface}, new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                if (Object.class.equals(method.getDeclaringClass())) {
                    return method.invoke(this, args);
                }
                String methodDescriptor = method.getName() + Type.getMethodDescriptor(method);
                Response response = invokeRemote(serviceInterface, methodDescriptor, args);
                if (response.getError() != null) {
                    throw new RuntimeException("远程服务调用异常", response.getError());
                }
                return response.getResult();
            }
        });
        return (T) instance;
    }
}
