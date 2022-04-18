package com.nadoyagsa.pillaroid;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPrefManager {
    private static SharedPrefManager instance;
    private static SharedPreferences prefs;
    private static SharedPreferences.Editor prefsEditor;
    private final String PREFERENCES_NAME = "sharedPref";

    public SharedPrefManager(Context context) {
        prefs = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        prefsEditor = prefs.edit();
    }

    public static synchronized void init(Context context) {
        if (instance == null)
            instance = new SharedPrefManager(context);
    }

    public static String read(String key, String defValue) {
        return prefs.getString(key, defValue);
    }

    public static void write(String key, String value) {
        prefsEditor.putString(key, value);
        prefsEditor.commit();
    }

    public static Integer read(String key, int defValue) {
        return prefs.getInt(key, defValue);
    }

    public static void write(String key, Integer value) {
        prefsEditor.putInt(key, value);
        prefsEditor.commit();
    }

    public static Float read(String key, float defValue) {
        return prefs.getFloat(key, defValue);
    }

    public static void write(String key, Float value) {
        prefsEditor.putFloat(key, value);
        prefsEditor.commit();
    }

    public static boolean read(String key, boolean defValue) {
        return prefs.getBoolean(key, defValue);
    }

    public static void write(String key, boolean value) {
        prefsEditor.putBoolean(key, value);
        prefsEditor.commit();
    }

    public static void remove(String key) {
        prefsEditor.remove(key);
        prefsEditor.commit();
    }

    public static void destroyPref() {
        prefsEditor.clear().commit();
    }
}
