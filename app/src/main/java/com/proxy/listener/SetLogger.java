package com.proxy.listener;

public class SetLogger {
	public static LogCallBack logCallBack;

	public interface LogCallBack {
		public void onLog(String str);
	}

	public static void setLogCallBack(LogCallBack logCallBack1) {
		logCallBack = logCallBack1;
	}

	public static void log(String str) {
		if (logCallBack != null) {

			new Thread(() -> {
				try {
					//	SetLogger.log("messaege was sent");
					logCallBack.onLog(str);
				} catch (Exception e) {
					//SetLogger.log("Exception in logCallBack: " + e.getMessage());
					e.printStackTrace();
				}
			}).start();
		}
	}
}