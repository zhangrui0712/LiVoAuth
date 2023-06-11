package com.github.niostar.audiopcmrecord.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

public class ResultParser {

    public static int parserCommand(String json) {
        StringBuilder ret = new StringBuilder();
        int id = -1;
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);
            JSONArray words = joResult.getJSONArray("ws");
            ret.append("【结果】");
            for (int i = 0; i < words.length(); i++) {
                JSONObject wsItem = words.getJSONObject(i);
                JSONArray items = wsItem.getJSONArray("cw");
                //本地多候选按照置信度高低排序，一般选取第一个结果即可
                JSONObject obj = items.getJSONObject(0);
                if (obj.getString("w").contains("nomatch")) {
                    ret.append("没有匹配结果.");
                    Log.e("result", ret.toString());
                }

                if (obj.has("id")) {
                    id = obj.getInt("id");
                }

                ret.append(obj.getString("w"));
                ret.append("【置信度】").append(joResult.getInt("sc"));
                ret.append("\n");
            }

        } catch (Exception e) {
            e.printStackTrace();
            ret.append("没有匹配结果.");
            id = -1;
        }

        Log.d("ResultParser", ret.toString());
        return id;
    }
}
