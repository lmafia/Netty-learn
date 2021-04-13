# Netty的线程模型

## Reactor模型

Netty的模型就是基于Reactor（反应堆）模式实现的，所以一定要先认识下Reactor。

Reactor模型也叫Dispatch（派遣）模型。当一个或多个请求同时传给服务端，服务端将它们**同步**分派给各个请求的处理线程。

### Reactor模型的三种角色
- Acceptor：处理客户端新连接，并分配请求到处理链中
- Reactor：负责监听和分配事件，将I/O事件分配给对应的Handler
- Handler：事件处理，如编码、业务处理、解码等

### Reactor的线程模型
一共有三种模型：单Reactor单线程模型、单Reactor多线程模型、主从Reactor多线程模型。
Netty使用的是主从Reactor多线程模型。

#### 1.单Reactor单线程模型

该线程模型下，所有的请求连接建立、I/O读写、业务处理都是在一个线程完成的。如果业务处理出现了耗时操作，因为在一个线程上所有操作时同步的，就会导致所有请求都会延时处理，造成阻塞。

![image-20210410191502467](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410191502.png?token=AGSD2IHEZ527W2R2LRTE3A3AOGEPI)

#### 2.单Reactor多线程模型

为了防止阻塞，该线程模型下，请求连接建立（包括授权认证等）、I/O读写在一个Reactor线程完成，另外业务处理在一个线程池中异步处理完成，处理完再回写。

![image-20210410191451273](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410191451.png?token=AGSD2IFDGFPTO65G5HGDO43AOGEOS)



#### 3.主从Reactor多线程模型

因为单Reactor还能不能榨干CPU多核的能力，所以可以建立多个Reactor线程。该线程模型下，有一主多从Reactor。主Reactor是进行请求连接建立（包括授权认证等），从Reactor们用于处理I/O读写，业务处理同样还是在一个线程池中异步处理。

![image-20210410192257512](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410192257.png?token=AGSD2IFNZWP7L6JHNYC4MJLAOGFM6)

### Netty的线程模型

![image-20210410203400672](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410203400.png?token=AGSD2IFPH7YRNCPVVJ5XQMLAOGNXM)

#### 成员组成

Netty线程模型组成大概有ServerBootStrap、NioEventLoopGroup和它的成份NioEventLoop、NioChannel、ChannelPipeline、ChannelHandler等等。

Netty采用的是主从Reactor多线程模型来实现，主Reactor就对应的是BossGroup，从Reactor对应是WorkerGroup。BossGroup用于接收连接，并把建立好的连接通过Channel方式注册到WorkerGroup，当IO事件触发时，由Pipeline来处理，Pipeline由对应的Handler来实际处理。

#### EventLoop

> 事件循环器

从名字可以得知它其实是一个不断循环的进程。就是相当于之前nio模型的`while(true)`里的那对代码段。

EvnetLoop里主要由一个Selector多路复用选择器来处理IO事件的，和一个TaskQueue存储提交的任务。

EventLoop的启动方式是初始时不启动，当有任务提交过来，就启动处理任务，并一直跑一下去。

##### EventLoop相关源码（删减）

```java
 @Override
    protected void run() {
        int selectCnt = 0;
        for (;;) {
            try {
                int strategy;
                try {
                    strategy = selectStrategy.calculateStrategy(selectNowSupplier, hasTasks());
                    switch (strategy) {
                    case SelectStrategy.CONTINUE:

                    case SelectStrategy.BUSY_WAIT:

                    case SelectStrategy.SELECT:
                }
                boolean ranTasks;
                if (ioRatio == 100) {
                    try {
                        if (strategy > 0) {
                            processSelectedKeys();
                        }
                    } finally {
                        // Ensure we always run tasks.
                        ranTasks = runAllTasks();
                    }
                } else if (strategy > 0) {
                    final long ioStartTime = System.nanoTime();
                    try {
                        processSelectedKeys();
                    } finally {
                        // Ensure we always run tasks.
                        final long ioTime = System.nanoTime() - ioStartTime;
                        ranTasks = runAllTasks(ioTime * (100 - ioRatio) / ioRatio);
                    }
                } else {
                    ranTasks = runAllTasks(0); // This will run the minimum number of tasks
                }
            }
        }
    }
```

##### 提交任务

```java
    @Test
    public void test() {
        //构建EventLoopGroup 线程数 为1,就是只有一个EventLoop
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //下面证明eventLoop是一个线程池,提交的任务的都是同一个线程处理的
        group.execute(() -> System.out.println("L_MaFia create the EventLoopGroup " + Thread.currentThread().getId()));
        Future<?> submit = group.submit(() -> System.out.println("submit task " + Thread.currentThread().getId()));
        submit.addListener(future -> System.out.println("完成任务啦"+Thread.currentThread().getId()));
        Scanner scanner = new Scanner(System.in);
        scanner.nextInt();
        group.shutdownGracefully();
    }
```

##### 处理Channel的IO事件

- 处理Channel 相关IO事件。如管道注册等。调用register方法最终会调用NIO当中的register方法进行注册，只不过Netty已经实现封装好了，并且处理好线程安全的问题。
- 原有的java原生的NIO Channel 被封装成了NioChannel，当然最终底层还是在调用NIO Channel。原来对Channel 中读写事件处理被封装成Channelhandler进行处理，并用引入Pipeline的概念。

```java
ChannelFuture register = group.register(channel);
看 NioChannel 用法 的代码
```



#### Channel

