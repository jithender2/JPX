package com.proxy.listener;

package com.proxy.listener;

import com.proxy.data.HttpMessage;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;
import com.proxy.store.Body;

public class HttpMessageListener {

	public static LogCallBack logCallBack;

	public interface LogCallBack {
		public void onLog(Object httpMessage, int id);
	}

	public static void setLogCallBack(LogCallBack logCallBack1) {
		logCallBack = logCallBack1;
	}
	/*
		public static void log(Object str, int streamId) {
			if (logCallBack != null) {
	
				logCallBack.onLog(str, streamId);
			}
		}*/
	public static void log(Object str, int streamId) {
		if (logCallBack != null) {
			new Thread(() -> {
				try {
				//	SetLogger.log("messaege was sent");
					logCallBack.onLog(str, streamId);
				} catch (Exception e) {
					//SetLogger.log("Exception in logCallBack: " + e.getMessage());
					e.printStackTrace();
				}
			}).start();
		} else {
			//SetLogger.log("logcall back is null");
		}
	}

}