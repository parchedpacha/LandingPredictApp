package net.squishypigs.serialgpsrxv3;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class storeCSV {

    public static void exportToCSV(Context context, String csvData) {
        File file = new File(context.getExternalFilesDir(null), "export.csv");

        try {
            FileWriter writer = new FileWriter(file);
            writer.write(csvData);
            writer.flush();
            writer.close();

            // Share the file
            Uri fileUri = FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider",
                    file);

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/csv");
            intent.putExtra(Intent.EXTRA_STREAM, fileUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            context.startActivity(Intent.createChooser(intent, "Share CSV file"));

        } catch (IOException e) {
            e.printStackTrace();
            // Optionally handle with a Toast or Log
        }
    }
}
