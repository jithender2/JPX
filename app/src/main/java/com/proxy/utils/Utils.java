package com.proxy.utils;

import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.JsonBeautifier;
import com.proxy.listener.SetLogger;
import java.io.InputStreamReader;
import java.io.IOException;
import com.proxy.store.Body;
import java.io.Reader;
import java.nio.charset.Charset;
import com.proxy.beautifier.Beautifier;
import io.netty.util.CharsetUtil;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.zip.Inflater;
import java.net.URISyntaxException;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import org.brotli.dec.BrotliInputStream;
import org.tukaani.xz.LZMAInputStream;

public class Utils {
	public static final int REQUEST = 0;
	public static final int RESPONSE = 1;
	public static final String CONTENT_ENCODING_HEADER = "Content-Encoding";
	public static final String CONTENT_TYPE_HEADER = "Content-Type"; // Corrected capitalization
	public static final String CONTENT_LENGTH_HEADER = "content-length"; // Corrected capitalization
	public static final List<Beautifier> beautifiers = List.of(new XmlBeautifier(), new HtmlBeautifier(),
			new JsonBeautifier());

	public static String convertHttp2ToHttp1(String http2HeadersText) {
		StringBuilder http1Request = new StringBuilder();
		StringBuilder http1Headers = new StringBuilder();
		String scheme = null;
		String method = null;
		String path = null;
		String authority = null;

		String[] lines = http2HeadersText.split("\\r?\\n");

		for (String line : lines) {
			line = line.trim();
			if (!line.contains(":")) {

				continue;
			}

			// Find the index of the first colon after the first character
			int idx = line.substring(1).indexOf(":") + 1;
			if (idx <= 0) {
				continue; // Invalid header line, skip it
			}

			String name = line.substring(0, idx).trim();
			String value = line.substring(idx + 1).trim();

			if (name.equalsIgnoreCase(":scheme")) {
				scheme = value;
			} else if (name.equalsIgnoreCase(":method")) {
				method = value;
			} else if (name.equalsIgnoreCase(":path")) {
				path = value;
			} else if (name.equalsIgnoreCase(":authority")) {
				authority = value;
			} else {
				http1Headers.append(name).append(": ").append(value).append("\r\n");
			}
		}

		if (scheme == null || method == null || path == null || authority == null) {
			throw new IllegalArgumentException("Missing required pseudo-headers (scheme, method, authority, or path)");
		}

		String rawPath, query = null;
		int queryIndex = path.indexOf('?');
		if (queryIndex >= 0) {
			rawPath = path.substring(0, queryIndex);
			query = path.substring(queryIndex + 1);
		} else {
			rawPath = path;
		}

		try {
			URI uri = new URI(scheme, authority, rawPath, query, null);
			query = query == null ? "" : "?" + query;
			http1Request.append(method).append(" ").append(uri.getRawPath() + query).append(" HTTP/2\r\n");
			http1Request.append("Host: ").append(uri.getHost()).append("\r\n"); // Add Host header
			http1Request.append(http1Headers);
			http1Request.append("\r\n");
		} catch (URISyntaxException e) {

		}

		return http1Request.toString();
	}

	public static String extractPath(String httpRequest) {
		// Extract the first line and get the path part
		String firstLine = httpRequest.split("\n")[0];
		String[] parts = firstLine.split(" ");
		return parts.length > 1 ? parts[1] : null; // Return the second part (the path)
	}

	public static String extractHost(String httpRequest) {
		// Look for the "Host" header in the request
		String[] lines = httpRequest.split("\n");
		for (String line : lines) {
			if (line.toLowerCase().startsWith("host")) {
				return line.split(":")[1].trim(); // Return the value after "Host:"
			}
		}
		return null; // Return null if Host is not found
	}

