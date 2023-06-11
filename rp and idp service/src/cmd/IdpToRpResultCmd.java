package cmd;

/**
 * idp service -> rp service
 * to tell rp service is user reg success
 * if uaId==null -> reg fail
 * else reg success
 */
public class IdpToRpResultCmd extends CMD {

    public String uaId;
    public boolean success;

    public IdpToRpResultCmd() {
        super();
    }

    public IdpToRpResultCmd(int code, boolean success, String uaId) {
        this.uaId = uaId;
        this.code = code;
        this.success = success;
    }
}
