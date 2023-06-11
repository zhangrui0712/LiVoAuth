package com.github.niostar.audiopcmrecord.utils;

import android.content.Context;

import java.io.InputStream;

public class FileUtil {
    private static final String TAG = "FileUtil";

    public static String readAssetsFile(Context mContext, String file, String code) {
        int len = 0;
        byte[] buf = null;
        String result = "";
        try {
            InputStream in = mContext.getAssets().open(file);
            len = in.available();
            buf = new byte[len];
            in.read(buf, 0, len);

            result = new String(buf, code);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
}
