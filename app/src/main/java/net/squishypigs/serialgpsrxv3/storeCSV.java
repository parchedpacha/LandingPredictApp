package net.squishypigs.serialgpsrxv3;
import java.util.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import android.content.Context;
public class storeCSV {
    List<String[]> data = new ArrayList<>();
    boolean error=false;
    Context mainActivityContext;

    public storeCSV(MainActivity mainActivity) {
        mainActivityContext = mainActivity;
    }
    // TODO use datastore to make persistent storage, just in case!
    public boolean public_export(ArrayList<String[]> data) {
        File file = new File(mainActivityContext.getExternalFilesDir(null), "export.csv");
        exportToCSV(file,data);
        return error;
    }
    public void exportToCSV(File file, List<String[]> data) {
        try {
            FileWriter writer = new FileWriter(file);
            for (String[] row : data) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < row.length; i++) {
                    // Escape commas and quotes
                    String cell = row[i].replace("\"", "\"\"");
                    if (cell.contains(",") || cell.contains("\"")) {
                        cell = "\"" + cell + "\"";
                    }
                    sb.append(cell);
                    if (i < row.length - 1) {
                        sb.append(",");
                    }
                }
                writer.append(sb.toString()).append("\n");
            }
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
