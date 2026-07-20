package com.example.projectaegis.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Generates and guards the 256-bit raw key SQLCipher uses to encrypt the vault database.
 * The key itself never leaves the hardware-backed Android Keystore: what's persisted in
 * SharedPreferences is only the SQLCipher key encrypted (AES-GCM) under a Keystore key
 * that cannot be exported, so a copy of app-private storage alone is useless without the
 * same device's TEE/StrongBox.
 */
public final class VaultKeyProvider {

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEYSTORE_ALIAS = "aegis_vault_db_key";
    private static final String PREFS_NAME = "vault_key_prefs";
    private static final String PREF_IV = "wrapped_key_iv";
    private static final String PREF_WRAPPED_KEY = "wrapped_vault_key";
    private static final int RAW_KEY_LENGTH_BYTES = 32; // 256-bit SQLCipher raw key
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private VaultKeyProvider() {
    }

    public static synchronized byte[] getOrCreateRawKey(Context context) {
        try {
            SecretKey wrappingKey = getOrCreateWrappingKey();
            SharedPreferences prefs = context.getApplicationContext()
                    .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

            if (!prefs.contains(PREF_WRAPPED_KEY)) {
                byte[] rawKey = new byte[RAW_KEY_LENGTH_BYTES];
                new SecureRandom().nextBytes(rawKey);
                wrapAndStore(wrappingKey, rawKey, prefs);
                return rawKey;
            }
            return unwrap(wrappingKey, prefs);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to access vault database key", e);
        }
    }

    private static SecretKey getOrCreateWrappingKey() throws GeneralSecurityException {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        try {
            keyStore.load(null);
        } catch (Exception e) {
            throw new GeneralSecurityException(e);
        }

        if (!keyStore.containsAlias(KEYSTORE_ALIAS)) {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEYSTORE_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build();
            keyGenerator.init(spec);
            keyGenerator.generateKey();
        }

        return (SecretKey) keyStore.getKey(KEYSTORE_ALIAS, null);
    }

    private static void wrapAndStore(SecretKey wrappingKey, byte[] rawKey, SharedPreferences prefs)
            throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey);
        byte[] wrapped = cipher.doFinal(rawKey);
        prefs.edit()
                .putString(PREF_IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                .putString(PREF_WRAPPED_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
                .apply();
    }

    private static byte[] unwrap(SecretKey wrappingKey, SharedPreferences prefs) throws GeneralSecurityException {
        byte[] iv = Base64.decode(prefs.getString(PREF_IV, ""), Base64.NO_WRAP);
        byte[] wrapped = Base64.decode(prefs.getString(PREF_WRAPPED_KEY, ""), Base64.NO_WRAP);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, wrappingKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
        return cipher.doFinal(wrapped);
    }
}
