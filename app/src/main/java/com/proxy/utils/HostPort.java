package com.proxy.utils;

import java.io.Serializable;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * `HostPort` represents a host and port combination. It supports parsing
 * host and port from strings, distinguishing between IPv4, IPv6, and domain
 * names.  It provides methods for accessing the host and port, checking if a
 * port is present, and formatting the host and port as a string.
 */
public class HostPort implements Serializable {
	private final String host;
	private final int port;
	private final HostType type;

	public enum HostType {
		IPv4, IPv6, DOMAIN
	}

	private HostPort(String host, int port, HostType type) {
		this.host = host;
		this.port = port;
		this.type = type;
	}

	public static HostPort of(String host) {
		Objects.requireNonNull(host);
		return new HostPort(host, -1, getHostType(host));
	}

	public static HostPort of(String host, int port) {
		Objects.requireNonNull(host);
		checkPort(port);
		return new HostPort(host, port, getHostType(host));
	}

	/**
	 * Parses a string representation of a host and port.  Supports IPv4, IPv6,
	 * and domain names.
	 *
	 * @param str The string to parse.
	 * @return A `HostPort` instance.
	 * @throws NullPointerException If `str` is null.
	 * @throws IllegalArgumentException If `str` is not a valid host/port string.
	 */
	public static HostPort parse(String str) {
		Objects.requireNonNull(str);
		int lastColon = str.lastIndexOf(':');

		if (lastColon < 0) {
			return of(str);
		}

		if (str.lastIndexOf(':', lastColon - 1) >= 0) {
			if (str.startsWith("[") && str.endsWith("]")) {
				return of(str.substring(1, str.length() - 1));
			}
			return of(str);
		}

		try {
			return of(trimIpv6Host(str.substring(0, lastColon)), Integer.parseInt(str.substring(lastColon + 1)));
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("Invalid address: " + str, e);
		}
	}

	private static String trimIpv6Host(String str) {
		if (str.startsWith("[") && str.endsWith("]")) {
			return str.substring(1, str.length() - 1);
		}
		return str;
	}

	private static HostType getHostType(String str) {
		if (isIPv4(str))
			return HostType.IPv4;
		if (isIPv6(str))
			return HostType.IPv6;
		if (isDomain(str))
			return HostType.DOMAIN;
		throw new IllegalArgumentException("Illegal host: " + str);
	}

	private static boolean isIPv4(String str) {
		String[] parts = str.split("\\.");
		if (parts.length != 4)
			return false;
		for (String part : parts) {
			try {
				int num = Integer.parseInt(part);
				if (num < 0 || num > 255)
					return false;
			} catch (NumberFormatException e) {
				return false;
			}
		}
		return true;
	}

	private static boolean isIPv6(String str) {
		String[] parts = str.split(":");
		if (parts.length <= 2)
			return false;
		for (String part : parts) {
			if (!part.isEmpty()) {
				try {
					int num = Integer.parseInt(part, 16);
					if (num < 0 || num > 0xFFFF)
						return false;
				} catch (NumberFormatException e) {
					if (!isIPv4(part))
						return false;
				}
			}
		}
		return true;
	}

	private static boolean isDomain(String str) {
		if (str.length() > 253)
			return false;
		for (String label : str.split("\\.")) {
			if (!isDomainLabel(label))
				return false;
		}
		return true;
	}

	private static boolean isDomainLabel(String label) {
		return !label.isEmpty() && label.length() <= 63 && Character.isLetterOrDigit(label.charAt(0))
				&& Character.isLetterOrDigit(label.charAt(label.length() - 1))
				&& label.chars().allMatch(c -> Character.isLetterOrDigit(c) || c == '-');
	}

	public HostPort withPort(int port) {
		return new HostPort(this.host, port, this.type);
	}

	public HostType getType() {
		return this.type;
	}

	public String host() {
		return this.host;
	}

	public boolean hasPort() {
		return this.port != -1;
	}

	public int ensurePort() {
		if (this.port == -1)
			throw new IllegalStateException("Port not set");
		return this.port;
	}

	public OptionalInt port() {
		return this.port == -1 ? OptionalInt.empty() : OptionalInt.of(this.port);
	}

	public String toStringWithDefault(int defaultPort) {
		checkPort(defaultPort);
		return (this.port == -1 || this.port == defaultPort) ? formatHost(this.host, this.type)
				: joinHostAndPort(this.host, this.port, this.type);
	}

	public String toStringWithPort(int defaultPort) {
		checkPort(defaultPort);
		return joinHostAndPort(this.host, this.port == -1 ? defaultPort : this.port, this.type);
	}

	@Override
	public String toString() {
		return this.port == -1 ? formatHost(this.host, this.type) : joinHostAndPort(this.host, this.port, this.type);
	}

	private static int checkPort(int port) {
		if (port < 0 || port > 65535)
			throw new IllegalArgumentException("Illegal port: " + port);
		return port;
	}

	private static String joinHostAndPort(String host, int port, HostType type) {
		return (type == HostType.IPv6 ? "[" + host + "]" : host) + ":" + port;
	}

	private static String formatHost(String host, HostType type) {
		return type == HostType.IPv6 ? "[" + host + "]" : host;
	}
}
