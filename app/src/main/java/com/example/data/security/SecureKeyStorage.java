package com.example.data.security;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

public class SecureKeyStorage {
    private static final String PREFS_FILE = "omnichat_secure_vault";
    private SharedPreferences encryptedPreferences;

    public SecureKeyStorage(@NonNull Context context) {
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();

            encryptedPreferences = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    PREFS_FILE,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (Exception e) {
            // Fallback to mode private if hardware keystore error on older ROMs
            encryptedPreferences = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        }
    }

    public void storeApiKey(@NonNull String providerId, @Nullable String apiKey) {
        if (apiKey == null) {
            encryptedPreferences.edit().remove("key_" + providerId).apply();
        } else {
            encryptedPreferences.edit().putString("key_" + providerId, apiKey).apply();
        }
    }

    @Nullable
    public String getApiKey(@NonNull String providerId) {
        return encryptedPreferences.getString("key_" + providerId, null);
    }

    public void removeApiKey(@NonNull String providerId) {
        encryptedPreferences.edit().remove("key_" + providerId).apply();
    }
}
