package com.example.jijipos;

import android.content.Context;
import java.io.File;

public class StorageManager {

    // Returns the size of the SQLite database file in Megabytes (MB)
    public static double getDatabaseSizeInMB(Context context) {
        try {
            File dbFile = context.getDatabasePath("jiji_pos_database");
            if (dbFile.exists()) {
                double bytes = dbFile.length();
                return bytes / (1024.0 * 1024.0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0.0;
    }

    // Safety guard threshold check
    public static boolean isStorageLimitExceeded(Context context, double limitInMB) {
        return getDatabaseSizeInMB(context) >= limitInMB;
    }
}
