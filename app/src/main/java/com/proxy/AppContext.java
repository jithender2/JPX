package com.proxy;

import android.content.Context;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.Objects;
import com.proxy.setting.KeyStoreSetting;
import com.proxy.setting.ProxySetting;
import com.proxy.setting.ServerSetting;

/**
 * `AppContext` is a singleton class responsible for holding and managing the application's
 * configuration and shared resources.  It provides access to server settings, keystore settings,
 * proxy settings, and the SSL context manager.
 *
 * This class follows the Singleton pattern to ensure only one instance exists throughout the
 * application's lifecycle, providing a central point for accessing configuration.
 */
public class AppContext {

	/**
	 * The server settings for the application.  Volatile ensures visibility across threads.
	 */
	private volatile ServerSetting serverSetting;

	/**
	 * The keystore settings for managing SSL certificates. Volatile ensures visibility across threads.
	 */
	private volatile KeyStoreSetting keyStoreSetting;

	/**
	 * The proxy settings for the application. Volatile ensures visibility across threads.
	 */
	private volatile ProxySetting proxySetting;

	/**
	 * Manages the SSL context for secure communication.
	 */
	private volatile ServerSSLContextManager sslContextManager;

	/**
	 * The Android Context.  Used for accessing resources and other Android-specific functionality.
	 */
	private Context context;

	private static AppContext instance;

	public AppContext(Context context) {
		this.context = context;
		initSslContextManager();
	}

	/*public void keyStoreSetting(KeyStoreSetting setting) {
		Objects.requireNonNull(setting);
		String path = setting.usedKeyStore();
		if (sslContextManager == null || !path.equals(sslContextManager.getRootKeyStorePath())) {
			this.sslContextManager = new ServerSSLContextManager(context, path, setting.usedPassword().toCharArray());
		}
		this.keyStoreSetting = setting;
	}*/

	public void serverSetting(ServerSetting serverSetting) {
		this.serverSetting = Objects.requireNonNull(serverSetting);
	}

	public void proxySetting(ProxySetting proxySetting) {
		this.proxySetting = Objects.requireNonNull(proxySetting);
	}

	public static AppContext getInstance() {
		return instance;
	}

	public ServerSetting getServerSetting() {
		return serverSetting != null ? serverSetting : ServerSetting.newDefaultServerSetting();
	}

	public KeyStoreSetting getKeyStoreSetting() {
		return keyStoreSetting;
	}

	public ProxySetting getProxySetting() {
		return proxySetting != null ? proxySetting : ProxySetting.newDefaultProxySetting();
	}

	public ServerSSLContextManager getSslContextManager() {
		return sslContextManager;
	}

	/**
	 * Initializes the `sslContextManager` with default values.  This is called in the
	 * constructor. Consider moving default keystore file name to a constant.
	 */
	private void initSslContextManager() {
		this.sslContextManager = new ServerSSLContextManager(this.context, "JPX.pem",
				KeyStoreSetting.defaultKeyStorePassword());
	}
}
