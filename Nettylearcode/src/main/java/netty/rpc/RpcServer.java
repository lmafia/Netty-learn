package netty.rpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import jdk.internal.org.objectweb.asm.Type;

import javax.xml.ws.Dispatch;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * @author L_MaFia
 * @classname RpcServer.java
 * @description TODO
 * @date 2021/4/18
 */
public class RpcServer {
    private Map<String, ServiceBean> register = new HashMap<>();
    private static DefaultEventExecutorGroup eventExecutors = new DefaultEventExecutorGroup(256);

    public void start(int port) throws InterruptedException {

        ServerBootstrap bootstrap = new ServerBootstrap();
        NioEventLoopGroup boss = new NioEventLoopGroup(1);
        NioEventLoopGroup worker = new NioEventLoopGroup(8);
        bootstrap.group(boss, worker);
        bootstrap.channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline()
                        .addLast("codec", new RpcCodec())
                        .addLast("dispatch", new Dispatch());

            }
        });
        bootstrap.bind(port).sync();
        System.out.println("服务启动成功");

    }

    public class Dispatch extends SimpleChannelInboundHandler<Transfer> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Transfer msg) throws Exception {
            if (msg.heartbeat) {
                Transfer transfer = new Transfer(msg.id);
                transfer.heartbeat = true;
                transfer.request = false;
                ctx.channel().writeAndFlush(msg);
            } else {
                eventExecutors.submit(() -> {
                    Transfer to = doDispatch(msg);
                    ctx.channel().writeAndFlush(to);
                });
            }
        }

        Transfer doDispatch(Transfer from) {
            Request request = (Request) from.target;
            Transfer to = new Transfer(from.id);
            to.request = false;
            to.heartbeat = false;
            to.serializableId = from.serializableId;
            Response response = new Response();
            try {
                String serverId = request.getClassName() + request.getMethodDesc();
                ServiceBean serviceBean = register.get(serverId);
                if (serviceBean == null) {
                    throw new IllegalArgumentException("找不到服务" + serverId);
                }
                Object result = serviceBean.invoke(request.getArgs());
                response.setResult(result);
                to.status = Transfer.STATUS_OK;
            } catch (Exception e) {
                e.printStackTrace();
                response.setError(e);
                to.status = Transfer.STATUS_ERROR;
            }
            to.target = response;
            return to;
        }
    }


    public void registerService(Class serviceInterface, Object serviceBean) {
        assert serviceInterface.isInterface();
        for (Method method : serviceInterface.getMethods()) {
            int modifiers = method.getModifiers();
            if (Modifier.isStatic(modifiers) || Modifier.isNative(modifiers)) {
                continue;
            }
            String methodDescriptor = Type.getMethodDescriptor(method);
            String key = serviceInterface.getName() + method.getName() + methodDescriptor;
            register.put(key, new ServiceBean(method, serviceBean));

        }
    }


    private class ServiceBean {
        Method method;
        Object target;

        public ServiceBean(Method method, Object target) {
            this.method = method;
            this.target = target;
        }

        public Object invoke(Object[] args) throws Exception {
            return method.invoke(target, args);
        }
    }


}
