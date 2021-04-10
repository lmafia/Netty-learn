- #  初识NIO

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

  ![image-20210328193934672](https://img-blog.csdnimg.cn/20210328220547459.png)

  NIO是如何做到非阻塞的呢？答案是因为NIO模型有个`多路复用选择器`

  管道会先向选择器注册，等到需要用到服务线程的时候才去调用。
  ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210330075656242.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTk3NDI2OQ==,size_16,color_FFFFFF,t_70)

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

  ### 选择器Selector

  选择器的核心组件：

  - SelectableChannel: 并非是所有的管道都能支持选择器注册的，只有`SelectableChannel`的子类才可以，所以FileChannel是不可以的。
  - Selector: 管道需要向选择器注册
  - SelectonKey：当管道注册到选择器的时，会返回一个Key。并且是通过这个key关联到哪个管道的。

  #### SelectableChannel

  核心方法`configureBlocking`方法 设置阻塞模式，默认为true，向选择器注册前必须置为false。第二就是是调用`register`方法注册到指定管道，并且指定要监听的事件。可选的事件有：

  OP_CONNECT(建立连接)、OP_ACCEPT(接受连接)、OP_READ(可读)、OP_WRITE(可写) 。但并非所有管道都支持这四事件，可通过validOps()来查看当前管道支持哪些事件

  - ServerSocketChannel  只支持OP_ACCEPT
  - SocketChannel 支持OP_CONNECT OP_READ OP_WRITE

  #### Selector 

  管道注册到选择器之后会生成一个Key，这个key在Seletor中keys数组里维护。Selector的`select`方法会刷新这些keys的状态，并且返回更新了状态的key的数量。

  - int select() : 会阻塞，直到有key更新了就立即返回数量。
  - int select( long ) : 阻塞到指定的毫秒时， 直到有key更新了就立即返回数量。

  - int selectNow() ： 不会阻塞，不论是否有key更新都会立即返回。

  #### SelectionKey

  键用于关联管道与选择器，并监听维护管道1至多个事件，监听事件可在注册时指定，也可后续通过调用SelectionKey.interestOps 来改变。

  ```java
  // 监听读取事件
  key=socketChannel.register(selector, SelectionKey.OP_READ);
  // 同时监听读写事件
  key.interestOps(SelectionKey.OP_READ|OP_WRITE);
  ```

  key 的主要方法有

  - 获取管道`channel()`

  - 判断状态，`isAcceptable()`，`isConnectable()`，`isReadable()`，`isWritable()`;

  - 判断该键是否有效，`isValid()`,当管道关闭、选择器关闭、取消键等操作都会导致该键无效。

  - 取消管道注册但不会关闭管道，`cancel()`

  #### 样例

  使用UDP管道DatagramChannel注册Selector来监听消息事件

  ```java
  public class UDPDemo {
  
      @Test
      public void test() throws IOException {
          //打开选择器
          Selector selector = Selector.open();
          //打开管道
          DatagramChannel channel = DatagramChannel.open();
          //管道监听端口
          channel.bind(new InetSocketAddress(8080));
          // ** 设置非阻塞模式
          channel.configureBlocking(false);
          // 1.管道向选择器注册读取就续事件
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
                      //5.处理该键
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
  ```

  

  #### 相关键集

  Selector中维护的相关键集，底层都是Set实现的：

  - 全部键集(keys)：所有向该选择器注册的键都放置于此

  - 选择键集(selectedKeys)：存放准备就续（例如可读）的键

  - 取消键集(cancelledKeys) ：存放已取消的键

  通过刷新或关闭选择器都会导致，键集发生变更。

  ![在这里插入图片描述](https://img-blog.csdnimg.cn/20210330075622998.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80MTk3NDI2OQ==,size_16,color_FFFFFF,t_70)


  - 调用select()会刷新键，将已就绪集添加至选择集中、清空取消键集并移除已取消键

  - 移除选择集，选择集不会被选择器移除，需自行调用Set.remove()进行移除

  - key的cancel()或关闭选择器，关闭管道都会都会将键添加至取消集，但其不会被立马清除，只有下一次刷新时才会被 清空 。