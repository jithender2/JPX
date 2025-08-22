package com.proxy.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.channels.FileChannel;
import java.io.RandomAccessFile;
import java.io.File;
import java.nio.ByteBuffer;

/**
 * `MMappedStore` is a `DataStore` implementation that uses memory mapping to
 * store and access data.  This is typically more efficient for larger datasets
 * as it allows the operating system to handle paging data in and out of memory
 * as needed.
 */
class MMappedStore extends DataStore {

	public MMappedStore(int size) {
		super(createBuffer(size));
	}

	private static ByteBuffer createBuffer(int size) {
		try {
			File file = File.createTempFile("cute_proxy_", ".tmp");
			RandomAccessFile raf = new RandomAccessFile(file, "rw");

			if (!file.delete()) {
				// fall back to deletion on exit on windows
				file.deleteOnExit();
			}
			return raf.getChannel().map(FileChannel.MapMode.READ_WRITE, 0, size);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}
}