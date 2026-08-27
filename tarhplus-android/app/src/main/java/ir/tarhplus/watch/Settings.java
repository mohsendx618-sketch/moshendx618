package ir.tarhplus.watch;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class Settings {
    private static final String ALIAS = "tarhplus_watch_local_key_v1";
    final SharedPreferences prefs;
    Settings(Context context) { prefs = context.getSharedPreferences("watch_private", Context.MODE_PRIVATE); }
    String city() { return prefs.getString("city", "آباده"); }
    String major() { return prefs.getString("major", "اتاق عمل"); }
    String reportUrl() { String value = secret("report_url"); return value.isEmpty() ? UrlPolicy.DEFAULT_REPORT : value; }

    private static synchronized SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance("AndroidKeyStore");
        store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey) store.getKey(ALIAS, null);
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true).build());
        return generator.generateKey();
    }

    void putSecret(String name, String value) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, key());
        String stored = Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP) + ":"
                + Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)), Base64.NO_WRAP);
        if (!prefs.edit().putString("secure_" + name, stored).commit()) throw new java.io.IOException("Could not save settings");
    }

    String secret(String name) {
        String raw = prefs.getString("secure_" + name, "");
        if (raw.isEmpty()) return "";
        try {
            String[] parts = raw.split(":", 2);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }

    void invalidateReport() {
        prefs.edit().putBoolean("validated", false).putBoolean("daily", false)
                .putLong("generation", prefs.getLong("generation", 0) + 1)
                .remove("fingerprint").remove("telegram_fingerprint").remove("last_warning").apply();
    }
}
