package com.proxy.listener;
import java.net.URI;
import io.netty.handler.codec.http.HttpRequest;
import okhttp3.Request;

public interface RedirectHandler {
		void onRedirectDetected(Request originalRequest, URI redirectUri, int code);
	}
