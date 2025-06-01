package net.squishypigs.serialgpsrxv3;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.IOException;
public class storeCSV {

    public static boolean exportToCSV(Context context, String csvData) {
        OutputStream outputStream = null;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault());
        String timestamp = sdf.format(new Date());
        String fileName = "LandingPredict Data recorded " + "_" + timestamp + ".csv";



        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // ✅ Android 10+ (API 29+): Use MediaStore
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                Uri uri = context.getContentResolver().insert(
                        MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);

                if (uri == null) return false;

                outputStream = context.getContentResolver().openOutputStream(uri);
            } else {
                // ✅ Android 9 and below (API < 29): Write directly
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) downloadsDir.mkdirs();

                File file = new File(downloadsDir, fileName);
                outputStream = new FileOutputStream(file);
            }

            // Write data
            outputStream.write(csvData.getBytes());
            outputStream.flush();
            outputStream.close();
            return true;

        } catch (IOException e) {
            e.printStackTrace();
            try {
                if (outputStream != null) outputStream.close();
            } catch (IOException ignored) {}
            return false;
        }
    }
}