package com.proxy.exception;

public class HttpDecodeException extends RuntimeException {
	public HttpDecodeException() {
	}
	
	public HttpDecodeException(String message) {
		super(message);
	}
	
	public HttpDecodeException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public HttpDecodeException(Throwable cause) {
		super(cause);
	}
}