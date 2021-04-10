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

为了防止阻塞，该线程模型下，请求连接建立、I/O读写在一个Reactor线程完成，另外业务处理在一个线程池中异步处理完成，处理完再回写。

![image-20210410191451273](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410191451.png?token=AGSD2IFDGFPTO65G5HGDO43AOGEOS)



#### 3.主从Reactor多线程模型

因为单Reactor还能不能榨干CPU多核的能力，所以可以建立多个Reactor线程。该线程模型下，有一主多从Reactor。主Reactor是进行请求连接建立，从Reactor们用于处理I/O读写，业务处理同样还是在一个线程池中异步处理。

![image-20210410192257512](https://raw.githubusercontent.com/lmafia/private-picture-could/main/20210410192257.png?token=AGSD2IFNZWP7L6JHNYC4MJLAOGFM6)