package com.proxy.ui;

package com.example.yourapp;

public class FuzzingResult {
    private String payload;
    private int statusCode;
    private long responseTime;
    private long responseSize;
    private String responseBody;
    private String requestHeaders;
    private String responseHeaders;

    public FuzzingResult(String payload, int statusCode, long responseTime, long responseSize) {
        this.payload = payload;
        this.statusCode = statusCode;
        this.responseTime = responseTime;
        this.responseSize = responseSize;
    }

    public FuzzingResult(String payload, int statusCode, long responseTime, long responseSize, 
                         String responseBody, String requestHeaders, String responseHeaders) {
        this.payload = payload;
        this.statusCode = statusCode;
        this.responseTime = responseTime;
        this.responseSize = responseSize;
        this.responseBody = responseBody;
        this.requestHeaders = requestHeaders;
        this.responseHeaders = responseHeaders;
    }

    public String getPayload() {
        return payload;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getResponseTime() {
        return responseTime;
    }

    public long getResponseSize() {
        return responseSize;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getRequestHeaders() {
        return requestHeaders;
    }

    public String getResponseHeaders() {
        return responseHeaders;
    }
}