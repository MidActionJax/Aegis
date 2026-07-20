package com.example.projectaegis.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

// SECURE (v2): the PIN is no longer stored as plaintext. setPin() derives a salted
// PBKDF2WithHmacSHA256 hash and only that (plus the random salt) is persisted, so
// reading auth_prefs off the device no longer hands over the PIN directly. checkPin()
// also now enforces a failed-attempt lockout (v1 had none), so the gate can't be
// brute-forced by an attacker who does obtain the stored hash+salt.
public class PinManager {

    private static final String PREFS_NAME = "auth_prefs";
    private static final String KEY_SALT = "pin_salt";
    private static final String KEY_HASH = "pin_hash";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts";
    private static final String KEY_LOCKOUT_UNTIL = "lockout_until";

    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int PBKDF2_ITERATIONS = 120_000;
    private static final int HASH_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;

    public static final int MAX_ATTEMPTS = 5;
    public static final long LOCKOUT_DURATION_MS = 30_000;

    private final SharedPreferences prefs;

    public PinManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public boolean isPinSet() {
        return prefs.contains(KEY_HASH);
    }

    public void setPin(String pin) {
        byte[] salt = new byte[SALT_LENGTH_BYTES];
        new SecureRandom().nextBytes(salt);
        byte[] hash = hash(pin, salt);
        prefs.edit()
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(KEY_HASH, Base64.encodeToString(hash, Base64.NO_WRAP))
                .putInt(KEY_FAILED_ATTEMPTS, 0)
                .putLong(KEY_LOCKOUT_UNTIL, 0)
                .apply();
    }

    public boolean isLockedOut() {
        return prefs.getLong(KEY_LOCKOUT_UNTIL, 0) > System.currentTimeMillis();
    }

    public long getLockoutRemainingSeconds() {
        long remainingMs = prefs.getLong(KEY_LOCKOUT_UNTIL, 0) - System.currentTimeMillis();
        return Math.max(0, (remainingMs + 999) / 1000);
    }

    public boolean checkPin(String pin) {
        if (isLockedOut()) {
            return false;
        }

        String saltEncoded = prefs.getString(KEY_SALT, null);
        String hashEncoded = prefs.getString(KEY_HASH, null);
        if (saltEncoded == null || hashEncoded == null) {
            return false;
        }

        byte[] salt = Base64.decode(saltEncoded, Base64.NO_WRAP);
        byte[] expected = Base64.decode(hashEncoded, Base64.NO_WRAP);
        byte[] actual = hash(pin, salt);
        boolean matches = MessageDigest.isEqual(expected, actual);

        if (matches) {
            prefs.edit().putInt(KEY_FAILED_ATTEMPTS, 0).putLong(KEY_LOCKOUT_UNTIL, 0).apply();
        } else {
            registerFailedAttempt();
        }
        return matches;
    }

    private void registerFailedAttempt() {
        int attempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1;
        SharedPreferences.Editor editor = prefs.edit();
        if (attempts >= MAX_ATTEMPTS) {
            editor.putInt(KEY_FAILED_ATTEMPTS, 0);
            editor.putLong(KEY_LOCKOUT_UNTIL, System.currentTimeMillis() + LOCKOUT_DURATION_MS);
        } else {
            editor.putInt(KEY_FAILED_ATTEMPTS, attempts);
        }
        editor.apply();
    }

    private byte[] hash(String pin, byte[] salt) {
        char[] pinChars = pin.toCharArray();
        try {
            PBEKeySpec spec = new PBEKeySpec(pinChars, salt, PBKDF2_ITERATIONS, HASH_LENGTH_BITS);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to hash PIN", e);
        } finally {
            Arrays.fill(pinChars, '\0');
        }
    }
}
