package com.proxy.setting;

import android.os.Environment;
import android.content.Context;
import java.io.File;
import java.io.Serializable;

import static java.util.Objects.requireNonNull;

/**
 * The proxy mainSetting infos
 *
 * @author Liu Dong
 */

public class ServerSetting implements Serializable {
	private static final long serialVersionUID = -1828819182428842928L;
	private final String host;
	private final int port;
	// timeout in seconds
	private final int timeout;

	public ServerSetting(String host, int port, int timeout) {
		this.host = requireNonNull(host);
		this.port = port;
		this.timeout = timeout;
	}

	/**
	 * Creates a new `ServerSetting` object with default values (empty host,
	 * port 8080, timeout 30 minutes (1800 seconds)).
	 *
	 * @return A new `ServerSetting` object with default values.
	 */
	public static ServerSetting newDefaultServerSetting() {
		return new ServerSetting("", 8080, 1800);
	}

	public String host() {
		return host;
	}

	public int port() {
		return port;
	}

	public int timeout() {
		return timeout;
	}
}