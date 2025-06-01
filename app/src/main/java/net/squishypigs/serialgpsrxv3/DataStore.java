package net.squishypigs.serialgpsrxv3;
import java.util.*;
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

    public void saveArrayList (String key, ArrayList<String[]> list){
        String value = collapseListToString( list);

        editor.putString(key, value);
        editor.apply();
    }
    private String collapseListToString(ArrayList<String[]> list) {
        StringBuilder result = new StringBuilder();

        for (String[] row : list) {
            for (int i = 0; i < row.length; i++) {
                result.append(row[i]);
                if (i < row.length - 1) {
                    result.append(",");
                }
            }
            result.append("\n");
        }

        return result.toString().trim(); // trim() removes the trailing newline
    }


    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }
}