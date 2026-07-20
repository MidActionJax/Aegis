package com.example.projectaegis.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

// INSECURE (v1, planted flaw): plain SQLiteOpenHelper, not SQLCipher.
// The database file at /data/data/com.example.projectaegis/databases/aegis_vault.db
// is stored on disk with passwords in cleartext. Fix in v2: swap this for a
// SQLCipher-backed SQLiteOpenHelper keyed from Android Keystore.
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

    public VaultDbHelper(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, DB_VERSION);
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
