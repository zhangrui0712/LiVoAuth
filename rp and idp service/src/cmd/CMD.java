package cmd;

public class CMD {
    public int code = CODE_NONE;

    public static final int CODE_NONE = -1;
    public static final int CODE_REG = 100;
    public static final int CODE_RE_REG = 101;
    public static final int CODE_AUTH = 102;
    public static final int CODE_DELETE_REG = 103;

    public static final int CODE_CHALLENGE = 104;
    public static final int CODE_VOICE_DATA = 104;



    public CMD() {
        super();
    }

    @Override
    public String toString() {
        return "CMD [code=" + code + "]";
    }
}
