package com.proxy.data;
import com.proxy.utils.NameValues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Http2 response headers
 */
public class Http2ResponseHeaders extends Http2Headers implements HttpResponseHeaders, Serializable {
    private static final long serialVersionUID = -7574758006808314305L;
    private final int status;

    public Http2ResponseHeaders(int status, List<Header> headers) {
        super(headers);
        this.status = status;
    }

    public int status() {
        return status;
    }

    @Override
    public List<String> rawLines() {
        List<Header> allHeaders = new ArrayList<>(1 + headers().size());
        allHeaders.addAll(headers());
        return NameValues.toAlignText(allHeaders, ": ");
    }

    @Override
    public List<NameValue> cookieValues() {
        return getHeaders("Set-Cookie").stream().map(CookieUtils::parseCookieHeader).collect(Collectors.toList());
    }

    @Override
    public int statusCode() {
        return Objects.requireNonNull(status);
    }
}