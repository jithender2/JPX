package com.proxy.data;

import com.proxy.store.Body;
import java.io.Serializable;

public abstract class HttpMessage extends Message implements Serializable {
	private static final long serialVersionUID = 422567306064093600L;

	protected HttpMessage(String host, String url) {
		super(host, url);
	}

	public abstract HttpHeaders requestHeader();

	public abstract Body requestBody();

	public abstract HttpHeaders responseHeader();

	public abstract Body responseBody();

}