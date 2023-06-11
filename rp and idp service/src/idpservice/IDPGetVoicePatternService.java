package idpservice;

import authority.AuthCmd;
import cmd.CMD;
import cmd.ClientToRpIdpCmd;
import cmd.ToIdpChallengeCodeResCmd;
import cmd.IdpToRpResultCmd;
import com.alibaba.fastjson.JSON;

import idpservice.audio.DynamicTimeWrapping2D;
import idpservice.audio.pretreatment.GetFeatureCoffients;
import idpservice.db.DBManager;
import util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

class IDPGetVoicePatternService extends Thread {
    private static final String TAG = IDPGetVoicePatternService.class.getSimpleName();

    private BufferedReader mClientReader;
    private BufferedWriter mRpWriter;
    private ClientToRpIdpCmd cmd;
    private List<String> challenageCodes;
    private ToIdpChallengeCodeResCmd cc;

    public IDPGetVoicePatternService(BufferedReader mClientReader, BufferedWriter mRpWriter, ClientToRpIdpCmd cmd, List<String> challenageCodes) {
        this.mClientReader = mClientReader;
        this.mRpWriter = mRpWriter;
        this.cmd = cmd;
        this.challenageCodes = challenageCodes;
    }

    @Override
    public void run() {
    	String uaID = IDPService.genUaId(cmd.clientId, cmd.rp_id);
        String line;
        try {
            cc= new ToIdpChallengeCodeResCmd();
            cc.challengeCodes = new ArrayList<>();
            cc.voicePatternCodes = new ArrayList<>();
            while ((line = mClientReader.readLine()) != null) {
//                ToIdpChallengeCodeResCmd c = JSON.parseObject(line, ToIdpChallengeCodeResCmd.class);
                //签名接收
                ToIdpChallengeCodeResCmd c =AuthCmd.getReadCmd(line, ToIdpChallengeCodeResCmd.class);

                if (c == null) continue;
                cc.challengeCodes.addAll(c.challengeCodes);
                cc.voicePatternCodes.addAll(c.voicePatternCodes);
                Log.v(TAG, "=====add=====");
            }

            IdpToRpResultCmd toRpResultCmd = null;
            Log.v(TAG, "get data from client :" + cc.code);
            if (!isCodeEqual(cc.challengeCodes)) {
                Log.v(TAG, "ToIdpChallengeCodeResCmd error: ");
                toRpResultCmd = new IdpToRpResultCmd(cmd.code, false, uaID);
            } else {
                switch (cmd.code) {
                    case CMD.CODE_REG:
                    case CMD.CODE_RE_REG:
                        //通知rp
                        if (!DBManager.saveUserVoice(uaID, cc)) {
                            toRpResultCmd = new IdpToRpResultCmd(cmd.code, false, "");
                        } else {
                            toRpResultCmd = new IdpToRpResultCmd(cmd.code, true, uaID);
                        }
                        break;
                    case CMD.CODE_AUTH:
                    	if (Compare(cc)) {
                   		   toRpResultCmd = new IdpToRpResultCmd(cmd.code, true, uaID);        
						}else {
						   toRpResultCmd = new IdpToRpResultCmd(cmd.code, false, "");
						}

                        break;
                }
            }

            //通知rp注册结果

//            mRpWriter.write(JSON.toJSONString(toRpResultCmd) + '\n');
            //签名发送
            String sendStr = AuthCmd.getWriteCmd(JSON.toJSONString(toRpResultCmd));
            mRpWriter.write(sendStr + '\n');
            mRpWriter.flush();

        } catch (IOException e) {
            e.printStackTrace();
        }   
    }


    private boolean isCodeEqual(List<String> challenageCodes) {
        if (challenageCodes != null && challenageCodes.size() == this.challenageCodes.size()) {
            for (int i = 0; i < challenageCodes.size(); i++) {
                if (!challenageCodes.get(i).equals(this.challenageCodes.get(i))) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    private boolean Compare(ToIdpChallengeCodeResCmd responseCmd){
    	 double distance = 0; 
    	 double template[][] = null;
    	 double alldistance = 0;
    	 for (int i = 0; i < responseCmd.challengeCodes.size(); i++) {
    		 GetFeatureCoffients featureCoffients = new GetFeatureCoffients(responseCmd.voicePatternCodes.get(i));
    		 double voicefeaturevector[][] = featureCoffients.getvoiceFeatureVector();
			try {
				String uaIDstored = IDPService.genUaId(cmd.clientId, cmd.rp_id);
				template = DBManager.getUserVoiceData(uaIDstored,responseCmd.challengeCodes.get(i));
				System.out.println(responseCmd.challengeCodes.get(i));
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
    		 DynamicTimeWrapping2D dtw = new DynamicTimeWrapping2D(voicefeaturevector,template);
    		 distance = dtw.calDistance();
    		 if(distance>100){
    			 distance = 10;
    		 }
    	     System.out.println("distance= "+distance);
    	     alldistance += distance;
    	 }
    	System.out.println("alldistance="+alldistance);
    	if (alldistance<350) {
			return true;
		}
    	else {
			return false;
	   }
     }
  }

