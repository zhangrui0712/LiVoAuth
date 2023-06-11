package com.github.niostar.audiopcmrecord;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechSynthesizer;
import com.iflytek.cloud.SynthesizerListener;
import com.iflytek.cloud.util.ResourceUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

import com.github.niostar.audiopcmrecord.audio.PcmHandler;
import com.github.niostar.audiopcmrecord.cmd.CMD;
import com.github.niostar.audiopcmrecord.cmd.ClientToRpIdpCmd;
import com.github.niostar.audiopcmrecord.cmd.ResultCmd;
import com.github.niostar.audiopcmrecord.cmd.RpToClientResultCmd;
import com.github.niostar.audiopcmrecord.cmd.ToClientChallengeCodeCmd;
import com.github.niostar.audiopcmrecord.cmd.ToIdpChallengeCodeResCmd;
import com.github.niostar.audiopcmrecord.utils.DeviceUtil;

public class AuthActivity extends Activity implements View.OnClickListener {

    private static final String TAG = AuthActivity.class.getSimpleName();
    private SpeechSynthesizer mTts;
    private int authPosition = -1;
    private ViewGroup codeContainer1,codeContainer2,codeContainer3,codeContainer4;
    private AudioRecord audioRecord;
    // 设置音频采样率，44100标准 22050，16000，11025
    private static int sampleRateInHz = 44100;
    // CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
    private static final int channelConfig = AudioFormat.CHANNEL_IN_STEREO;
    private static final int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSizeInBytes = 0;
    private boolean isRecord = false;// 设置正在录制的状态
    private List<float[]> userVoicePatternCodes;
    private List<String> authCodes = new ArrayList<>();
    private ServiceConnection serviceConnection = new MyServiceConnection();
    private Messenger mServerMessenger;
    private Handler mClientHandler = new MyClientHandler();
    private Messenger mClientMessenger = new Messenger(mClientHandler);
    private int code;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        int mode = getIntent().getIntExtra("MODE", -1);
        if (mode == Constants.MODE_AUTHENTICATE) {
            code = CMD.CODE_AUTH;
            setTitle("认证");
        } else if (mode == Constants.MODE_REGISTER) {
            code = CMD.CODE_REG;
            setTitle("注册");
        } else if (mode == Constants.MODE_RE_REGISTER) {
            code = CMD.CODE_RE_REG;
            setTitle("重新注册");
        } else {
            code = CMD.CODE_DELETE_REG;
            findViewById(R.id.delete_view).setVisibility(View.VISIBLE);
            findViewById(R.id.main_view).setVisibility(View.GONE);
            setTitle("删除注册");
        }

        userVoicePatternCodes = new ArrayList<>();
        codeContainer1 = (ViewGroup) findViewById(R.id.code_container_1);
        codeContainer2 = (ViewGroup) findViewById(R.id.code_container_2);
        codeContainer3 = (ViewGroup) findViewById(R.id.code_container_3);
        codeContainer4 = (ViewGroup) findViewById(R.id.code_container_4);
        findViewById(R.id.upload_view).setVisibility(View.GONE);
        findViewById(R.id.upload_view).setOnClickListener(this);

        Intent intent = new Intent(AuthActivity.this, ConnectService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        mTts = SpeechSynthesizer.createSynthesizer(this, new InitListener() {
            @Override
            public void onInit(int code) {
                Log.d(TAG, "InitListener init() code = " + code);
                if (code != ErrorCode.SUCCESS) {
                    showTip("初始化失败,错误码：" + code);
                } else {
                    initParams();
                }
            }
        });
    }

    private void sendMsgToService(CMD cmd) {
        if (!mBound) return;
        Message m = new Message();
        m.what = cmd.code;
        m.obj = cmd;
        m.replyTo = mClientMessenger;

        try {
            mServerMessenger.send(m);
        } catch (RemoteException e) {
            e.printStackTrace();
            showTip("error send msg to service");
        }
    }

    private void startRecord() {
        if (audioRecord == null) {
            bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleRateInHz, channelConfig, audioFormat, bufferSizeInBytes);
        }
        audioRecord.startRecording();
        isRecord = true;
        new GetRecordTask().execute();

        mClientHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopRecord();
            }
        }, 2400);
    }

    private void stopRecord() {
        if (audioRecord != null) {
            System.out.println("stopRecord");
            isRecord = false;
            if (audioRecord.getState() == AudioRecord.STATE_INITIALIZED) {
                audioRecord.stop();
                audioRecord.release();
            }
            audioRecord = null;
        }
    }

    private void processAuth() {
        if (authPosition == -1) {
            startSpeak("请跟着我读出以下字符");
        } else if (authPosition < authCodes.size()) {
            startSpeak(authCodes.get(authPosition));
        } else {//完毕
            //录音完毕
            Log.d(TAG, "record success size is :" + userVoicePatternCodes.size());
            System.out.println(userVoicePatternCodes);
            findViewById(R.id.upload_view).setVisibility(View.VISIBLE);
        }

        setAuthCodeView();
    }

    private void initAuthCodeView() {
        codeContainer1.removeAllViews();
        codeContainer2.removeAllViews();
        codeContainer3.removeAllViews();
        codeContainer4.removeAllViews();
        for(int i =0; i<authCodes.size();i++){
            String code = authCodes.get(i);
            TextView tv = new TextView(this);
            tv.setText(code);
            tv.setPadding(20, 20, 20, 20);
            tv.setTextColor(0xff04c0cf);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
            if(i<9){
                codeContainer1.addView(tv);
            }else if(i<18){
                codeContainer2.addView(tv);
            }else if(i<27){
                codeContainer3.addView(tv);
            } else{
                codeContainer4.addView(tv);
            }
        }

        findViewById(R.id.loading_view).setVisibility(View.GONE);
        codeContainer1.setVisibility(View.VISIBLE);
        codeContainer2.setVisibility(View.VISIBLE);
        codeContainer3.setVisibility(View.VISIBLE);
        codeContainer4.setVisibility(View.VISIBLE);
        processAuth();
    }

    private void setAuthCodeView() {
        if (authPosition >= 0 && authPosition < authCodes.size()) {
            TextView tv = null;
            if(authPosition<9){
                tv = (TextView) codeContainer1.getChildAt(authPosition);
            }else if(authPosition<18){
                tv = (TextView) codeContainer2.getChildAt(authPosition-9);
            }else if(authPosition<27){
                tv = (TextView) codeContainer2.getChildAt(authPosition-18);
            } else{
                tv = (TextView) codeContainer3.getChildAt(authPosition-27);
            }
            tv.setTextColor(0xfffc5b23);
            tv.setScaleX(1.3f);
            tv.setScaleY(1.3f);
        }

        if (authPosition > 0) {
            TextView tvPre =null;
            if(authPosition<=9){
                tvPre = (TextView) codeContainer1.getChildAt(authPosition - 1);
            }else if(authPosition<=18){
                tvPre = (TextView) codeContainer2.getChildAt(authPosition - 10);
            }else if(authPosition<=27){
                tvPre = (TextView) codeContainer3.getChildAt(authPosition - 19);
            } else{
                tvPre = (TextView) codeContainer4.getChildAt(authPosition - 28);
            }

            if(tvPre!=null){
                tvPre.setTextColor(0xff04c0cf);
                tvPre.setScaleX(1.0f);
                tvPre.setScaleY(1.0f);
            }

        }
    }

    private void startSpeak(String text) {
        Log.d(TAG, "start speak " + text);
        int code = mTts.startSpeaking(text, new SynthesizerListener() {
            @Override
            public void onSpeakBegin() {
                if (authPosition >= 0 && authPosition < authCodes.size()) {
                    showTip("请读" + authCodes.get(authPosition));
                }
            }

            @Override
            public void onBufferProgress(int i, int i1, int i2, String s) {

            }

            @Override
            public void onSpeakPaused() {

            }

            @Override
            public void onSpeakResumed() {

            }

            @Override
            public void onSpeakProgress(int i, int i1, int i2) {

            }

            @Override
            public void onCompleted(SpeechError speechError) {
                if (speechError == null) {
                    authPosition++;

                    if(mBound)
                    mClientHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            processAuth();
                        }
                    }, 2000);

                } else {
                    showTip(speechError.getPlainDescription(true));
                }

                startRecord();
            }

            @Override
            public void onEvent(int i, int i1, int i2, Bundle bundle) {

            }
        });


        if (code != ErrorCode.SUCCESS) {
            showTip("语音合成失败,错误码: " + code);
        }
    }

    private void initParams() {
        // 清空参数
        mTts.setParameter(SpeechConstant.PARAMS, null);
        mTts.setParameter(SpeechConstant.ENGINE_TYPE, SpeechConstant.TYPE_LOCAL);
        mTts.setParameter(ResourceUtil.TTS_RES_PATH, getResourcePath());
        mTts.setParameter(SpeechConstant.VOICE_NAME, "xiaofeng");//xiaoyan
        mTts.setParameter(SpeechConstant.VOLUME, "50");
        //设置播放器音频流类型
        mTts.setParameter(SpeechConstant.STREAM_TYPE, "3");
    }

    //获取发音人资源路径
    private String getResourcePath() {
        //合成通用资源
        return ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/common.jet") +
                ";" +//发音人资源
                ResourceUtil.generateResourcePath(this, ResourceUtil.RESOURCE_TYPE.assets, "tts/xiaofeng.jet");
    }


    //录音task
    private class GetRecordTask extends AsyncTask<Void, Void, float[]> {

        byte[] pcmData = new byte[bufferSizeInBytes * 120];

        @Override
        protected float[] doInBackground(Void... params) {
            byte[] readBuffer = new byte[bufferSizeInBytes];

            int len, position = 0;
            while (isRecord && position < pcmData.length && audioRecord != null) {
                if ((len = audioRecord.read(readBuffer, 0, readBuffer.length)) > 0) {
                    System.arraycopy(readBuffer, 0, pcmData, position, len);
                    position += len;
                }
            }

            if (isRecord) {
                stopRecord();
            }

            System.out.println("bufferSizeInBytes:" + bufferSizeInBytes);
            System.out.println("position:" + position);

            //byte->float
            float[] pcmDataF = new float[position / 2];
            for (int i = 0; i < position / 2; i++) {
                int LSB = pcmData[2 * i];
                int MSB = pcmData[2 * i + 1];
                pcmDataF[i] = MSB << 8 | (255 & LSB);
            }

            PcmHandler.normalizePCM(pcmDataF);
            return PcmHandler.handleEndPoint(pcmDataF, sampleRateInHz);
        }

        @Override
        protected void onPostExecute(float[] pcmF) {
            userVoicePatternCodes.add(pcmF);
        }
    }


    //service 发送消息在这儿接收
    private class MyClientHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Log.d(TAG, "reveive msg from service：" + msg.what);
            switch (msg.what) {
                //获得 CHALLENGE_CODE
                case CMD.CODE_CHALLENGE:
                    authCodes.clear();
                    userVoicePatternCodes.clear();
                    ToClientChallengeCodeCmd c = (ToClientChallengeCodeCmd) msg.obj;
                    Collections.addAll(authCodes, c.challengeCode);
                    initAuthCodeView();
                    break;
                case CMD.CODE_REG:
                case CMD.CODE_AUTH:
                case CMD.CODE_RE_REG:
                case CMD.CODE_DELETE_REG:
                case CMD.CODE_NONE:
                    RpToClientResultCmd cc = (RpToClientResultCmd) msg.obj;
                    if (cc.success) {
                        showTip("操作成功");
                        finish();
                    } else {
                        showTip("操作失败");
                    }

                    if (msg.what == CMD.CODE_DELETE_REG) {
                        findViewById(R.id.delete_progress).setVisibility(View.GONE);
                        ((TextView) findViewById(R.id.delete_text)).setText(cc.success ? "删除注册成功" : "删除注册失败");
                    }
                    break;
                case CMD.CODE_RESULT:
                    ResultCmd resultCmd = (ResultCmd) msg.obj;
                    showTip(resultCmd.message);
                    break;
                default:
            }
        }
    }

    //service is bind
    private boolean mBound;

    private class MyServiceConnection implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            Log.d(TAG, "in MyServiceConnection onServiceConnected");
            mServerMessenger = new Messenger(binder);
            mBound = true;
            //发送命令到rp
            ClientToRpIdpCmd cmd = new ClientToRpIdpCmd(code, DeviceUtil.getLocalMacAddress(AuthActivity.this));
            cmd.clientPort = Constants.AUTH_SERVICE_PORT;
            cmd.clientIp = DeviceUtil.getLocalIpAddress(AuthActivity.this);
            sendMsgToService(cmd);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "in MyServiceConnection onServiceDisconnected");
            mBound = false;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.upload_view:
                ToIdpChallengeCodeResCmd c = new ToIdpChallengeCodeResCmd(authCodes, userVoicePatternCodes);
                c.clientId = DeviceUtil.getLocalMacAddress(this);
                sendMsgToService(c);
                showTip("上传中...");
                break;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        stopRecord();
        mTts.stopSpeaking();
        mTts.destroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
    }


    private static Toast mToast;

    private void showTip(final String str) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mToast == null) {
                    mToast = Toast.makeText(AuthActivity.this, str, Toast.LENGTH_SHORT);
                } else {
                    mToast.setText(str);
                }
                mToast.show();
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();

        Log.e(TAG, "onDestroy");
        stopRecord();
        mTts.stopSpeaking();
        mTts.destroy();
        if (mBound) {
            unbindService(serviceConnection);
            mBound = false;
        }
    }
}
