package com.example.projectaegis.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class VaultRepository {

    private static volatile VaultRepository instance;

    private final VaultDbHelper dbHelper;

    private VaultRepository(Context context) {
        this.dbHelper = new VaultDbHelper(context);
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
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = VaultDbHelper.toValues(accountName, url, username, password, System.currentTimeMillis());
        return db.insert(VaultDbHelper.TABLE, null, values);
    }

    public void update(long id, String accountName, String url, String username, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = VaultDbHelper.toValues(accountName, url, username, password, System.currentTimeMillis());
        db.update(VaultDbHelper.TABLE, values, VaultDbHelper.COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(VaultDbHelper.TABLE, VaultDbHelper.COL_ID + "=?", new String[]{String.valueOf(id)});
    }

    public Credential getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(VaultDbHelper.TABLE, null, VaultDbHelper.COL_ID + "=?",
                new String[]{String.valueOf(id)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return VaultDbHelper.fromCursor(cursor);
            }
            return null;
        }
    }

    public List<Credential> getAll() {
        List<Credential> results = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query(VaultDbHelper.TABLE, null, null, null, null, null,
                VaultDbHelper.COL_ACCOUNT_NAME + " COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                results.add(VaultDbHelper.fromCursor(cursor));
            }
        }
        return results;
    }
}
