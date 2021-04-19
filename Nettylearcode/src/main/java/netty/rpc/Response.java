package netty.rpc;

import java.io.Serializable;

/**
 * @author L_MaFia
 * @classname Respone.java
 * @description TODO
 * @date 2021/4/18
 */
public class Response implements Serializable {
    private Object result;
    private Throwable error;

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public Throwable getError() {
        return error;
    }

    public void setError(Throwable error) {
        this.error = error;
    }

}