	public static SpannableStringBuilder highlightAndFormatHttpRequest(String requestEditText) {

		StringBuilder formattedText = new StringBuilder();

		String[] lines = requestEditText.split("\n");
		int starting = 0;
		boolean issBody = false; // Track when we reach the body section
		for (String line : lines) {
			if (line.trim().isEmpty()) {
				formattedText.append("\n");
				starting++;
				issBody = true; // Empty line means headers are done, now it's body
				continue;
			}
			if (!issBody && line.contains(":")) {

				int colonIndex = line.indexOf(":");
				if (line.startsWith(":") && colonIndex != -1) {
					int secondColonIndex = line.indexOf(":", colonIndex + 1);
					if (secondColonIndex != -1) {
						colonIndex = secondColonIndex;
					}
				}

				String headerName = line.substring(0, colonIndex).trim();
				String headerValue = line.substring(colonIndex + 1).trim();

				// For HTTP/2 pseudo-headers, maintain a different format if needed
				if (headerName.startsWith(":")) {
					formattedText.append(headerName.trim()).append(":  ").append(headerValue.trim()).append("\n");
				} else {
					formattedText.append(headerName.trim()).append(":  ").append(headerValue.trim()).append("\n");
				}
			} else {
				// Keep non-header lines (like HTTP method and path) unchanged
				formattedText.append(line).append("\n");
			}
			starting += line.length() + 1;
		}

		SpannableStringBuilder stringBuilder = new SpannableStringBuilder(formattedText);
		lines = formattedText.toString().split("\n");

		int start = 0; // Track the cumulative position in the text

		boolean isBody = false;
		for (String line : lines) {
			if (line.trim().isEmpty()) {

				start++;
				isBody = true; // Empty line means headers are done, now it's body
				continue;
			}
			if (!isBody && line.contains(":")) {
				int colonIndex = line.indexOf(":");
				if (line.startsWith(":") && colonIndex != -1) {
					int secondColonIndex = line.indexOf(":", colonIndex + 1);
					if (secondColonIndex != -1) {
						colonIndex = secondColonIndex;
					}
				}
				// Highlight the header name (before the colon)
				stringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#efb92c")), start, start + colonIndex,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

				// Optionally, highlight pseudo-headers differently

			} else if (line.startsWith("HTTP")) {
				int pos = line.indexOf(" ");

				stringBuilder.setSpan(new ForegroundColorSpan(Color.parseColor("#2596be")), 0, pos,
						Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

			}
			// Move the start position to the next line
			start += line.length() + 1; // +1 for the newline character
		}

		return stringBuilder;
	}

	// Pre-defined colors as constants
	private static final int COLOR_HEADER_NAME = 0xFFEFB92C; // Yellow
	private static final int COLOR_HTTP_VERSION = 0xFFABB2BF; // Blue
	private static final int COLOR_BODY = 0xFFABB2BF; // Default text color

	public static SpannableStringBuilder highlightAndFormatHttpRequestEfficiently(String requestEditText) {
		SpannableStringBuilder spannableText = new SpannableStringBuilder();
		if (requestEditText.isEmpty())
			return spannableText;

		boolean isBody = false;
		int currentPosition = 0;
		int lineStart = 0;
		int textLength = requestEditText.length();

		// Manual line parsing is faster than Scanner
		while (currentPosition < textLength) {
			// Find end of line
			int lineEnd = currentPosition;
			while (lineEnd < textLength && requestEditText.charAt(lineEnd) != '\n') {
				lineEnd++;
			}

			// Extract line (without newline)
			String line = requestEditText.substring(currentPosition, lineEnd);
			lineStart = spannableText.length();

			// Handle empty line (headers/body separator)
			if (line.trim().isEmpty()) {
				spannableText.append("\n");
				isBody = true;
				currentPosition = lineEnd + 1;
				continue;
			}

			if (!isBody) {
				if (line.startsWith("HTTP")) {
					// HTTP version line
					spannableText.append(line).append("\n");
					int versionEnd = line.indexOf(' ');
					if (versionEnd == -1)
						versionEnd = line.length();
					spannableText.setSpan(new ForegroundColorSpan(COLOR_HTTP_VERSION), lineStart,
							lineStart + versionEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				} else if (line.contains(":")) {
					// Header line
					int colonIndex = line.indexOf(':');

					// Handle HTTP/2 pseudo-headers
					if (line.charAt(0) == ':' && colonIndex != -1) {
						int secondColon = line.indexOf(':', colonIndex + 1);
						if (secondColon != -1)
							colonIndex = secondColon;
					}

					spannableText.append(line).append("\n");
					spannableText.setSpan(new ForegroundColorSpan(COLOR_HEADER_NAME), lineStart, lineStart + colonIndex,
							Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				} else {
					// Other request lines (method, path, etc.)
					spannableText.append(line).append("\n");
				}
			} else {
				// Body content
				spannableText.append(line).append("\n");
			}

			currentPosition = lineEnd + 1; // Move past newline
		}

		// Apply body color to everything after first empty line
		if (isBody) {
			spannableText.setSpan(new ForegroundColorSpan(COLOR_BODY), lineStart, spannableText.length(),
					Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		}

		return spannableText;
	}

	public static String getBody(Body body) {
		
			String text = "";
			
			BodyType bodyType = body.type();

			Charset charset = CharsetUtil.UTF_8;

			if (bodyType.isText()) {

				try (InputStream input = body.getDecodedInputStream();
						Reader reader = new InputStreamReader(input, charset)) {
					text = Body.readAll(reader);
				} catch (IOException | IllegalStateException e) {

				}
			}
			if (text.equals("")) {
				text = body.content().toString(StandardCharsets.UTF_8);
			}
			for (Beautifier beautifier : beautifiers) {
				if (beautifier.accept(bodyType)) {

					text = beautifier.beautify(text, CharsetUtil.UTF_8);
					break;
				}
			}
	
		return text;

	}

	public static String beautify(String text, BodyType bodyType) {
		String newText = "";
		for (Beautifier beautifier : beautifiers) {
			if (beautifier.accept(bodyType)) {

				newText = beautifier.beautify(text, CharsetUtil.UTF_8);
				break;
			}
		}
		return newText;
	}

	public static InputStream getDecodedInputStream(InputStream rawInput, String contentEncoding) {
		if (rawInput == null || contentEncoding == null || contentEncoding.isEmpty()) {
			return rawInput; // No encoding specified or no content
		}

		InputStream input = rawInput;
		String ce = contentEncoding.toLowerCase();

		try {
			switch (ce) {
			case "identity":
			case "":
				break;
			case "gzip":
				input = new GZIPInputStream(input);
				break;
			case "deflate":
				input = new InflaterInputStream(input, new Inflater(true)); // Handle deflate
				break;
			case "br":
				// Brotli decoding (requires Brotli library)
				input = new BrotliInputStream(input);
				break;
			case "lzma":
				// LZMA decoding (requires xz or SevenZipJBinding library)
				input = new LZMAInputStream(input, -1);
				break;
			default:

				System.err.println("Unsupported content-encoding: " + contentEncoding);
				break;
			}
		} catch (Exception e) {
			System.err.println("Failed to decode stream. Content-Encoding: " + contentEncoding);
			e.printStackTrace();
			// Return the original input stream if decoding fails
			return rawInput;
		}

		return input;
	}

}