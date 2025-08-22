package com.proxy.listener;

import com.proxy.data.Http2Message;
import com.proxy.netty.codec.frame.Http2DataEvent;
import com.proxy.netty.codec.frame.Http2PriorityHeadersEvent;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;

import com.proxy.store.Body;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http2.Http2Headers;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

public class Http2InterceptorListener {
	private static Http2InterceptorListener instance;
	private boolean interceptionOn = false;
	//	private volatile boolean latchAwaiting = false;
	// Map to store the pending request data for each stream
	private ConcurrentHashMap<Integer, InterceptedRequest> interceptedRequests = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();

	public static synchronized Http2InterceptorListener getInstance() {
		if (instance == null) {
			instance = new Http2InterceptorListener();
		}
		return instance;
	}

	public synchronized void toggleInterception() {
		interceptionOn = !interceptionOn;
	}

	public boolean isInterceptionOn() {
		return interceptionOn;
	}

	public void storePendingRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, int streamId) {
		interceptedRequests.put(streamId, new InterceptedRequest(ctx, msg, promise));

		HttpMessageListener.log(msg, streamId);

		CountDownLatch latch = new CountDownLatch(1);
		countDownLatchMap.put(streamId, latch);

		CountDownLatch checkLatch = countDownLatchMap.get(streamId);

		try {

			latch.await();

			forward(streamId);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();

		}
	}

	public void forward(int streamId) {
		InterceptedRequest interceptedRequest = interceptedRequests.get(streamId); // Do not remove yet

		if (interceptedRequest != null) {
			ChannelHandlerContext ctx = interceptedRequest.getCtx();
			Object msg = interceptedRequest.getMsg();
			ChannelPromise promise = interceptedRequest.getPromise();

			ctx.write(msg, promise).addListener(future -> {
				if (!future.isSuccess()) {
				}
				if (msg instanceof IHttp2HeadersEvent) {
					interceptedRequest.setHeadersSent(true);
				}
				// Remove request only if headers and data are sent
				if (interceptedRequest.isHeadersSent() && msg instanceof Http2DataEvent) {
					interceptedRequests.remove(streamId);
				}

			});
		}
	}

	public void forwardOnClick(int streamId, OnHeadersForwarded onHeadersForwarded) {
		CountDownLatch latch = countDownLatchMap.get(streamId);
		if (latch != null) {

			onHeadersForwarded.onForwarded();
			latch.countDown();
			countDownLatchMap.remove(streamId);

		} else {

		}
	}

	public void forwardOnClick(int streamId) {
		CountDownLatch latch = countDownLatchMap.get(streamId);
		if (latch != null) {
			//SetLogger.log("latch released" + streamId);
			latch.countDown();
			countDownLatchMap.remove(streamId);

		} else {

		}
	}

	public void forwardOnClick(int streamId, Http2Headers modifiedHeaders, OnHeadersForwarded onHeadersForwarded) {
		InterceptedRequest interceptedRequest = interceptedRequests.get(streamId);
		if (interceptedRequest != null) {
			if (interceptedRequest.getMsg() instanceof IHttp2HeadersEvent) {

				IHttp2HeadersEvent headersEvent = (IHttp2HeadersEvent) interceptedRequest.getMsg();
				IHttp2HeadersEvent newHeadersEvent = new Http2PriorityHeadersEvent(streamId, modifiedHeaders,
						headersEvent.padding(), headersEvent.endOfStream(),
						((Http2PriorityHeadersEvent) headersEvent).streamDependency(),
						((Http2PriorityHeadersEvent) headersEvent).weight(),
						((Http2PriorityHeadersEvent) headersEvent).exclusive());
				interceptedRequest.updateMsg(newHeadersEvent);
			} else if (interceptedRequest.getMsg() instanceof Http2Message) {
				Http2Message message = (Http2Message) interceptedRequest.getMsg();

				IHttp2HeadersEvent newHeadersEvent = new Http2PriorityHeadersEvent(streamId, modifiedHeaders, // New headers
						0, // Default padding
						false, // Default endOfStream
						0, // Default streamDependency
						(short) 0, // Default weight
						false // Default exclusive
				);

				interceptedRequest.updateMsg(newHeadersEvent);
			}

		} else {
			//	SetLogger.log("Intercepted request is null");
		}
		//	SetLogger.log("forward on click");
		forwardOnClick(streamId, onHeadersForwarded);
	}

	public void forwardOnClick(int streamId, String modifiedHeadersText, OnHeadersForwarded onHeadersForwarded) {
		// Split the modified headers into individual lines
		String[] headerLines = modifiedHeadersText.split("\n");
		Http2Headers modifiedHeaders = new DefaultHttp2Headers();

		String scheme = null;
		String method = null;
		String path = null;
		String authority = null;

		for (String line : headerLines) {
			line = line.trim(); // Remove leading/trailing whitespaces

			// Skip empty or malformed lines
			if (line.isEmpty() || !line.contains(":")) {
				continue;
			}

			// Find the index of the first colon after the first character
			int idx = line.substring(1).indexOf(":") + 1;
			if (idx <= 0) {
				continue; // Invalid header line, skip it
			}

			String name = line.substring(0, idx).trim();
			String value = line.substring(idx + 1).trim();

			if (!name.isEmpty() && !value.isEmpty()) {
				modifiedHeaders.add(name.toLowerCase(), value);

				// Handle pseudo-headers
				switch (name) {
				case ":scheme":
					scheme = value;
					break;
				case ":method":
					method = value;
					break;
				case ":path":
					path = value;
					break;
				case ":authority":
					authority = value;
					break;
				}
			}
		}

		// Ensure pseudo-headers are set (optional defaults can be applied)
		if (scheme != null)
			modifiedHeaders.scheme(scheme);
		if (method != null)
			modifiedHeaders.method(method);
		if (path != null)
			modifiedHeaders.path(path);
		if (authority != null)
			modifiedHeaders.authority(authority);

		// Call the existing method to forward the request
		forwardOnClick(streamId, modifiedHeaders, onHeadersForwarded);
	}

	/*	public void forwardData(Http2DataEvent dataEvent, int streamId) {
	InterceptedRequest request = interceptedRequests.get(streamId);
	if (request != null) {
		request.updateMsg(dataEvent);
		SetLogger.log("updated");
		forwardOnClick(streamId);
		} else {
		SetLogger.log(" request not found");
	}
	}*/
	
	public void forwardData(Http2DataEvent dataEvent, int streamId) {
		InterceptedRequest request = interceptedRequests.get(streamId);
		if (request != null) {
			request.updateMsg(dataEvent);

			// Create a new latch for the data frame
			CountDownLatch newLatch = new CountDownLatch(1);
			countDownLatchMap.put(streamId, newLatch);

			//	SetLogger.log("updated");
			forwardOnClick(streamId);
		} else {
			//SetLogger.log(" request not found");
		}
	}

	
	private static class InterceptedRequest {
		private final ChannelHandlerContext ctx;
		private Object msg;
		private final ChannelPromise promise;
		private boolean headersSent = false;

		public InterceptedRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
			this.ctx = ctx;
			this.msg = msg;
			this.promise = promise;

		}

		public ChannelHandlerContext getCtx() {
			return ctx;
		}

		public Object getMsg() {
			return msg;
		}

		public ChannelPromise getPromise() {
			return promise;
		}

		public void updateMsg(Object newMsg) {
			this.msg = newMsg;
		}

		public boolean isHeadersSent() {
			return headersSent;
		}

		public void setHeadersSent(boolean headersSent) {
			this.headersSent = headersSent;
		}
	}

}

