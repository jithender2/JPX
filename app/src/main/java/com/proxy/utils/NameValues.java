package com.proxy.utils;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import com.proxy.data.Parameter;
import java.net.URLDecoder;

import com.proxy.data.NameValue;
import java.util.List;

/**
 * `NameValues` provides utility methods for working with lists of name-value pairs.
 * It includes methods for formatting name-value pairs for aligned text output and
 * parsing URL-encoded parameters.
 */
public class NameValues {
	/**
	 * Formats a list of `NameValue` objects into aligned text, with a specified
	 * separator string.  The names are padded with spaces to achieve alignment.
	 * The maximum name length considered for alignment is 20 characters.
	 *
	 * @param list The list of `NameValue` objects to format.
	 * @param str  The separator string to insert between the name and value.
	 * @return A list of formatted strings, where each string represents a
	 *         `NameValue` pair.
	 */
	public static List<String> toAlignText(List<? extends NameValue> list, String str) {
		int min = Math.min(list.stream().mapToInt(nameValue -> nameValue.name().length()).max().orElse(0), 20);
		List<String> arrayList = new ArrayList<>();
		for (NameValue nameValue : list) {
			int paddingCount = Math.max(min - nameValue.name().length(), 0);
			StringBuilder padding = new StringBuilder(paddingCount);
			for (int i = 0; i < paddingCount; i++) {
				padding.append(' ');
			}
			arrayList.add(nameValue.name() + padding.toString() + str + nameValue.value());
		}
		return arrayList;
	}

	/**
	 * Parse url encoded key values.
	 */
	public static List<? extends NameValue> parseUrlEncodedParams(String text, Charset charset) {
		if (text.isEmpty()) {
			return List.of();
		}
		ArrayList<Parameter> params = new ArrayList<Parameter>();
		for (String segment : text.split("&")) {
			segment = segment.trim();
			if (segment.isEmpty()) {
				continue;
			}
			int idx = segment.indexOf("=");
			if (idx >= 0) {
				String name = segment.substring(0, idx).trim();
				String value = segment.substring(idx + 1).trim();
				try {
					value = URLDecoder.decode(value, charset.toString());
					name = URLDecoder.decode(name, charset.toString());
					params.add(new Parameter(name, value));
				} catch (UnsupportedEncodingException e) {
				}

			} else {
				try {
					String value = URLDecoder.decode(segment, charset.toString());
					params.add(new Parameter("", value));
				} catch (UnsupportedEncodingException e) {

				}
			}
		}
		return params;
	}
}