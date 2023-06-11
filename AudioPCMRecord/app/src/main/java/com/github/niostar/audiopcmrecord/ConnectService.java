package com.github.niostar.audiopcmrecord;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import com.alibaba.fastjson.JSON;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;

import com.github.niostar.audiopcmrecord.authority.AuthCmd;
import com.github.niostar.audiopcmrecord.cmd.CMD;
import com.github.niostar.audiopcmrecord.cmd.ResultCmd;
import com.github.niostar.audiopcmrecord.cmd.RpToClientResultCmd;
import com.github.niostar.audiopcmrecord.cmd.ToClientChallengeCodeCmd;
import com.github.niostar.audiopcmrecord.cmd.ToIdpChallengeCodeResCmd;
import org.json.JSONException;

//bind -> create -> bind
//bind again null
//ubind ->unbind -> destroy
public class ConnectService extends Service {

    private static final String TAG = "ConnectService";
    private Socket rpConnectSocket, idpConnetSocket;
    private BufferedReader rpReader, idpReader;
    private BufferedWriter rpWriter, idpWriter;
    private boolean isConnectToRp, isConnectToIdp;
    private Messenger mClientMessenger;
    private SendCmdToServerThread sender;
    private boolean running;
    private int hostPort = Constants.AUTH_SERVICE_PORT;
    private ServerSocket mServerSocket;

    @Override
    public void onCreate() {
        running = true;
        sender = new SendCmdToServerThread();
        super.onCreate();
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "in onBind");
        Messenger mServerMessenger = new Messenger(new ServerHandler());
        //返回service handler 对象给客户端

        sender.start();

