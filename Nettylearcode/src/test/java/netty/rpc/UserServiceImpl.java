package netty.rpc;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author 鲁班大叔
 * @date 2020/7/3112:03 AM
 */
public class UserServiceImpl implements UserService {
    Random random = new Random(1);

    @Override
    public User getUser(Integer id)  {
        User user = newUser(1).get(0);
        user.setId(id);
        try {
            Thread.sleep(random.nextInt(500));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return user;
    }


    @Override
    public List<User> findUser(String name) {
        return newUser(10);
    }

    private static List<User> newUser(int count) {
        List<User> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            User user = new User();
            user.setId(i);
            user.setName("lmafia" + i);
            user.setCreateTime(new Date());
            list.add(user);
        }
        return list;

    }
}
