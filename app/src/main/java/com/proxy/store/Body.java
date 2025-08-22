package com.proxy.store;

import com.proxy.utils.BufferListInputStream;
import com.proxy.utils.DataStore;
import com.proxy.utils.DataStoreManager;

import com.proxy.data.ContentType;
import com.proxy.utils.Chunk;
import com.proxy.utils.BodyType;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import org.brotli.dec.BrotliInputStream;
import org.tukaani.xz.LZMAInputStream;

public class Body implements Serializable {
	private static final long serialVersionUID = -5119917294712380816L;

	private long size;

	private boolean finished;

	private BodyType type;
	private Optional<Charset> charset;
	private String contentEncoding;

	private transient boolean beautify;

	private List<Chunk> chunkList;

	public Body(BodyType type, Optional<Charset> charset, String contentEncoding) {
		this.type = requireNonNull(type);
		this.charset = requireNonNull(charset);
		this.contentEncoding = requireNonNull(contentEncoding);
		this.chunkList = new ArrayList<>();
	}

	public static Body create(ContentType contentType, String contentEncoding) {
		BodyType bodyType = getHttpBodyType(requireNonNull(contentType));
		return new Body(bodyType, contentType.charset(), requireNonNull(contentEncoding));
	}

	/**
	* Determines the appropriate `BodyType` based on the provided `ContentType`.
	* This function maps MIME subtypes to their corresponding `BodyType` enum
	* values.  It handles image, text, and other (binary) content types.
	*
	* @param contentType The `ContentType` object to analyze.
	* @return The corresponding `BodyType` enum value.
	*/

	public static BodyType getHttpBodyType(ContentType contentType) {
		BodyType bodyType;
		String subType = contentType.mimeType().getSubType();
		if (contentType.isImage()) {
			switch (subType) {
			case "png":
				bodyType = BodyType.png;
				break;
			case "jpeg":
				bodyType = BodyType.jpeg;
				break;
			case "gif":
				bodyType = BodyType.gif;
				break;
			case "bmp":
				bodyType = BodyType.bmp;
				break;
			case "x-icon":
			case "image/vnd.microsoft.icon":
				bodyType = BodyType.icon;
				break;
			default:
				bodyType = BodyType.otherImage;
				break;
			}
		} else if (contentType.isText()) {
			switch (subType) {
			case "json":
				bodyType = BodyType.json;
				break;
			case "html":
				bodyType = BodyType.html;
				break;
			case "xml":
				bodyType = BodyType.xml;
				break;
			case "x-www-form-urlencoded":
				bodyType = BodyType.www_form;
				break;
			case "css":
				bodyType = BodyType.css;
				break;
			case "javascript":
			case "x-javascript":
				bodyType = BodyType.javascript;
				break;
			default:
				bodyType = BodyType.text;
				break;
			}
		} else {
			bodyType = BodyType.binary;
		}
		return bodyType;
	}

	/**
	 * Append new data to this body
	 */
	public void append(ByteBuffer buffer) {
		int size = buffer.remaining();
		if (size == 0) {
			return;
		}
		DataStoreManager manager = DataStoreManager.getInstance();
		this.size += size;
		DataStore dataStore = manager.fetchStore(size);
		int offset = dataStore.write(buffer);
		Chunk chunk = new Chunk(dataStore, offset, size);
		chunkList.add(chunk);
	}

	public void finish() {
		this.finished = true;
	}

	public boolean finished() {
		return finished;
	}

	/**
	 * The len of data
	 */
	public long size() {
		return this.size;
	}

	public InputStream getInputStream() {
		if (!finished) {
			throw new IllegalStateException("Http body not finished yet");
		}
		List<ByteBuffer> bufferList = chunkList.stream().map(Chunk::read).collect(toList());
		return new BufferListInputStream(bufferList);
	}

	/**
	 * Get content as input stream, with content decompressed is needed
	 */
	public InputStream getDecodedInputStream() {
		InputStream input = getInputStream();
		if (size() == 0) {
			return input;
		}

		String ce = contentEncoding.toLowerCase();
		try {
			switch (ce) {
			case "":
			case "identity":
				// do nothing
				break;
			case "gzip":
				input = new GZIPInputStream(input);
				break;
			case "deflate":
				input = new InflaterInputStream(getInputStream(), new Inflater(true));
				break;
			case "br":
				input = new BrotliInputStream(input);
				break;

			case "lzma":
				input = new LZMAInputStream(input, -1);

				break;
			default:
				// logger.warn("unsupported content-encoding: {}", contentEncoding);
				break;
			}
		} catch (Throwable t) {
			//logger.error("Decode stream failed, encoding: {}", contentEncoding, t);
			return input;
		}
		return input;
	}

