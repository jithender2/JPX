package com.proxy.ui.Fragments;

import android.graphics.Color;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextUtils;
import android.util.Log;
import android.view.ViewGroup;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;
import com.proxy.R;
import android.view.LayoutInflater;
import androidx.fragment.app.Fragment;
import com.proxy.beautifier.Beautifier;
import com.proxy.data.Http1Message;
import com.proxy.data.Http1RequestHeaders;
import com.proxy.data.Http2Message;
import com.proxy.data.Header;
import com.proxy.data.Http2RequestHeaders;
import com.proxy.data.HttpHeaders;
import com.proxy.data.RequestLine;

import com.proxy.databinding.InterceptorBinding;
import com.proxy.listener.HttpInterceptorListener;
import com.proxy.listener.HttpMessageListener;
import com.proxy.listener.OnHeadersForwarded;
import com.proxy.netty.codec.frame.Http2DataEvent;
import com.proxy.netty.codec.frame.IHttp2HeadersEvent;
import com.proxy.store.Body;
import com.proxy.listener.Http2InterceptorListener;

import com.proxy.listener.SetLogger;

import com.proxy.utils.BodyType;
import com.proxy.utils.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import java.io.InputStreamReader;
import io.netty.handler.codec.http.HttpVersion;
import java.io.IOException;
import java.nio.charset.Charset;
import java.io.InputStream;
import java.io.Reader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import io.netty.handler.codec.http2.Http2Headers;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class Intercept extends Fragment {
	private final Queue<HttpRequestEntry> httpRequestEntryQueue = new ConcurrentLinkedQueue<>();
	private final Queue<RequestEntry> requestQueue = new ConcurrentLinkedQueue<>();

	private PopupMenu cachedPopupMenu;
	private Handler handler = new Handler(Looper.getMainLooper());
	private InterceptorBinding interceptorBinding;

	private int streamId;
	private volatile boolean isProcessing = false;
	private final ExecutorService executorService = Executors.newCachedThreadPool();

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		interceptorBinding = InterceptorBinding.inflate(arg0, arg1, false);
		View view = interceptorBinding.getRoot();
		listener();
		clickListeners();

		return view;
	}

	private void clickListeners() {
		interceptButtonListener();
		interceptForwardButtonListener();

	}

	private void toggleHttpInterception() {
		HttpInterceptorListener.getInstance().toggleInterception();
	}

	private void toggleHttp2Interception() {
		Http2InterceptorListener.getInstance().toggleInterception();
	}

	private void interceptButtonListener() {
		interceptorBinding.interceptorButtonIntercept.setOnClickListener(v -> {
			executorService.execute(() -> {
				try {
					processPendingRequests(); // Forward any pending requests before toggling.

				} catch (Exception e) {
					handleError(e);
				}
			});
			toggleHttpInterception();
			toggleHttp2Interception();
			updateButtonText();

		});

	}

	private void interceptForwardButtonListener() {
		interceptorBinding.interceptorButtonForward.setOnClickListener(v -> {
			String requestText = interceptorBinding.interceptorEditText.getText().toString();
			executorService.execute(() -> {

				if (requestText.startsWith(":")) {
					processHttp2Request();
				} else {
					processHttp1Request();
				}

			});
		});
	}

	private Http2DataEvent data(String data) {
		ByteBuf buf = Unpooled.copiedBuffer(data, StandardCharsets.UTF_8);
		Http2DataEvent dataEvent = new Http2DataEvent(streamId, buf, 0, true);
		return dataEvent;
	}

	private void processPendingRequests() {
		if (!requestQueue.isEmpty() || !httpRequestEntryQueue.isEmpty()) {
			forwardAllRemainingRequests();

		}
	}

	/**
	 * Toggles HTTP interception using the HttpInterceptorListener.
	 */

	private void updateButtonText() {
		handler.postDelayed(() -> {
			boolean interceptionOn = HttpInterceptorListener.getInstance().isInterceptionOn()
					&& Http2InterceptorListener.getInstance().isInterceptionOn();
			interceptorBinding.interceptorButtonIntercept.setTextColor(interceptionOn ? Color.BLACK : Color.WHITE);

			interceptorBinding.interceptorButtonIntercept
					.setBackground(interceptionOn ? requireContext().getDrawable(R.drawable.rounded_corner)
							: requireContext().getDrawable(R.drawable.rounded_corner_bg));
			interceptorBinding.interceptorButtonIntercept
					.setTypeface(interceptorBinding.interceptorButtonIntercept.getTypeface(), Typeface.BOLD);
		}, 50);
	}

	private void handleError(Exception e) {
		Log.e("InterceptionError", "Error during interception toggle", e); // Log the full exception details (including stack trace)
		handler.postDelayed(() -> {
			Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
		}, 20);
	}

	private void processHttp2Request() {
		synchronized (requestQueue) {
			RequestEntry requestEntry = requestQueue.poll();
			if (requestEntry != null && interceptorBinding.interceptorEditText != null) {
				String requestText = interceptorBinding.interceptorEditText.getText().toString();
				String[] parts = requestText.split("\n\n", 2);
				String extractedRequest = parts.length > 0 ? parts[0] : "";
				String extractedBody = parts.length > 1 ? parts[1] : "";
				forwardHttp2Request(streamId, extractedRequest, extractedBody);
			} else {
				isProcessing = false;
				if (interceptorBinding.interceptorEditText != null) {
					handler.post(() -> {
						interceptorBinding.interceptorEditText.setText("");
					});
				}
			}
		}
	}

	private void forwardHttp2Request(int streamId, String request, String body) {
		Http2InterceptorListener.getInstance().forwardOnClick(streamId, request, new OnHeadersForwarded() {
			@Override
			public void onForwarded() {
				if (!TextUtils.isEmpty(body)) {
					Http2InterceptorListener.getInstance().forwardData(data(body), streamId);
				}
				isProcessing = false;
				processNextRequest();
			}
		});
	}

	private void processHttp1Request() {
		synchronized (httpRequestEntryQueue) {
			if (!httpRequestEntryQueue.isEmpty()) {
				HttpRequestEntry httpRequestEntry = httpRequestEntryQueue.poll();
				try {
					Http1RequestHeaders requestHeaders = parseHeaders(
							interceptorBinding.interceptorEditText.getText().toString());
					HttpInterceptorListener.getInstance().forwardOnClick(streamId, updatedHttpRequest(requestHeaders));
					isProcessing = false;
					processNextHttpRequest();
				} catch (IllegalArgumentException e) {
					Toast.makeText(requireContext(), "Invalid request format", Toast.LENGTH_SHORT).show();
				}
			} else {
				handler.post(() -> {
					interceptorBinding.interceptorEditText.setText("");
				});
			}
		}
	}

	private void listener() {
		HttpMessageListener.setLogCallBack(new HttpMessageListener.LogCallBack() {
			@Override
			public void onLog(Object msg, int id) {
				try {
					streamId = id;
					if (msg instanceof IHttp2HeadersEvent) {
						handleHttp2Headers((IHttp2HeadersEvent) msg, id);
					} else if (msg instanceof Http2Message) {

						Http2Message message = (Http2Message) msg;
						handleHttp2Message(message, id);

					} else if (msg instanceof Http1Message) {
						handleHttp1Message((Http1Message) msg, id);
					}
				} catch (Exception e) {
					Log.e("HttpMessageListener", "Error processing message: " + e.getMessage());
				}
			}
		});
	}

	private void handleHttp2Headers(IHttp2HeadersEvent headersEvent, int id) {
		Http2RequestHeaders http2RequestHeaders = onRequestHeaders(id, headersEvent.headers(),
				headersEvent.endOfStream());
		requestQueue.offer(new RequestEntry(id, http2RequestHeaders, null));
		processNextRequest();
	}

	private void handleHttp2Message(Http2Message message, int streamId) {
		Http2RequestHeaders http2RequestHeaders = message.requestHeader();

		requestQueue.offer(new RequestEntry(streamId, http2RequestHeaders, message.requestBody()));
		processNextRequest();

	}

	private void processNextRequest() {

		if (!requestQueue.isEmpty() && !isProcessing) {
			isProcessing = true; // Lock processing
			showNextRequest();

		}
	}

	/*private void showNextRequest() {
		synchronized (requestQueue) {
			if (!requestQueue.isEmpty()) {
				RequestEntry nextRequest = requestQueue.peek();
				String body = nextRequest.getBody() != null ? getBody(nextRequest.getBody()) : "";
				handler.post(() -> {
					editTextRequest.setText(String.join("\n", nextRequest.getHeaders().rawLines()) + "\n\n" + body);
					// Update UI here
				});
	
				streamId = nextRequest.getStreamId(); // Update the current streamId
			} else {
				// Clear the EditText if there are no more requests
				handler.post(() -> {
					editTextRequest.setText("");
				});
				streamId = -1; // Reset the streamId
	
			}
		}
	}*/
	private void showNextRequest() {
		RequestEntry nextRequest;
		synchronized (requestQueue) {
			nextRequest = requestQueue.peek();
		}
		executorService.execute(() -> {
			if (nextRequest != null) {
				String body = nextRequest.getBody() != null ? getBody(nextRequest.getBody()) : "";

				StringBuilder sb = new StringBuilder();
				for (String line : nextRequest.getHeaders().rawLines()) {
					sb.append(line).append("\n");
				}
				sb.append("\n").append(body);

				String newText = sb.toString();
				streamId = nextRequest.getStreamId(); // Update outside synchronized for better clarity

				handler.post(() -> {
					if (!interceptorBinding.interceptorEditText.getText().toString().equals(newText)) {
						interceptorBinding.interceptorEditText.setText(newText);
					}
				});
			} else {
				streamId = -1;
				handler.post(() -> interceptorBinding.interceptorEditText.setText(""));
			}
		});

	}

	private void handleHttp1Message(Http1Message message, int id) {
		Http1RequestHeaders requestHeaders = message.requestHeader();
		Body body = message.requestBody();

		synchronized (httpRequestEntryQueue) {
			httpRequestEntryQueue.offer(new HttpRequestEntry(id, requestHeaders, body));
			processNextHttpRequest();
		}
	}

	private void processNextHttpRequest() {
		synchronized (httpRequestEntryQueue) {
			if (!httpRequestEntryQueue.isEmpty() && !isProcessing) {
				isProcessing = true; // Lock processing
				showNextHttpRequest();
			}
		}
	}

	private void showNextHttpRequest() {

		if (!httpRequestEntryQueue.isEmpty()) {
			HttpRequestEntry nextRequest = httpRequestEntryQueue.peek(); // Get the first request without removing
			Http1RequestHeaders requestHeaders = nextRequest.getHeaders();

			handler.post(() -> {
				interceptorBinding.interceptorEditText.setText(String.join("\n", requestHeaders.rawLines()));
				// Update UI here
			});

			streamId = nextRequest.getStreamId(); // Update the current streamId
		} else {

			handler.post(() -> {
				interceptorBinding.interceptorEditText.setText("");
				streamId = -1; // Reset the streamId
			});
		}

	}

	private void forwardAllRemainingRequests() {
		// Forward remaining HTTP/2 requests

		while (!requestQueue.isEmpty()) {
			RequestEntry currentRequest = requestQueue.poll(); // Remove the current request
			Http2InterceptorListener.getInstance().forwardOnClick(currentRequest.getStreamId(),
					String.join("\n", currentRequest.getHeaders().rawLines()), new OnHeadersForwarded() {
						@Override
						public void onForwarded() {
						}
					});

		}

		while (!httpRequestEntryQueue.isEmpty()) {
			HttpRequestEntry httpRequestEntry = httpRequestEntryQueue.poll(); // Remove the current request
			try {
				Http1RequestHeaders requestHeaders = httpRequestEntry.getHeaders();
				Body body = httpRequestEntry.getBody();
				HttpInterceptorListener.getInstance().forwardOnClick(httpRequestEntry.getStreamId(),
						updatedHttpRequest(requestHeaders));
			} catch (IllegalArgumentException e) {
				e.printStackTrace(); // Log any parsing issues
			}
		}

	}

	private Http1RequestHeaders parseHeaders(String rawHeaders) {
		if (rawHeaders == null || rawHeaders.trim().isEmpty()) {
			throw new IllegalArgumentException("Raw headers cannot be null or empty");
		}

		List<Header> updatedHeaders = new ArrayList<>();
		String[] lines = rawHeaders.split("\\r?\\n");
		String[] topPart = null;

		for (int i = 0; i < lines.length; i++) {
			String line = lines[i].trim();

			if (i == 0) {
				// Parse the request line (e.g., "GET /path HTTP/1.1")
				topPart = parseRequestLine(line);
			} else {
				// Parse headers (e.g., "Host: example.com")
				parseHeaderLine(line, updatedHeaders);
			}
		}

		if (topPart == null || topPart.length != 3) {
			throw new IllegalStateException("Request headers missing");
		}

		RequestLine requestLine = new RequestLine(topPart[0], topPart[1], topPart[2]);
		return new Http1RequestHeaders(requestLine, updatedHeaders);
	}

	private String[] parseRequestLine(String line) {
		String[] parts = line.split(" ");
		if (parts.length != 3 || !parts[2].startsWith("HTTP/")) {
			throw new IllegalArgumentException("Invalid request line: " + line);
		}
		return parts;
	}

	private void parseHeaderLine(String line, List<Header> headers) {
		String[] headerParts = line.split(":", 2);
		if (headerParts.length == 2) {
			String name = headerParts[0].trim();
			String value = headerParts[1].trim();
			if (!name.isEmpty() && !value.isEmpty()) {
				headers.add(new Header(name, value));
			} else {
				throw new IllegalArgumentException("Invalid header: " + line);
			}
		} else {
			throw new IllegalArgumentException("Invalid header format: " + line);
		}

	}

	private HttpRequest updatedHttpRequest(Http1RequestHeaders http1RequestHeaders) {
		if (http1RequestHeaders == null) {
			throw new IllegalArgumentException("Invalid headers");
		}

		// Ensure the Host header is set
		String host = http1RequestHeaders.headers().stream().filter(header -> "Host".equalsIgnoreCase(header.name()))
				.map(Header::value).findFirst().orElse(null);

		if (host == null || host.isEmpty()) {
			throw new IllegalStateException("Missing Host header");
		}

		HttpVersion version = HttpVersion.valueOf(http1RequestHeaders.requestLine().version());
		HttpMethod method = HttpMethod.valueOf(http1RequestHeaders.requestLine().method());
		String uri = http1RequestHeaders.requestLine().path();

		HttpRequest request = new DefaultHttpRequest(version, method, uri);
		for (Header header : http1RequestHeaders.headers()) {
			request.headers().set(header.name(), header.value());
		}

		// Ensure the Host header is set (if not already in the headers)
		if (!request.headers().contains("Host")) {
			request.headers().set("Host", host);
		}

		return request;
	}

	public static String extractPath(String httpRequest) {
		// Extract the first line and get the path part
		String firstLine = httpRequest.split("\n")[0];
		String[] parts = firstLine.split(" ");
		return parts.length > 1 ? parts[1] : null; // Return the second part (the path)
	}

	private Http2RequestHeaders onRequestHeaders(int streamId, Http2Headers http2Headers, boolean endOfStream) {

		List<Header> headers = convertHeaders(http2Headers);
		Http2RequestHeaders requestHeaders = new Http2RequestHeaders(headers, http2Headers.scheme().toString(),
				http2Headers.method().toString(), http2Headers.path().toString());

		return requestHeaders;
	}

	private List<Header> convertHeaders(Http2Headers nettyHeaders) {
		ArrayList<Header> http2HeadersArrayList = new ArrayList<>();
		for (Map.Entry next : nettyHeaders) {
			if (next.getKey() != null) {
				http2HeadersArrayList.add(new Header(next.getKey().toString(), next.getValue().toString()));
			}
		}
		return http2HeadersArrayList;
	}

	private static Http1RequestHeaders converHttpHeaders(HttpRequest request) {

		RequestLine requestLine = new RequestLine(request.method().name(), request.uri(),
				request.protocolVersion().text());
		List<Header> headers = new ArrayList<>();

		request.headers().iteratorAsString().forEachRemaining(h -> headers.add(new Header(h.getKey(), h.getValue())));
		return new Http1RequestHeaders(requestLine, headers);
	}

	private String getBody(Body body) {
		String text = "";
		BodyType bodyType = body.type();
		Charset charset = body.charset().orElse(CharsetUtil.UTF_8);
		if (bodyType.isText()) {

			try (InputStream input = body.getDecodedInputStream();
					Reader reader = new InputStreamReader(input, charset)) {
				text = readAll(reader);
			} catch (IOException e) {

			}
		}
		if (text.equals("")) {
			text = body.content().toString(StandardCharsets.UTF_8);
		}
		for (Beautifier beautifier : Utils.beautifiers) {
			if (beautifier.accept(bodyType)) {

				text = beautifier.beautify(text, CharsetUtil.UTF_8);
				break;
			}
		}

		return text;
	}

	public static String readAll(Reader reader) throws IOException {
		StringBuilder sb = new StringBuilder();
		char[] buffer = new char[1024]; // Buffer for reading characters in chunks
		int read;

		// Read characters in chunks and append to StringBuilder
		while ((read = reader.read(buffer)) != -1) {
			sb.append(buffer, 0, read);
		}

		return sb.toString(); // Return the concatenated string

	}

	public class RequestEntry {

		private final int streamId;
		private final Http2RequestHeaders headers;
		Body body;

		public RequestEntry(int streamId, Http2RequestHeaders headers, Body body) {
			this.streamId = streamId;
			this.headers = headers;
			this.body = body;
		}

		public Body getBody() {
			return this.body;
		}

		public int getStreamId() {
			return streamId;
		}

		public Http2RequestHeaders getHeaders() {
			return headers;
		}
	}

	public class HttpRequestEntry {
		private final int streamId;
		private final Http1RequestHeaders headers;
		private final Body body; // Make body final as well for immutability.

		public HttpRequestEntry(int streamId, Http1RequestHeaders headers, Body body) {
			this.streamId = streamId;
			this.headers = headers;

			this.body = body;
		}

		public int getStreamId() {
			return streamId;
		}

		public Http1RequestHeaders getHeaders() {
			return headers;
		}

		public Body getBody() {
			return body;
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdown(); // Initiate shutdown (waits for tasks to complete).
			requestQueue.clear();
			httpRequestEntryQueue.clear();

		}
	}

}