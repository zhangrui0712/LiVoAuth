package cmd;


/**
 * idp service -> user
 */
public class ToClientChallengeCodeCmd extends CMD {

    public String[] challengeCode;

    public ToClientChallengeCodeCmd() {
        super();
    }

    public ToClientChallengeCodeCmd(String[] challengeCode) {
        this.challengeCode = challengeCode;
        this.code = CMD.CODE_CHALLENGE;
    }
}
