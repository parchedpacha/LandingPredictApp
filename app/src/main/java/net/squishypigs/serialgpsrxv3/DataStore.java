package net.squishypigs.serialgpsrxv3;

import android.content.Context;
import android.content.SharedPreferences;

public class DataStore {
    private static final String PREFS_NAME = "MyPrefs";
    private final SharedPreferences sharedPreferences;
    private final SharedPreferences.Editor editor;

    public DataStore(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    public void saveString(String key, String value) {
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
}