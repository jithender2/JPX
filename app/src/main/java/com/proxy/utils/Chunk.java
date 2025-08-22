package com.proxy.utils;

import java.nio.ByteBuffer;

/**
 * `Chunk` represents a contiguous segment of data within a `DataStore`.  It provides
 * a convenient way to access and manage portions of data without needing to
 * handle the underlying storage details directly.
 */
public class Chunk {
	private DataStore store;
	private int offset;
	private int size;

	public Chunk(DataStore store, int offset, int size) {
		this.store = store;
		this.offset = offset;
		this.size = size;
	}

	public ByteBuffer read() {
		return store.read(offset, size);
	}

	public DataStore getStore() {
		return store;
	}

	public int getOffset() {
		return offset;
	}

	public int getSize() {
		return size;
	}
}