        new ReadIDP().start();
        new ReadRp().start();
        return mServerMessenger.getBinder();
    }

    private boolean connectToRpService() {
        try {
            rpConnectSocket = new Socket(Constants.RP_IP, Constants.RP_PORT);
            rpReader = new BufferedReader(new InputStreamReader(rpConnectSocket.getInputStream()));
            rpWriter = new BufferedWriter(new OutputStreamWriter(rpConnectSocket.getOutputStream()));
            Log.d(TAG, "success connect to ro service at " + Constants.RP_IP + "@" + Constants.RP_PORT);
            isConnectToRp = true;
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        isConnectToRp = false;
        return false;
    }

    private boolean sendToService(boolean isRpService, CMD cmd) {
        if (isRpService) {
            String sendCmd = JSON.toJSONString(cmd);
            if (!isConnectToRp || rpConnectSocket == null) {
                boolean b = connectToRpService();
                if (!b) {
                    notifyClient(new ResultCmd(false, "error connect to rp service at " + Constants.RP_IP + "@" + Constants.RP_PORT));
                    isConnectToRp = false;
                    return false;
                }
            }

            Log.d(TAG, "send command to rp:" + sendCmd);

            try {
                //签名数据发送
                rpWriter.write(AuthCmd.getWriteCmd(sendCmd)+"\n");
//                rpWriter.write(sendCmd + "\n");
                rpWriter.flush();
                return true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            if (idpWriter == null) return false;
            ToIdpChallengeCodeResCmd codeResCmd = (ToIdpChallengeCodeResCmd) cmd;
            int count = codeResCmd.challengeCodes.size();

            Log.d(TAG, "size :" + count + "|" + codeResCmd.voicePatternCodes.size());
            int times = count / 5;
            if (count % 5 != 0) {
                times++;
            }

            for (int i = 1; i <= times; i++) {
                ToIdpChallengeCodeResCmd c1 = new ToIdpChallengeCodeResCmd();
                int size = i < times ? 5 : count - (i - 1) * 5;
                c1.challengeCodes = codeResCmd.challengeCodes.subList((i - 1) * 5, (i - 1) * 5 + size);
                c1.voicePatternCodes = codeResCmd.voicePatternCodes.subList((i - 1) * 5, (i - 1) * 5 + size);
                String sendCmd = JSON.toJSONString(c1);
                try {


                    //签名数据发送
                    idpWriter.write((AuthCmd.getWriteCmd(sendCmd)+"\n"));

//                    idpWriter.write(sendCmd + "\n");
                    idpWriter.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.d(TAG, "send command to idp" + c1.code + "|" + c1.challengeCodes.get(0) + "-" + c1.challengeCodes.get(size - 1));
            }

            try {
                idpConnetSocket.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return false;
    }

    private class ServerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "receive msg from client");
            switch (msg.what) {
                case CMD.CODE_REG:
                case CMD.CODE_AUTH:
                case CMD.CODE_RE_REG:
                case CMD.CODE_DELETE_REG:
                    //发送到rp
                    mClientMessenger = msg.replyTo;
                    sender.toRpCmd = (CMD) msg.obj;
                    break;
                case CMD.CODE_VOICE_DATA:
                    Log.d(TAG, "获得声音数据 发送到idp");
                    sender.toIdpCmd = (CMD) msg.obj;
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private class SendCmdToServerThread extends Thread {
        CMD toRpCmd, toIdpCmd;
        @Override
        public void run() {
            Log.d(TAG, "sender thread start");
            while (running) {
                if (toRpCmd != null) {
                    if (!sendToService(true, toRpCmd)) {
                        notifyClient(new ResultCmd(false, "发送命令到RP失败"));
                    }
                    toRpCmd = null;
                }

                if (toIdpCmd != null) {
                    if (!sendToService(false, toIdpCmd)) {
                        notifyClient(new ResultCmd(false, "发送命令到IDP失败"));
                    }
                    toIdpCmd = null;
                }
            }
        }
    }

    private class ReadIDP extends Thread {
        @Override
        public void run() {
            try {
                mServerSocket = new ServerSocket(hostPort);
                Log.v(TAG, "auth service start waiting for idp to connected");
                idpConnetSocket = mServerSocket.accept();
                idpReader = new BufferedReader(new InputStreamReader(idpConnetSocket.getInputStream()));
                idpWriter = new BufferedWriter(new OutputStreamWriter(idpConnetSocket.getOutputStream()));
                isConnectToIdp = true;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            while (running) {
                //获得idp 数据
                if (idpReader != null) {
                    try {
                        String readLine = idpReader.readLine();
                        if (readLine != null) {

//                            ToClientChallengeCodeCmd command = JSON.parseObject(readLine, ToClientChallengeCodeCmd.class);

                            //签名数据读取
                            ToClientChallengeCodeCmd   command = AuthCmd.getReadCmd(readLine, ToClientChallengeCodeCmd.class);

                            if (command == null) {
                                Log.e(TAG, "error parse cmd :" + readLine);
                                continue;
                            }
                            notifyClient(command);
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }

            try {
                mServerSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    private class ReadRp extends Thread {
        @Override
        public void run() {
            //获得rp数据
            while (running) {
                if (rpReader != null) {
                    try {
                        String readLine = rpReader.readLine();
                        if (readLine != null) {
//                            RpToClientResultCmd res = JSON.parseObject(readLine, RpToClientResultCmd.class);
                            //签名数据读取
                            RpToClientResultCmd res =AuthCmd.getReadCmd(readLine, RpToClientResultCmd.class);
                            if (res == null) {
                                Log.e(TAG, "error parse cmd :" + readLine);
                                continue;
                            }
                            notifyClient(res);
                        }
                    }catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void notifyClient(CMD cmd) {
        if (mClientMessenger == null) return;
        // service发送消息给client
        Message toClientMsg = new Message();
        toClientMsg.what = cmd.code;
        toClientMsg.obj = cmd;

        try {
            mClientMessenger.send(toClientMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.d(TAG, "in onUnbind");
        if (mServerSocket != null) {
            try {
                mServerSocket.close();
                mServerSocket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "in onDestroy");
        running = false;
        super.onDestroy();
    }
}
