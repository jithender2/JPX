package com.proxy.utils;

import java.nio.ByteBuffer;
import java.util.List;
import java.io.InputStream;

/**
 * `BufferListInputStream` provides an InputStream interface over a list of
 * ByteBuffers.  This allows reading data sequentially from multiple buffers
 * as if they were a single contiguous stream.  This is useful when dealing with
 * data that has been received or stored in chunks.
 */
public class BufferListInputStream extends InputStream {
	private final List<ByteBuffer> bufferList;
	private int index;

	public BufferListInputStream(List<ByteBuffer> bufferList) {
		this.bufferList = bufferList;
	}

	/**
	 * Reads a single byte from the input stream.
	 *
	 * @return The byte read, or -1 if the end of the stream has been reached.
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public synchronized int read() {
		ByteBuffer buffer = indexBuffer();
		if (buffer == null) {
			return -1;
		}
		return buffer.get() & 0xff;
	}

	/**
	 * Reads up to `len` bytes of data from the input stream into an array of bytes.
	 *
	 * @param b   The buffer into which the data is read.
	 * @param off The start offset in the destination array `b`.
	 * @param len The maximum number of bytes to read.
	 * @return The total number of bytes read into the buffer, or -1 if there is no
	 *         more data because the end of the stream has been reached.
	 * @throws NullPointerException      If `b` is null.
	 * @throws IndexOutOfBoundsException If `off` is negative, `len` is negative,
	 *                                  or `len` is greater than `b.length - off`.
	 * @throws IOException              If an I/O error occurs.
	 */

	@Override
	public synchronized int read(byte[] b, int off, int len) {
		if (b == null) {
			throw new NullPointerException();
		} else if (off < 0 || len < 0 || len > b.length - off) {
			throw new IndexOutOfBoundsException();
		} else if (len == 0) {
			return 0;
		}

		ByteBuffer buffer = indexBuffer();
		if (buffer == null) {
			return -1;
		}
		int toRead = Math.min(len, buffer.remaining());
		buffer.get(b, off, toRead);
		return toRead;
	}

	/**
	 * Skips over and discards `n` bytes of data from this input stream.
	 *
	 * @param n the number of bytes to be skipped.
	 * @return the actual number of bytes skipped.
	 * @throws IOException If an I/O error occurs.
	 */
	@Override
	public synchronized long skip(long n) {
		if (n <= 0) {
			return 0;
		}
		ByteBuffer buffer = indexBuffer();
		if (buffer == null) {
			return 0;
		}
		int toSkip = (int) Math.min(n, buffer.remaining());
		buffer.position(buffer.position() + toSkip);
		return toSkip;
	}

	/**
	 * Gets the current ByteBuffer.  If the current buffer is empty, it moves
	 * to the next buffer in the list.
	 *
	 * @return The current ByteBuffer, or null if the end of the stream has been
	 *         reached.
	 */
	private ByteBuffer indexBuffer() {
		if (index >= bufferList.size()) {
			return null;
		}
		ByteBuffer buffer = bufferList.get(index);
		if (buffer.remaining() > 0) {
			return buffer;
		}
		index++;
		if (index >= bufferList.size()) {
			return null;
		}
		return bufferList.get(index);
	}

	@Override
	public synchronized void close() {
		bufferList.clear();
	}
}