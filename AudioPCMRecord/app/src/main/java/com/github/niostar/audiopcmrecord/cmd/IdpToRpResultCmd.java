package com.github.niostar.audiopcmrecord.cmd;


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
