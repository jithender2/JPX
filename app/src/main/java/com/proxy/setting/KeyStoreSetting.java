package com.proxy.setting;

import android.content.Context;
import java.io.File;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * Key store setting
 *
 * @author Liu Dong
 */
public class KeyStoreSetting implements Serializable {
    private static final long serialVersionUID = -8001899659204205513L;
    // path for keyStore file
    private String keyStore;

    private char[] keyStorePassword;
    private boolean useCustom;
	static Context context;

    public KeyStoreSetting(Context context2,String keyStore, char[] keyStorePassword, boolean useCustom) {
        this.keyStore = requireNonNull(keyStore);
        this.keyStorePassword = requireNonNull(keyStorePassword);
        this.useCustom = useCustom;
		context=context2;
		
    }

    /**
     * The default key store file path
     */
  //  public static String defaultKeyStorePath() {
     //   return Settings.getParentDirectory(context).getAbsolutePath().toString();
    //}

    /**
     * The default key store password
     */
    public static char[] defaultKeyStorePassword() {
        return Settings.rootKeyStorePassword;
    }

    

    public char[] usedPassword() {
        if (useCustom) {
            return keyStorePassword;
        }
        return defaultKeyStorePassword();
    }

   // public static KeyStoreSetting newDefaultKeyStoreSetting() {
       // return new KeyStoreSetting(context,"", "", false);
    //}

    public String keyStore() {
        return keyStore;
    }

    public char[] keyStorePassword() {
        return keyStorePassword;
    }

    public boolean useCustom() {
        return useCustom;
    }
}