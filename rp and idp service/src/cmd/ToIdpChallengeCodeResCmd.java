package cmd;

import java.util.List;

/**
 * user -> idp service
 * to response ToClientChallengeCodeCmd from idp service
 */
public class ToIdpChallengeCodeResCmd extends CMD {

    public String clientId;
    public int rpId;
    public List<String> challengeCodes;
    public List<float[]> voicePatternCodes;

    public ToIdpChallengeCodeResCmd() {
        super();
    }

    public ToIdpChallengeCodeResCmd(List<String> challengeCodes, List<float[]> voicePatternCodes) {
        this.challengeCodes = challengeCodes;
        this.voicePatternCodes = voicePatternCodes;
        this.code = CMD.CODE_VOICE_DATA;
    }
}
