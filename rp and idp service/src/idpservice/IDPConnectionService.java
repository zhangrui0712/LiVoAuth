package idpservice;

import authority.AuthCmd;
import cmd.CMD;
import cmd.IdpToRpResultCmd;
import cmd.ToClientChallengeCodeCmd;
import cmd.ClientToRpIdpCmd;
import com.alibaba.fastjson.JSON;
import idpservice.db.DBManager;
import util.Log;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * rpc service
 * send the client cmd to idp service
 * build the connection between client and idp service
 * client->rp(rpc service)->idp
 * idp->client
 */
public class IDPConnectionService extends Thread {
    private static final String TAG = IDPConnectionService.class.getSimpleName();

    private Socket mRpSocket;

    public IDPConnectionService(Socket mRpSocket) {
        this.mRpSocket = mRpSocket;
    }

    @Override
    public void run() {
        BufferedReader mRpReader;
        BufferedWriter mRpWriter;

        try {
            mRpReader = new BufferedReader(new InputStreamReader(mRpSocket.getInputStream()));
            mRpWriter = new BufferedWriter(new OutputStreamWriter(mRpSocket.getOutputStream()));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        while (true) {
            try {
                String rpLine = mRpReader.readLine();
                if (rpLine != null && !rpLine.isEmpty()) {
                    Log.v(TAG, "get command from rp:" + rpLine);
                    consumeRpCmd(rpLine, mRpWriter);
                }
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }

    }


    private void consumeRpCmd(String rpCmdStr, BufferedWriter mRpWriter) throws IOException {
//        ClientToRpIdpCmd rpCommand = JSON.parseObject(rpCmdStr, ClientToRpIdpCmd.class);

        //签名接收
        ClientToRpIdpCmd rpCommand =AuthCmd.getReadCmd(rpCmdStr,ClientToRpIdpCmd.class);
        if (rpCommand != null) {
            String uaid = IDPService.genUaId(rpCommand.clientId, rpCommand.rp_id);
            switch (rpCommand.code) {
                case CMD.CODE_REG:
                case CMD.CODE_AUTH:
                    handleAuthReg(rpCommand, mRpWriter);
                    break;
                case CMD.CODE_RE_REG:
                    DBManager.deleteUserVoice(uaid);
                    handleAuthReg(rpCommand, mRpWriter);
                    break;
                case CMD.CODE_DELETE_REG:
                    IdpToRpResultCmd regResultCmd = null;

                    if (DBManager.deleteUserVoice(uaid)) {
                        regResultCmd = new IdpToRpResultCmd(CMD.CODE_DELETE_REG, true, uaid);
                    } else {
                        regResultCmd = new IdpToRpResultCmd(CMD.CODE_DELETE_REG, false, uaid);
                    }


//                    mRpWriter.write(JSON.toJSONString(regResultCmd) + "\n");
                    //签名发送
                    String sendStr = AuthCmd.getWriteCmd(JSON.toJSONString(regResultCmd));
                    mRpWriter.write(sendStr + "\n");

                    mRpWriter.flush();
                    Log.v(TAG, "send delete res to rp " + sendStr);
                    break;
            }
        }
    }

    private void handleAuthReg(ClientToRpIdpCmd cmd, BufferedWriter mRpWriter) {
        BufferedReader mClientReader;
        BufferedWriter mClientWrite;
        try {



            Socket mClientSocket = new Socket(cmd.clientIp, cmd.clientPort);
            mClientReader = new BufferedReader(new InputStreamReader(mClientSocket.getInputStream()));
            mClientWrite = new BufferedWriter(new OutputStreamWriter(mClientSocket.getOutputStream()));
            Log.v(TAG, "connected to client " + cmd.clientIp + "@" + cmd.clientPort);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, "error connect to the client");
            return;
        }

        String[] challengeCode = null;
        if (cmd.code == CMD.CODE_REG || cmd.code == CMD.CODE_RE_REG) {
            challengeCode = ChallengeCode.generateRegChallenageCode();
        } else if (cmd.code == CMD.CODE_AUTH) {
            challengeCode = ChallengeCode.generateRandomChallenageCode(8);
        }

        if (challengeCode == null) return;
        ToClientChallengeCodeCmd codeCmd = new ToClientChallengeCodeCmd(challengeCode);

        try {

//            mClientWrite.write(JSON.toJSONString(codeCmd) + "\n");
            //签名发送
            String sendStr =AuthCmd.getWriteCmd(JSON.toJSONString(codeCmd));
            mClientWrite.write(sendStr + "\n");
            mClientWrite.flush();
            List<String> codes = new ArrayList<>();
            for (String c : challengeCode) {
                codes.add(c);
            }
            new IDPGetVoicePatternService(mClientReader, mRpWriter, cmd, codes).start();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
}
