package com.proxy.beautifier;

import com.proxy.data.NameValue;
import com.proxy.data.Parameter;
import com.proxy.utils.NameValues;
import com.proxy.utils.BodyType;
import java.nio.charset.Charset;
import static com.proxy.utils.NameValues.parseUrlEncodedParams;
import java.util.ArrayList;
import java.util.List;

/**
 * `FormEncodedBeautifier` is a `Beautifier` implementation that formats
 * URL-encoded form data (content type `application/x-www-form-urlencoded`) into
 * a more readable, aligned text format.
 */
public class FormEncodedBeautifier implements Beautifier {

	/**
	 * Checks if this beautifier can handle the given `BodyType`.
	 *
	 * @param type The `BodyType` to check.
	 * @return `true` if the `BodyType` is `BodyType.www_form` (URL-encoded form data),
	 *         `false` otherwise.
	 */
	@Override
	public boolean accept(BodyType type) {
		return type == BodyType.www_form;
	}

	/**
	 * Beautifies the given URL-encoded form data string.  Parses the string
	 * into name-value pairs and formats them with aligned text.
	 *
	 * @param s       The URL-encoded form data string.
	 * @param charset The character set used to decode the string.
	 * @return The beautified string, or the original string if it's empty.
	 */
	@Override
	public String beautify(String s, Charset charset) {
		if (s.isEmpty()) {
			return s;
		}
		List<? extends NameValue> nameValues = parseUrlEncodedParams(s, charset);
		return String.join("\n", NameValues.toAlignText(nameValues, " = "));
	}
}