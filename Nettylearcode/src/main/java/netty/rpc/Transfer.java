package netty.rpc;

/**
 * @author L_MaFia
 * @classname Transfer.java
 * @description RPC的传输对象
 * @date 2021/4/18
 */
public class Transfer {
    public final static byte STATUS_ERROR = 0;
    public final static byte STATUS_OK = 1;
    public final static byte STATUS_ILLEGAL = 2;

    public final static byte SERIALIZABLE_JAVA = 1;
    public final static byte SERIALIZABLE_HESSIAN = 2;
    public final static byte SERIALIZABLE_JSON = 3;

    protected boolean request;
    protected byte serializableId;
    protected boolean twoWay;
    protected boolean heartbeat;
    protected long id;
    /**
     * 1.正常 0.失败 2.不合法
     */
    protected byte status;
    protected Object target;

    public Transfer(long id) {
        this.id = id;
    }

    void copy(Transfer from) {
        this.request = from.request;
        this.serializableId = from.serializableId;
        this.twoWay = from.twoWay;
        this.heartbeat = from.heartbeat;
        this.id = from.id;
        this.status = from.status;
        this.target = from.target;
    }


}
