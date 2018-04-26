package com.mst.karsac.Utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mst.karsac.Setting;

public class SharedPreferencesHandler {

    public static SharedPreferences getSharedPreferences(Context context){
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    public static void setStringPreferences(Context context, String key, String value){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(key, value);
        editor.commit();
    }

    public static void setTimestamp(Context context, String key, int value){
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt(key, value);
        editor.commit();
    }

    public static int getTimestamp(Context context, String key){
        return getSharedPreferences(context).getInt(key, 0);
    }

    public static String getStringPreferences(Context context, String key){
        return getSharedPreferences(context).getString(key, "");
    }

    public static String getSelectedMode(Context context, String key){
        return getSharedPreferences(context).getString(key, Setting.PUSH);
    }
}
