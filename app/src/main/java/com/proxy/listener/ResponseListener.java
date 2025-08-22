package com.proxy.listener;
import com.proxy.data.HttpMessage;

public interface ResponseListener {
    void onResponseComplete(HttpMessage message);
}