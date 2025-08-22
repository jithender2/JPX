package com.proxy.utils;

/**
 * `NetworkInfo` represents information about a network interface,
 * specifically its name and IP address.  It's a simple data holder class.
 */
public class NetworkInfo {
	private String name;
	private String ip;

	public NetworkInfo(String name, String ip) {
		this.name = name;
		this.ip = ip;
	}

	public String getName() {
		return name;
	}

	public String getIp() {
		return ip;
	}

	@Override
	public String toString() {
		return "NetworkInfo{" + "name='" + name + '\'' + ", ip='" + ip + '\'' + '}';
	}
}