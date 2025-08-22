package com.proxy.netty.codec.web;

import io.netty.handler.codec.http.HttpResponseStatus;

import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * `WebResponse` represents an HTTP web response, storing the status code,
 * content type, and response data.  Provides convenient factory methods for
 * creating common responses (e.g., 404 Not Found, HTML, text, error).
 */
class WebResponse {
	private HttpResponseStatus status;
	private String contentType;
	private byte[] data;

	public WebResponse(HttpResponseStatus status, String contentType, byte[] data) {
		this.status = status;
		this.contentType = contentType;
		this.data = data;
	}

	public static WebResponse notFound(String uri) {
		return new WebResponse(NOT_FOUND, "text/plain; charset=utf-8", (uri + " not found").getBytes());
	}

	public static WebResponse html(byte[] data) {
		return new WebResponse(OK, "text/html; charset=utf-8", data);
	}

	public static WebResponse html(String html) {
		return html(html.getBytes(UTF_8));
	}

	public static WebResponse text(String text) {
		return new WebResponse(OK, "text/plain; charset=utf-8", text.getBytes(UTF_8));
	}

	public static WebResponse fromThrowable(Throwable e) {
		return new WebResponse(INTERNAL_SERVER_ERROR, "text/plain; charset=utf-8", e.getMessage().getBytes(UTF_8));
	}

	public HttpResponseStatus getStatus() {
		return status;
	}

	public void setStatus(HttpResponseStatus status) {
		this.status = status;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

}