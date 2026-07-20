package com.example.projectaegis.auth;

import android.content.Context;
import android.content.SharedPreferences;

// INSECURE (v1, planted flaw): the PIN is stored as plaintext in SharedPreferences
// with no hashing/salting, and there is no failed-attempt lockout, so the vault
// gate can be brute-forced offline once the prefs file is read. Fix in v2: hash
// the PIN (e.g. PBKDF2) before storing it, and add an attempt counter/backoff.
public class PinManager {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_PIN = "pin";

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPinSet() {
        return prefs.contains(KEY_PIN);
    }

    public void setPin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public boolean checkPin(String pin) {
        String stored = prefs.getString(KEY_PIN, null);
        return stored != null && stored.equals(pin);
    }
}
