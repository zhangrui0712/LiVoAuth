package rpservice;

import authority.AuthCmd;
import cmd.CMD;
import cmd.RpToClientResultCmd;
import cmd.IdpToRpResultCmd;
import com.alibaba.fastjson.JSON;
import util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

class RPGetIDPResultService extends Thread {
    private static final String TAG = RPGetIDPResultService.class.getSimpleName();

    private BufferedReader mIDPServiceReader;
    private BufferedWriter mClientWriter;

    public RPGetIDPResultService(BufferedReader mIDPServiceReader, BufferedWriter mClientWriter) {
        this.mIDPServiceReader = mIDPServiceReader;
        this.mClientWriter = mClientWriter;
    }

    @Override
    public void run() {
        String userIdCommand;
        try {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            userIdCommand = mIDPServiceReader.readLine();
            if (userIdCommand != null && !userIdCommand.isEmpty()) {
//                IdpToRpResultCmd resultCmd = JSON.parseObject(userIdCommand, IdpToRpResultCmd.class);

                //签名接收
                IdpToRpResultCmd resultCmd = AuthCmd.getReadCmd(userIdCommand,IdpToRpResultCmd.class);
                Log.v(TAG, "rev cmd from idp: " + userIdCommand);
                RpToClientResultCmd toClientCmd;
                if (resultCmd == null || !resultCmd.success) {
                    toClientCmd = new RpToClientResultCmd(resultCmd != null ? resultCmd.code : CMD.CODE_NONE,
                            false, "操作失败");
                } else {
                    toClientCmd = new RpToClientResultCmd(resultCmd.code, true, "操作成功");
                }


//                mClientWriter.write(JSON.toJSONString(toClientCmd) + '\n');
                //签名发送
                String sendStr =AuthCmd.getWriteCmd(JSON.toJSONString(toClientCmd));

                mClientWriter.write(sendStr+ '\n');
                mClientWriter.flush();

//                Log.v(TAG, "send cmd to client: " + JSON.toJSONString(toClientCmd));
                Log.v(TAG, "send cmd to client: " + sendStr);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
