package com.proxy.beautifier;

import com.proxy.utils.BodyType;
import org.jsoup.Jsoup;

import java.nio.charset.Charset;
import org.jsoup.nodes.Document;

/**
 * `HtmlBeautifier` is a `Beautifier` implementation that formats HTML content
 * into a more readable, indented format using the Jsoup library.
 */
public class HtmlBeautifier implements Beautifier {
	public HtmlBeautifier() {
	}

	/**
	 * Checks if this beautifier accepts the given `BodyType`.
	 *
	 * @param type The `BodyType` to check.
	 * @return `true` if the `BodyType` is `BodyType.html`, `false` otherwise.
	 */
	@Override
	public boolean accept(BodyType type) {
		return type == BodyType.html;
	}

	/**
	 * Beautifies the given HTML string using Jsoup to parse and format the
	 * HTML with indentation.
	 *
	 * @param s       The HTML string to beautify.
	 * @param charset The character set of the HTML string (currently unused
	 *                by this implementation, but included to fulfill the
	 *                `Beautifier` interface).
	 * @return The beautified HTML string.
	 */
	@Override
	public String beautify(String s, Charset charset) {
		Document doc = Jsoup.parse(s);
		doc.outputSettings().indentAmount(1);
		return doc.toString();
	}
}