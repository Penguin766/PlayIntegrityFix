package es.chiteroman.playintegrityfix;

import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Field;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.KeyStoreSpi;
import java.security.Provider;
import java.security.Security;
import java.util.Properties;

public final class EntryPoint {
    private static final Properties props = new Properties();
    private static final File file = new File("/data/data/com.google.android.gms/pif.prop");

    public static void init() {

        try (Reader reader = new FileReader(file)) {
            props.load(reader);
        } catch (IOException e) {
            LOG("Couldn't load pif.prop file!");
            return;
        }

        LOG("Loaded " + props.size() + " fields!");

        spoofDevice();
        spoofProvider();
    }

    private static void spoofProvider() {
        final String KEYSTORE = "AndroidKeyStore";

        try {
            Provider provider = Security.getProvider(KEYSTORE);
            KeyStore keyStore = KeyStore.getInstance(KEYSTORE);

            Field f = keyStore.getClass().getDeclaredField("keyStoreSpi");
            f.setAccessible(true);
            CustomKeyStoreSpi.keyStoreSpi = (KeyStoreSpi) f.get(keyStore);
            f.setAccessible(false);

            CustomProvider customProvider = new CustomProvider(provider);
            Security.removeProvider(KEYSTORE);
            Security.insertProviderAt(customProvider, 1);

            LOG("Spoof KeyStoreSpi and Provider done!");

        } catch (KeyStoreException e) {
            LOG("Couldn't find KeyStore: " + e);
        } catch (NoSuchFieldException e) {
            LOG("Couldn't find field: " + e);
        } catch (IllegalAccessException e) {
            LOG("Couldn't change access of field: " + e);
        }
    }

    public static void spoofDevice() {
        setProp("PRODUCT", props.getProperty("PRODUCT"));
        setProp("DEVICE", props.getProperty("DEVICE"));
        setProp("MANUFACTURER", props.getProperty("MANUFACTURER"));
        setProp("BRAND", props.getProperty("BRAND"));
        setProp("MODEL", props.getProperty("MODEL"));
        setProp("FINGERPRINT", props.getProperty("FINGERPRINT"));
        setVersionProp("SECURITY_PATCH", props.getProperty("SECURITY_PATCH"));
    }

    private static void setProp(String name, String value) {
        if (name == null || value == null || name.isEmpty() || value.isEmpty()) return;
        try {
            Field field = Build.class.getDeclaredField(name);
            field.setAccessible(true);
            String oldValue = (String) field.get(null);
            field.set(null, value);
            field.setAccessible(false);
            if (value.equals(oldValue)) return;
            LOG(String.format("[%s]: %s -> %s", name, oldValue, value));
        } catch (NoSuchFieldException e) {
            LOG(String.format("Couldn't find '%s' field name.", name));
        } catch (IllegalAccessException e) {
            LOG(String.format("Couldn't modify '%s' field value.", name));
        }
    }

    private static void setVersionProp(String name, String value) {
        if (name == null || value == null || name.isEmpty() || value.isEmpty()) return;
        try {
            Field field = Build.VERSION.class.getDeclaredField(name);
            field.setAccessible(true);
            String oldValue = (String) field.get(null);
            field.set(null, value);
            field.setAccessible(false);
            if (value.equals(oldValue)) return;
            LOG(String.format("[%s]: %s -> %s", name, oldValue, value));
        } catch (NoSuchFieldException e) {
            LOG(String.format("Couldn't find '%s' field name.", name));
        } catch (IllegalAccessException e) {
            LOG(String.format("Couldn't modify '%s' field value.", name));
        }
    }

    public static void LOG(String msg) {
        Log.d("PIF/Java", msg);
    }
}