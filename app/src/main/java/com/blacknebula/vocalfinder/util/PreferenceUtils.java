package com.blacknebula.vocalfinder.util;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.blacknebula.vocalfinder.VocalFinderApplication;

/**
 * @author hazem
 */

public class PreferenceUtils {

    private static SharedPreferences sharedPreferences;

    public static SharedPreferences getPreferences() {
        if (sharedPreferences == null) {
            sharedPreferences = PreferenceManager.getDefaultSharedPreferences(VocalFinderApplication.getAppContext());
        }
        return sharedPreferences;
    }

    public static String asString(String key, String defaultValue) {
        return getPreferences().getString(key, defaultValue);
    }

    public static Boolean asBoolean(String key, Boolean defaultValue) {
        return getPreferences().getBoolean(key, defaultValue);
    }

    public static int asInt(String key, int defaultValue) {
        return Integer.parseInt(getPreferences().getString(key, "" + defaultValue));
    }
}
