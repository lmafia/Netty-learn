package netty.rpc;

import java.io.Serializable;

/**
 * @author L_MaFia
 * @classname Request.java
 * @description TODO
 * @date 2021/4/18
 */
public class Request implements Serializable {
    private String methodDesc;
    private String className;
    private Object[] args;

    public Request(String className,String methodDesc) {
        this.className=className;
        this.methodDesc=methodDesc;
    }

    public String getMethodDesc() {
        return methodDesc;
    }

    public void setMethodDesc(String methodDesc) {
        this.methodDesc = methodDesc;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public Object[] getArgs() {
        return args;
    }

    public void setArgs(Object[] args) {
        this.args = args;
    }
}
