#  初识NIO

## IO模型

在java中有三种IO模型

### BIO 同步阻塞

阻塞式IO，在java1.4之前通过InputStream、OutputStream实现。对于每个网络请求都需要创建一个线程，因为需要消耗资源，所以不适合高并发场景，优点时稳定。

### NIO 同步非阻塞

基于BIO的问题，java1.4之后加入了NIO.实现原理时基于多路复用选择器来监测（双向管道）连接状态并通知线程处理。

### AIO 异步非阻塞

NIO的非阻塞时基于多路复用选择器的对管道的轮询实现的。当管道数目较多，性能会被影响。在java1.7之后引入了异步非阻塞式IO，通过一部回调的方式代替选择器。AIO和NIO的优化在Windows系统明显，在Linux系统不明显。



## NIO的基础组件

NIO使用了一个双向管道代替了BIO的InputStream和OutputStream，并且管道必须搭配个缓冲区来使用实现通信。

![image-20210328193934672](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210328193934.png?token=AGSD2IDA3OS3VXZTOLLET6TAMBVTI)

### 缓冲区Buffer

Buffer是一种数据容器。Buffer只能支持存储基本类型（不包含String）。

### 管道Channel

Channel用于连接文件、网络等，可以同时执行读取和写入的IO操作，所以称为双向管道。

- FileChannel：(同时支持读写的话要用`RandomAccessFile`的读写模式打开文件)
  - 1.支持对文件指定区域的读写；
  - 2.直接映射JVM声明之外的内存，提升大文件读写效率；
  - 3.零拷贝技术，`transferFrom`、`transferTo`传输文件到管道中;
- DatagramChannel（UDP套接字管道）：

- ServerSocketChannel （建立连接）和SocketChannel （进行通信）

```java
//1.打开TCP服务管道
ServerSocketChannel channel = ServerSocketChannel.open();
//2.绑定端口
channel.bind(new IntelSocketAddress(8080));
//3.接收客户端发送的请求连接,没有人连接会阻塞
SocketChannel socketChannel = channel.accept();
ByteBuffer buffer = ByteBuffer.allocate(1024);
//4.读取客户端传来的数据进缓冲区,没有数据会阻塞
socketChannel.read(buffer);
//5.回写消息到缓冲区并通过管道传回
socketChannel.write(ByteBuffer.wrap("返回消息".getBytes()));
//6.关闭管道
socketChannel.close();
```

