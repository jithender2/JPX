package com.proxy.listener;

package com.proxy.listener;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.concurrent.Future;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class HttpInterceptorListener {
	private static HttpInterceptorListener instance;
	private final AtomicInteger streamId = new AtomicInteger(0);
	private final ConcurrentHashMap<Integer, CountDownLatch> countDownLatchMap = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<Integer, InterceptedRequest> interceptedRequests = new ConcurrentHashMap<>();
	private boolean interceptionOn = false;

	private HttpInterceptorListener() {
	}

	/**
	 * Gets the singleton instance of `HttpInterceptorListener`.
	 *
	 * @return The singleton instance.
	 */
	public static synchronized HttpInterceptorListener getInstance() {
		if (instance == null) {
			instance = new HttpInterceptorListener();
		}
		return instance;
	}

	/**
	 * Generates the next available stream ID.  This should be synchronized if
	 * there's a chance of concurrent calls to avoid duplicate stream IDs.
	 * @return The next available stream ID.
	 */
	public int generateStreamId() {
		return streamId.getAndIncrement();
	}

	/**
	 * Toggles HTTP/1 interception on or off.
	 */
	public synchronized void toggleInterception() {
		interceptionOn = !interceptionOn;
	}

	/**
	 * Checks if HTTP/1 interception is currently enabled.
	 *
	 * @return `true` if interception is on, `false` otherwise.
	 */
	public boolean isInterceptionOn() {
		return interceptionOn;
	}

	/**
	 * Stores a pending HTTP/1 request, pausing it until explicitly forwarded.
	 *
	 * @param ctx     The `ChannelHandlerContext`.
	 * @param request The intercepted request.
	 * @param promise The `ChannelPromise`.
	 */
	public synchronized void storePendingRequest(ChannelHandlerContext ctx, Object request, ChannelPromise promise) {
		int streamId = generateStreamId();
		InterceptedRequest interceptedRequest = new InterceptedRequest(ctx, request, promise);

		interceptedRequests.put(streamId, interceptedRequest);
		HttpMessageListener.log(request, streamId);

		CountDownLatch latch = new CountDownLatch(1);
		countDownLatchMap.put(streamId, latch);

		try {

			latch.await();

			forward(streamId);
		} catch (InterruptedException e) {

			Thread.currentThread().interrupt(); // Restore the interrupted state
		}
	}

	/**
	 * Forwards a pending request.
	 *
	 * @param streamId The ID of the stream to forward.
	 */
	public void forward(int streamId) {
		InterceptedRequest request = interceptedRequests.remove(streamId);

		if (request != null) {

			request.getCtx().write(request.getMsg(), request.getPromise()).addListener(future -> {
				if (!future.isSuccess()) {
					//Todo throw a error to show the request was failed
				}
			});
		} else {

		}
	}

	/**
	 * Updates a pending request and then forwards it.
	 *
	 * @param streamId      The ID of the stream to update and forward.
	 * @param updatedRequest The updated request object.
	 */
	public void forwardOnClick(int streamId, Object updatedRequest) {
		InterceptedRequest interceptedRequest = interceptedRequests.get(streamId);
		if (interceptedRequest != null) {
			interceptedRequest.updateMsg(updatedRequest);

		}
		forwardOnClick(streamId);
	}

	/**
	 * Unblocks and forwards a pending request.  This is the trigger to release
	 * the waiting thread in `storePendingRequest`.
	 *
	 * @param streamId The ID of the stream to forward.
	 */
	public void forwardOnClick(int streamId) {
		CountDownLatch latch = countDownLatchMap.remove(streamId);
		if (latch != null) {
			latch.countDown();

		}
	}

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

		public void updateMsg(Object msg) {
			this.msg = msg;
		}
	}
}