package com.proxy.data;

import com.proxy.data.MimeType;
import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.util.Optional;
import java.util.Set;

import static java.util.Objects.requireNonNull;

/**
 * `ContentType` represents a MIME type and its associated character set.
 * It provides utility methods for parsing content type strings and checking
 * if the content is text or an image.
 */
public class ContentType {

	private final String rawMimeType;
	private final MimeType mimeType;
	private final Optional<Charset> charset;

	public static ContentType binary = ContentType.parse("application/octet-stream");

	/**
	 * Constructs a `ContentType` object with a specified character set.
	 *
	 * @param rawMimeType The raw MIME type string (e.g., "text/html; charset=UTF-8").
	 * @param charset     The character set (optional).
	 * @throws NullPointerException If `rawMimeType` or `charset` is null.
	 */
	public ContentType(String rawMimeType, Optional<Charset> charset) {
		this.rawMimeType = requireNonNull(rawMimeType);
		this.mimeType = MimeType.parse(rawMimeType);
		this.charset = requireNonNull(charset);
	}

	/**
	 * Constructs a `ContentType` object without a character set.
	 *
	 * @param rawMimeType The raw MIME type string (e.g., "text/html").
	 * @throws NullPointerException If `rawMimeType` is null.
	 */
	public ContentType(String rawMimeType) {
		this.rawMimeType = requireNonNull(rawMimeType);
		this.mimeType = MimeType.parse(rawMimeType);
		this.charset = Optional.empty();
	}

	/**
	 * Parses a content type string into a `ContentType` object.  Handles
	 * the "charset" parameter if present.
	 *
	 * @param str The content type string.
	 * @return A `ContentType` object representing the parsed string.
	 */
	public static ContentType parse(String str) {
		String[] items = str.split(";");
		String type = "";
		String encoding = null;
		for (int i = 0; i < items.length; i++) {
			if (i == 0) {
				type = items[i];
				continue;
			}
			String item = items[i].trim();
			int idx = item.indexOf("=");
			if (idx > 0) {
				if (item.substring(0, idx).trim().equalsIgnoreCase("charset")) {
					encoding = item.substring(idx + 1).trim().replace("\"", "").replace("'", "");
				}
			}
		}
		return new ContentType(type, parseCharset(encoding));
	}

	/**
	 * Parses the charset encoding string into an `Optional<Charset>`.
	 *
	 * @param encoding The charset encoding string.
	 * @return An `Optional<Charset>` representing the parsed charset, or
	 *         empty if the encoding is null or invalid.
	 */
	private static Optional<Charset> parseCharset(String encoding) {
		if (encoding == null) {
			return Optional.empty();
		}
		try {
			return Optional.of(Charset.forName(encoding));
		} catch (IllegalCharsetNameException e) {

			return Optional.empty();
		}
	}

	private static Set<String> textTypes = Set.of("text");
	private static Set<String> textSubTypes = Set.of("json", "x-www-form-urlencoded", "xml", "x-javascript",
			"javascript", "html");

	public boolean isText() {
		return textTypes.contains(mimeType.getType()) || textSubTypes.contains(mimeType.getSubType());
	}

	public boolean isImage() {
		return mimeType.getType().equals("image");
	}

	public String rawMimeType() {
		return rawMimeType;
	}

	public MimeType mimeType() {
		return mimeType;
	}

	public Optional<Charset> charset() {
		return charset;
	}

}