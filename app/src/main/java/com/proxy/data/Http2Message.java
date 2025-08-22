package com.proxy.data;

import com.proxy.listener.ResponseListener;
import com.proxy.store.Body;
import com.proxy.utils.HostPort;
import io.netty.channel.ChannelId;
import java.io.Serializable;
import java.util.Objects;
import static java.util.Objects.requireNonNull;
import java.util.Optional;

public class Http2Message extends HttpMessage implements Serializable {
	private static final long serialVersionUID = 2578040550199755205L;
	private final Http2RequestHeaders requestHeader;
	private final Body requestBody;

	private volatile Http2ResponseHeaders responseHeader;
	private volatile Body responseBody;
	HostPort hostPort;
	int StreamId;

	

	

	public Http2Message(HostPort address, Http2RequestHeaders requestHeader, Body body, int StreamId) {
		super(requestHeader.getHeader("Host").orElse(address.host()), getUrl(address, requestHeader));
		this.requestHeader = Objects.requireNonNull(requestHeader);
		this.requestBody = body;
		hostPort = address;
		this.StreamId = StreamId;

	}

	private static String getUrl(HostPort address, Http2RequestHeaders requestHeader) {
		String scheme = requestHeader.scheme();
		int port = address.ensurePort();
		StringBuilder sb = new StringBuilder(scheme).append("://");
		sb.append(requestHeader.getHeader("Host").orElse(address.host()));
		if (scheme.equals("https") && port != 443 || scheme.equals("http") && port != 80) {
			sb.append(":").append(port);
		}
		sb.append(requestHeader.path());
		return sb.toString();
	}

	@Override
	public String displayText() {
		return url();
	}

	public int getStreamId() {
		return this.StreamId;
	}

	public Http2RequestHeaders requestHeader() {
		return requestHeader;
	}

	public Body requestBody() {
		return requestBody;
	}

	public HostPort getHostPort() {
		return hostPort;
	}

	public String getPath() {
		return requireNonNull(requestHeader().path());
	}

	public String MimeType() {
		Optional<ContentType> contentType = responseHeader.contentType();
		if (contentType.isPresent()) {
			return contentType.get().mimeType().getSubType();
		}
		return "-";
	}

	public int code() {
		return responseHeader().statusCode();
	}

	public long length() {
		if (responseHeader == null)
			return 0;
		return responseBody.size();
	}

	public String getMethod() {
		return requestHeader.method();
	}

	public Http2ResponseHeaders responseHeader() {
		return responseHeader;
	}

	public void setResponseHeader(Http2ResponseHeaders responseHeader) {
		this.responseHeader = Objects.requireNonNull(responseHeader);
		
	}

	public Body responseBody() {
		return responseBody;
	}

	public void setResponseBody(Body responseBody) {
		this.responseBody = responseBody;
	}

	@Override
	public String toString() {
		return "Http2Message{url=" + url() + "}";
	} 
	
	
        
    

}