/*
public class Http2InterceptorListener {
private static Http2InterceptorListener instance;
private boolean interceptionOn = false;
//	private volatile boolean latchAwaiting = false;
// Map to store the pending request data for each stream
private ConcurrentHashMap<Integer, InterceptedRequest> interceptedRequests = new ConcurrentHashMap<>();
private final ConcurrentHashMap<Integer, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();
Body body = null;

public static synchronized Http2InterceptorListener getInstance() {
	if (instance == null) {
		instance = new Http2InterceptorListener();
	}
	return instance;
}

public synchronized void toggleInterception() {
	interceptionOn = !interceptionOn;
}

public boolean isInterceptionOn() {
	return interceptionOn;
}

// Store pending request for later forwarding
public void storePendingRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise, int streamId) {
	if (msg instanceof Http2DataEvent) {
		ctx.write(msg, promise);
		return;
	}
	interceptedRequests.put(streamId, new InterceptedRequest(ctx, msg, promise));
	
	HttpMessageListener.log(msg, streamId);
	
	CountDownLatch latch = new CountDownLatch(1);
	countDownLatchMap.put(streamId, latch);
	
	CountDownLatch checkLatch = countDownLatchMap.get(streamId);
	
	try {
		//SetLogger.log("Awaiting latch for Stream ID: " + streamId);
		latch.await();
		
		forward(streamId);
		} catch (InterruptedException e) {
		Thread.currentThread().interrupt();
		//	SetLogger.log("Latch await interrupted for Stream ID: " + streamId);
	}
}


public void forward(int streamId) {
	InterceptedRequest interceptedRequest = interceptedRequests.remove(streamId);
	if (interceptedRequest != null) {
		
		ChannelHandlerContext ctx = interceptedRequest.getCtx();
		Object msg = interceptedRequest.getMsg();
		ChannelPromise promise = interceptedRequest.getPromise();
		ctx.write(msg, promise).addListener(future -> {
			if (!future.isSuccess()) {
				//Todo throw a error to show the request was failed
			}
			
		});
		} else {
		
	}
}


public void forwardOnClick(int streamId) {
	CountDownLatch latch = countDownLatchMap.get(streamId);
	if (latch != null) {
		latch.countDown();
		countDownLatchMap.remove(streamId);
		
	}
	
}



public void forwardOnClick(int streamId, Http2Headers modifiedHeaders) {
	InterceptedRequest interceptedRequest = interceptedRequests.get(streamId);
	
	if (interceptedRequest != null) {
		
		IHttp2HeadersEvent headersEvent = (IHttp2HeadersEvent) interceptedRequest.getMsg();
		IHttp2HeadersEvent newHeadersEvent = new Http2PriorityHeadersEvent(streamId, modifiedHeaders,
		headersEvent.padding(), headersEvent.endOfStream(),
		((Http2PriorityHeadersEvent) headersEvent).streamDependency(),
		((Http2PriorityHeadersEvent) headersEvent).weight(),
		((Http2PriorityHeadersEvent) headersEvent).exclusive());
		interceptedRequest.updateMsg(newHeadersEvent);
		
	}
	
	forwardOnClick(streamId);
}

private void ConvertToHttp1Headers() {
	
}


public void forwardOnClick(int streamId, String modifiedHeadersText) {
	// Split the modified headers into individual lines
	String[] headerLines = modifiedHeadersText.split("\n");
	Http2Headers modifiedHeaders = new DefaultHttp2Headers();
	
	String scheme = null;
	String method = null;
	String path = null;
	String authority = null;
	
	for (String line : headerLines) {
		line = line.trim(); // Remove leading/trailing whitespaces
		
		// Skip empty or malformed lines
		if (line.isEmpty() || !line.contains(":")) {
			continue;
		}
		
		// Find the index of the first colon after the first character
		int idx = line.substring(1).indexOf(":") + 1;
		if (idx <= 0) {
			continue; // Invalid header line, skip it
		}
		
		String name = line.substring(0, idx).trim();
		String value = line.substring(idx + 1).trim();
		
		if (!name.isEmpty() && !value.isEmpty()) {
			modifiedHeaders.add(name, value);
			
			// Handle pseudo-headers
			switch (name) {
				case ":scheme":
				scheme = value;
				break;
				case ":method":
				method = value;
				break;
				case ":path":
				path = value;
				break;
				case ":authority":
				authority = value;
				break;
			}
		}
	}
	
	// Ensure pseudo-headers are set (optional defaults can be applied)
	if (scheme != null)
	modifiedHeaders.scheme(scheme);
	if (method != null)
	modifiedHeaders.method(method);
	if (path != null)
	modifiedHeaders.path(path);
	if (authority != null)
	modifiedHeaders.authority(authority);
	
	// Call the existing method to forward the request
	forwardOnClick(streamId, modifiedHeaders);
}

// Nested class to store intercepted request data
private static class InterceptedRequest {
	private final ChannelHandlerContext ctx;
	private Object msg;
	private final ChannelPromise promise;
	
	public InterceptedRequest(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
		this.ctx = ctx;
		this.msg = msg;
		this.promise = promise;
		
	}
	
	public ChannelHandlerContext getCtx() {
		return ctx;
	}
	
	public Object getMsg() {
		return msg;
	}
	
	public ChannelPromise getPromise() {
		return promise;
	}
	
	public void updateMsg(Object newMsg) {
		this.msg = newMsg;
	}
}

}*/