netty对java的原生Channel进行封装了。并且给其添加了Pipeline和ChannelHandler的概念。Channel的具体IO事件处理就是通过Pipeline和ChanneHandler（它Pipeline里）来处理的。

![image-20210412073724333](https://cdn.jsdelivr.net/gh/lmafia/private-picture-could@master/20210412073724.png)

##### NioChannel 用法

1. 初始化Channel

   初始化操作与原生NIO类似，都是打开管道、注册选择器最后绑定端口。但有一点要说明NioChannel当中所有操作都是在EventLoop中完成的，所以在绑定端口之前必须先注册。

```java
	NioDatagramChannel channel = new NioDatagramChannel();
	ChannelFuture register = group.register(channel);
	ChannelFuture future = channel.bind(new InetSocketAddress(8080));
```

2. 初始化Pipeline

   原生NIO 是直接遍历选择集（`SeletionKey`）然后处理读写事件，在Netty中直接处理读写是**不安全**的，而是**采用ChannelHandler来间接的处理读写事件**。一般情况下读写是有多个步骤的。Netty中提供了Pipeline来组织这些ChannelHandler。Pipeline是一个链表容器，可以通过addFirst、addLast 在首尾增加Handler。

   `SimpleChannelInboundHandler()`是netty提供的一种处理读事件的Handler

   ```java
           channel.pipeline().addLast(new SimpleChannelInboundHandler() {
               @Override
               protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
                   if (msg instanceof DatagramPacket) {
                       System.out.println(System.currentTimeMillis()+ "_:" + ((DatagramPacket) msg).content().toString(Charset.defaultCharset()));
                   }
               }
           });
   ```

   

测试Demo

```java
    @Test
    public void test(){
        //1.创建NioEventGroup
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        //2.创建Channel
        //原有的java原生的NIO Channel 被封装成了NioChannel，当然最终底层还是在调用NIO Channel。原来对Channel 中读写事件处理被封装成Channelhandler进行处理，并用引入Pipeline的概念。
        NioDatagramChannel channel = new NioDatagramChannel();
        //3.用EventLoopGroup 注册 一个Channel，相当于给group提交一个任务，返回一个Future作为结果回调.
        //Netty 为了安全的调用IO操作，把所有对IO的直接操作都封状了一个任务，交由IO线程执行。所以我们通过Netty来调用IO是不会立即返回的
        ChannelFuture register = group.register(channel);
        register.addListener(future -> System.out.println("完成注册"));
        //4.将Channel绑定端口
        ChannelFuture future = channel.bind(new InetSocketAddress(8080));
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
```

### 常规Netty用法

常规的用法就不需要老是为主Channel初始化，又要对子Channel初始化。Netty提供了ServerBootStrap封装了上面的注册、绑定、初始化等操作。简化了对Netty API的调用。

1. 初始化
   通过ServerBootStrap可以直接设定线程组，boss组用于处理NioServerSocketChannel的Accept事件，来处理请求的连接与认证等，worker组用于处理I/O读写事件，以及处理提交的异步任务。可以通过指定Channl.class来告诉BootStrap需要维护的管道。

```java
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup work = new NioEventLoopGroup(8);
        bootstrap.group(boss, work).channel(NioServerSocketChannel.class);
```




2. 设置子管道的Pipeline

   初始化子管道（TCP里就是SocketChannel）的Pipeline，ChannelInitializer，并为其添加对应Handler。

```java
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new MyChannelHandler());
            }
        });
```

3. 业务处理

这里我们继承SimpleChannelInboundHandler来实现读事件的业务处理。

4. 绑定端口

   为ServerBootStrap绑定端口

```java
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080)).sync();
        future.addListener((future1) -> System.out.println("完成绑定"));
```





```java
    @Test
    public void test() throws InterruptedException {
        //指定要打开的管道 自动进行进行注册==》NioServerSocketChannel ->        .channel(NioServerSocketChannel.class)
        ServerBootstrap bootstrap = new ServerBootstrap();
        EventLoopGroup boss = new NioEventLoopGroup(1);
        EventLoopGroup work = new NioEventLoopGroup(8);
        bootstrap.group(boss, work).channel(NioServerSocketChannel.class);

        //5.为Channel的pipeline 添加 Handler
        bootstrap.childHandler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel ch) throws Exception {
                ch.pipeline().addLast(new MyChannelHandler());
            }
        });
        ChannelFuture future = bootstrap.bind(new InetSocketAddress(8080)).sync();
        future.addListener((future1) -> System.out.println("完成绑定"));
        Scanner scanner = new Scanner(System.in);
        scanner.nextInt();
    }


    class MyChannelHandler extends SimpleChannelInboundHandler {
        @Override
        protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof ByteBuf) {
                System.out.println(System.currentTimeMillis() + "_:" + ((ByteBuf) msg).toString(Charset.defaultCharset()));
                ChannelFuture channelFuture = ctx.channel().writeAndFlush(Unpooled.copiedBuffer(System.currentTimeMillis() + "_:" + ((ByteBuf) msg).toString(Charset.defaultCharset()), Charset.defaultCharset()));
                channelFuture.addListener(future -> System.out.println("写出成功"));
            }
        }
    }
```

### 总结：
1. Netty使用的是主从Reactor多线程模型
2. EventLoop是Netty处理请求、IO事件等操作的检测与分配
3. Netty引入了Channel、Pipeline、ChannelHandler来异步处理任务
4. Netty提供了ServerBootStrap来简化管道初始化