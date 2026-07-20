package com.example.projectaegis.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

import net.zetetic.database.sqlcipher.SQLiteDatabase;
import net.zetetic.database.sqlcipher.SQLiteOpenHelper;

// SECURE (v2): backed by SQLCipher instead of plain SQLite. The database file on disk
// is encrypted with the 256-bit key VaultKeyProvider hands VaultRepository at open time;
// without it, the file is unreadable ciphertext (fixes the v1 plaintext-storage flaw).
public class VaultDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "aegis_vault.db";
    private static final int DB_VERSION = 1;

    static final String TABLE = "credentials";
    static final String COL_ID = "id";
    static final String COL_ACCOUNT_NAME = "account_name";
    static final String COL_URL = "url";
    static final String COL_USERNAME = "username";
    static final String COL_PASSWORD = "password";
    static final String COL_UPDATED_AT = "updated_at";

    public VaultDbHelper(Context context, byte[] password) {
        super(context.getApplicationContext(), DB_NAME, password, null, DB_VERSION,
                0, null, null, false);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_ACCOUNT_NAME + " TEXT NOT NULL, " +
                COL_URL + " TEXT, " +
                COL_USERNAME + " TEXT NOT NULL, " +
                COL_PASSWORD + " TEXT NOT NULL, " +
                COL_UPDATED_AT + " INTEGER NOT NULL)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }

    static ContentValues toValues(String accountName, String url, String username, String password, long updatedAt) {
        ContentValues values = new ContentValues();
        values.put(COL_ACCOUNT_NAME, accountName);
        values.put(COL_URL, url);
        values.put(COL_USERNAME, username);
        values.put(COL_PASSWORD, password);
        values.put(COL_UPDATED_AT, updatedAt);
        return values;
    }

    static Credential fromCursor(Cursor cursor) {
        return new Credential(
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_ACCOUNT_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_URL)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_USERNAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_PASSWORD)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_UPDATED_AT))
        );
    }
}
