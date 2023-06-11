package com.github.niostar.audiopcmrecord.cmd;

public class ResultCmd extends CMD {

    public boolean state;
    public String message;

    public ResultCmd() {
        super();
    }

    public ResultCmd(boolean state, String message) {
        this.state = state;
        this.message = message;
        code = CODE_RESULT;
    }
}
