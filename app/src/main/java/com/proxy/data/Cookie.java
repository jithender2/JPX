package com.proxy.data;

import java.util.Optional;
import static java.util.Objects.requireNonNull;
import java.time.Instant;

/**
 * `Cookie` represents an HTTP cookie, storing its domain, path, name, value,
 * expiry time, and secure flag.  Implements the `NameValue` interface.
 */
public class Cookie implements NameValue {
	private final String domain;
	private final String path;
	private final String name;
	private final String value;
	private final Optional<Instant> expiry;
	private final boolean secure;

	/**
	 * Constructs a new `Cookie` object.
	 *
	 * @param domain The domain of the cookie.
	 * @param path   The path of the cookie.
	 * @param name   The name of the cookie.
	 * @param value  The value of the cookie.
	 * @param expiry The expiry time of the cookie (optional).
	 * @param secure `true` if the cookie is secure, `false` otherwise.
	 * @throws NullPointerException If any of the required parameters are null.
	 */
	public Cookie(String domain, String path, String name, String value, Optional<Instant> expiry, boolean secure) {
		this.domain = requireNonNull(domain);
		this.path = requireNonNull(path);
		this.name = requireNonNull(name);
		this.value = requireNonNull(value);
		this.expiry = requireNonNull(expiry);
		this.secure = secure;
	}

	/**
	 * Checks if the cookie has expired at the given time.
	 *
	 * @param now The current time.
	 * @return `true` if the cookie is expired, `false` otherwise.
	 */
	public boolean expired(Instant now) {
		return expiry.isPresent() && expiry.get().isBefore(now);
	}

	public String domain() {
		return domain;
	}

	public String path() {
		return path;
	}

	@Override
	public String name() {
		return name;
	}

	@Override
	public String value() {
		return value;
	}

	public Optional<Instant> expiry() {
		return expiry;
	}

	public boolean secure() {
		return secure;
	}
}