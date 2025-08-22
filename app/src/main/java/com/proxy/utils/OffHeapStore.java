package com.proxy.utils;

import java.nio.ByteBuffer;

/**
 * `OffHeapStore` is a `DataStore` implementation that stores data in off-heap
 * memory using `ByteBuffer.allocateDirect()`.  Off-heap memory is not managed
 * by the Java garbage collector, which can improve performance for certain
 * types of applications by reducing garbage collection overhead.  However,
 * it's important to manage off-heap memory carefully to avoid memory leaks.
 */
class OffHeapStore extends DataStore {

	public OffHeapStore(int size) {
		super(createBuffer(size));
	}

	private static ByteBuffer createBuffer(int size) {
		return ByteBuffer.allocateDirect(size);
	}
}