package com.github.niostar.audiopcmrecord.cmd;

/**
 * user -> rp service to reg
 */
public class ClientToRpIdpCmd extends CMD {

    public String clientId;
    public int rp_id;
    public String clientIp;
    public int clientPort;

    public ClientToRpIdpCmd() {
        super();
    }

    public ClientToRpIdpCmd(int code,String clientId) {
        this.clientId = clientId;
        this.code = code;
    }
}
