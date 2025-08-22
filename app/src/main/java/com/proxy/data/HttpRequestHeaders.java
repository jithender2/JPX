package com.proxy.data;
import java.util.List;

public interface HttpRequestHeaders {

    /**
     * request method
     */
    String method();

    /**
     * Request path
     */
    String path();

    /**
     * Http version
     */
    String version();

    /**
     * all commons headers(exclude request line)
     */
    List<Header> headers();
}