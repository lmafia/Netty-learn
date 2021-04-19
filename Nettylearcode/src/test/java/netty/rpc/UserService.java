package netty.rpc;

import java.util.List;

public interface UserService {

    User getUser(Integer id) ;
    List<User> findUser(String name);

}
