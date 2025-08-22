package com.proxy.data;
import java.util.List;

public interface HttpResponseHeaders {

    /**
     * Http status code
     */
    int statusCode();

    /**
     * all commons headers(exclude request line)
     */
    List<Header> headers();
}