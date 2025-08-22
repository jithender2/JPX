package com.proxy.utils;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Networks {

	public static final int HOST_TYPE_IPV6 = 0;
	public static final int HOST_TYPE_IPV4 = 1;
	public static final int HOST_TYPE_DOMAIN = 2;

	/**
	 * Determines the host type (IPv6, IPv4, or Domain) of a given host string.
	 *
	 * @param host The host string to check.
	 * @return The host type (HOST_TYPE_IPV6, HOST_TYPE_IPV4, or HOST_TYPE_DOMAIN).
	 */
	public static int getHostType(String host) {
		if (host.contains(":") && !host.contains(".")) {
			return HOST_TYPE_IPV6;
		}
		if (host.matches("^[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}")) {
			return HOST_TYPE_IPV4;
		}
		return HOST_TYPE_DOMAIN;
	}

	/**
	 * Creates a wildcard version of a host string.
	 * If the host is not a domain or has fewer than 4 parts, it returns the original host.
	 * Otherwise, it replaces the first part of the domain with a wildcard.
	 * For example:  "sub.example.com" becomes "*.example.com".
	 *
	 * @param host The host string to wildcard.
	 * @return The wildcarded host string, or the original host if it's not a domain or too short.
	 */
	public static String wildcardHost(String host) {
		if (getHostType(host) != HOST_TYPE_DOMAIN) {
			return host;
		}
		String[] items = host.split("\\.");
		if (items.length <= 3) {
			return host;
		}
		return "*." + items[items.length - 3] + "." + items[items.length - 2] + "." + items[items.length - 1];
	}

	/**
	 * Creates a generic multi-CDNS version of a host string.
	 * This function is designed to handle specific host naming conventions, likely related to CDNs.
	 * It checks if the first part of the hostname starts with a letter, ends with a digit, and has digits in between.
	 * If so, it replaces the digit part with a wildcard.
	 * For example: "abc123xyz.example.com" becomes "abc*.example.com".
	 * If the host does not match this pattern, it returns the original host.
	 *
	 * @param host The host string to process.
	 * @return The generic multi-CDNS host string, or the original host if it doesn't match the pattern.
	 */
	public static String genericMultiCDNS(String host) {
		int idx = host.indexOf(".");
		if (idx < 2) {
			return host;
		}
		String first = host.substring(0, idx);
		if (!Character.isLetter(first.charAt(0))) {
			return host;
		}
		char c = first.charAt(first.length() - 1);
		if (!Character.isDigit(c)) {
			return host;
		}
		int firstEnd = first.length() - 2;
		while (Character.isDigit(first.charAt(firstEnd))) {
			firstEnd--;
		}
		return first.substring(0, firstEnd + 1) + "*." + host.substring(idx + 1);
	}

}