package netty.rpc;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author L_MaFia
 * @classname RpcTest.java
 * @description TODO
 * @date 2021/4/20
 */
public class RpcTest {
    @Test
    public void startServerTest() throws InterruptedException, IOException {
        RpcServer server = new RpcServer();
        server.registerService(UserService.class, new UserServiceImpl());
        server.start(8080);
        System.in.read();
    }

    public static void main(String[] args) throws InterruptedException, IOException {
        RpcClient client = new RpcClient();
        client.init("127.0.0.1", 8080);
        UserService service = client.getRemoteService(UserService.class);


        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            String s = in.readLine();
            System.out.println(service.getUser(1));

        }
    }

    // 多线程并发调用
    @Test
    public void syncTest() throws InterruptedException, IOException {

        ExecutorService executor = Executors.newFixedThreadPool(100);
        for (int i = 0; i < 500; i++) {
            RpcClient client = new RpcClient();
            client.init("127.0.0.1", 8080);
            UserService service = client.getRemoteService(UserService.class);
            int id = i;
            executor.execute(() -> {
                User user = service.getUser(id);
                System.out.println(user);
                assert user.getId().equals(id);
            });
        }
        System.in.read();
    }
}