	/**
	 * If is text body, read to string
	 */
	public String getAsString() {
		if (!type.isText()) {
			throw new UnsupportedOperationException("not textual body");
		}
		try (InputStream input = getDecodedInputStream();
				InputStreamReader reader = new InputStreamReader(input, charset().orElse(UTF_8))) {
			return readAll(reader);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public ByteBuf content() {
		try {
			if (type.isText()) {
				// If the body is textual, use getAsString()
				String content = getAsString();
				return Unpooled.copiedBuffer(content, charset().orElse(Charset.defaultCharset()));
			} else {
				// For binary content, use getDecodedInputStream()
				InputStream inputStream = getDecodedInputStream();
				ByteBuf buffer = Unpooled.buffer();
				byte[] chunk = new byte[1024];
				int bytesRead;
				while ((bytesRead = inputStream.read(chunk)) != -1) {
					buffer.writeBytes(chunk, 0, bytesRead);
				}
				return buffer;
			}
		} catch (IOException e) {
			throw new RuntimeException("Failed to read body content", e);
		}
	}

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeBoolean(finished);
		out.writeObject(type);
		if (charset.isPresent()) {
			out.writeBoolean(true);
			out.writeUTF(charset.get().name());
		} else {
			out.writeBoolean(false);
		}

		out.writeUTF(contentEncoding);

		if (finished) {
			out.writeLong(size);
			try (InputStream in = getInputStream()) {
				copy(in, out);

			}
		}
	}

	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		finished = in.readBoolean();
		type = (BodyType) in.readObject();
		boolean charsetPresent = in.readBoolean();
		if (!charsetPresent) {
			charset = Optional.empty();
		} else {
			charset = Optional.of(Charset.forName(in.readUTF()));
		}
		contentEncoding = in.readUTF();

		if (finished) {
			size = in.readLong();
			chunkList = new ArrayList<>();
			loadFromInput(in, size);

		}
	}

	/**
	 * Loads data from the given `InputStream` into the buffer.  Reads data in
	 * chunks to avoid loading the entire content into memory at once.
	 *
	 * @param in   The `InputStream` to read from.
	 * @param size The total size of the data to read.  If size is negative, it will
	 *             read until the end of the stream.  It will also cap the chunk size
	 *             to Integer.MAX_VALUE to avoid potential overflow issues.
	 * @throws IOException If an I/O error occurs during the read operation.
	 */
	public void loadFromInput(InputStream in, long size) throws IOException {
		if (size < 0) {
			size = Integer.MAX_VALUE;
		}
		int chunkSize = (int) Math.min(4 * 1024, size);

		long offset = 0;
		byte[] buffer = new byte[chunkSize];
		int read;
		while (offset < size && (read = in.read(buffer, 0, (int) Math.min(size - offset, chunkSize))) != -1) {
			append(ByteBuffer.wrap(buffer, 0, read));
			offset += read;
		}
	}

	public BodyType type() {
		return type;
	}

	public Optional<Charset> charset() {
		return charset;
	}

	public String contentEncoding() {
		return contentEncoding;
	}

	public void charset(Charset charset) {
		this.charset = Optional.of(charset);
	}

	public void type(BodyType type) {
		this.type = type;
	}

	public boolean beautify() {
		return beautify;
	}

	public Body beautify(boolean beautify) {
		this.beautify = beautify;
		return this;
	}

	@Override
	public String toString() {
		return "Body{size=" + size() + "}";
	}

	/**
	 * Copies all data from an `InputStream` to an `OutputStream`.  Uses a
	 * buffer of 1024 bytes for efficient copying.  Does not close the streams;
	 * it's the caller's responsibility to close them.
	 *
	 * @param inputStream  The `InputStream` to read from.
	 * @param outputStream The `OutputStream` to write to.
	 * @throws IOException If an I/O error occurs during the copy operation.
	 * @throws NullPointerException If either `inputStream` or `outputStream` is null.
	 */
	public static void copy(InputStream inputStream, OutputStream outputStream) throws IOException {
		byte[] buffer = new byte[1024]; // Buffer for reading bytes in chunks
		int bytesRead;

		// Read bytes in chunks and write to the OutputStream
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead); // Write the read bytes to the output stream
		}
	}

	public static String readAll(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[1024]; // Buffer for reading characters in chunks
		int read;

		// Read characters in chunks and append to StringBuilder
		while ((read = reader.read(buffer)) != -1) {
			sb.append(buffer, 0, read);
		}

		return sb.toString(); // Return the concatenated string

	}

}