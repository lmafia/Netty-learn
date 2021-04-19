package netty.rpc;
/**
 * @Copyright 源码阅读网 http://coderead.cn
 */

import java.util.Date;

/**
 * @author 鲁班大叔
 * @date 2020/7/31 00:04
 */
public class User implements java.io.Serializable {
    Integer id;
    String name;
    String sex;
    Date createTime;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", sex='" + sex + '\'' +
                ", createTime=" + createTime.getTime() +
                '}';
    }
}
