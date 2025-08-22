package com.proxy.netty.codec.web;

import java.io.ByteArrayOutputStream;
import java.util.concurrent.Executors;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ResourceLoader {
	private static ResourceLoader instance = new ResourceLoader();

	private ExecutorService resourceLoaderExecutor = Executors.newSingleThreadExecutor(r -> {
		Thread thread = new Thread(r);
		thread.setDaemon(true);
		thread.setName("Resource-Loader");
		return thread;
	});

	public static ResourceLoader getInstance() {
		return instance;
	}

	public CompletableFuture<byte[]> loadResource(InputStream input) {
		return CompletableFuture.supplyAsync(() -> {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			byte[] data = new byte[1024];
			int bytesRead;

			try {
				while ((bytesRead = input.read(data, 0, data.length)) != -1) {
					buffer.write(data, 0, bytesRead);
				}
			} catch (IOException e) {
				throw new UncheckedIOException(e);
			} finally {
				try {
					input.close();
				} catch (IOException e) {
					// Log or handle the close exception if necessary
				}
			}

			return buffer.toByteArray();
		}, resourceLoaderExecutor);
	}

	public CompletableFuture<byte[]> loadClassPathResource(String path) {
		return loadResource(this.getClass().getResourceAsStream(path));
	}
}