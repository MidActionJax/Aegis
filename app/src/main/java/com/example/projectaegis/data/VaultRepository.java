package com.example.projectaegis.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import com.example.projectaegis.security.VaultKeyProvider;

import net.zetetic.database.sqlcipher.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class VaultRepository {

    private static volatile VaultRepository instance;

    private final SQLiteDatabase database;

    private VaultRepository(Context context) {
        Context appContext = context.getApplicationContext();
        System.loadLibrary("sqlcipher");

        byte[] rawKey = VaultKeyProvider.getOrCreateRawKey(appContext);
        try {
            VaultDbHelper dbHelper = new VaultDbHelper(appContext, rawKey);
            this.database = dbHelper.getWritableDatabase();
        } finally {
            Arrays.fill(rawKey, (byte) 0);
        }
    }

    public static VaultRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (VaultRepository.class) {
                if (instance == null) {
                    instance = new VaultRepository(context);
                }
            }
        }
        return instance;
    }

    public long insert(String accountName, String url, String username, String password) {
        ContentValues values = VaultDbHelper.toValues(accountName, url, username, password, System.currentTimeMillis());
        return database.insert(VaultDbHelper.TABLE, null, values);
    }

    public void update(long id, String accountName, String url, String username, String password) {
        ContentValues values = VaultDbHelper.toValues(accountName, url, username, password, System.currentTimeMillis());
        database.update(VaultDbHelper.TABLE, values, VaultDbHelper.COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        database.delete(VaultDbHelper.TABLE, VaultDbHelper.COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Credential getById(long id) {
        try (Cursor cursor = database.query(VaultDbHelper.TABLE, null, VaultDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return VaultDbHelper.fromCursor(cursor);
            }
            return null;
        }
    }

    public List<Credential> getAll() {
        List<Credential> results = new ArrayList<>();
        try (Cursor cursor = database.query(VaultDbHelper.TABLE, null, null, null, null, null,
                VaultDbHelper.COL_ACCOUNT_NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                results.add(VaultDbHelper.fromCursor(cursor));
            }
        }
        return results;
    }
}
