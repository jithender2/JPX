package com.proxy.setting;

import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * `ProxySetting` represents the configuration for a proxy server. It stores
 * information such as the proxy type (SOCKS5, SOCKS4, HTTP), host, port,
 * username, password, and whether the proxy should be used.  Implements
 * `Serializable` to allow the object to be saved and restored.
 */
public class ProxySetting implements Serializable {

	private static final long serialVersionUID = 7257755061846443485L;
	private String type;
	private String host;
	private int port;
	private String user;
	// should use char[]?
	private String password;
	private boolean use;

	public static final String TYPE_SOCKS5 = "socks5";
	public static final String TYPE_SOCKS4 = "socks4";
	public static final String TYPE_HTTP = "http";

	public ProxySetting(String type, String host, int port, String user, String password, boolean use) {
		this.type = requireNonNull(type);
		this.host = requireNonNull(host);
		this.port = port;
		this.user = requireNonNull(user);
		this.password = requireNonNull(password);
		this.use = use;
	}

	public static ProxySetting newDefaultProxySetting() {
		return new ProxySetting(TYPE_SOCKS5, "", 0, "", "", false);
	}

	public String type() {
		return type;
	}

	public String host() {
		return host;
	}

	public int port() {
		return port;
	}

	public String user() {
		return user;
	}

	public String password() {
		return password;
	}

	public boolean use() {
		return use;
	}
}