package com.github.niostar.audiopcmrecord.cmd;

/**
 * idp service -> rp service
 * to tell rp service is user reg success
 * if uaId==null -> reg fail
 * else reg success
 */
public class RpToClientResultCmd extends CMD {

    public boolean success;
    public String message;

    public RpToClientResultCmd() {
        super();
    }

    public RpToClientResultCmd(int code, boolean success, String message) {
        this.success = success;
        this.message = message;
        this.code = code;
    }
}
