package com.proxy.Fuzzer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.proxy.data.Header;
import com.proxy.data.Http2RequestHeaders;
import com.proxy.data.HttpHeaders;
import com.proxy.data.HttpRequestHeaders;
import com.proxy.listener.SetLogger;

import com.proxy.store.Body;
import io.netty.handler.codec.compression.Brotli;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.ArrayList;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.concurrent.TimeUnit;
import java.net.URI;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.InflaterInputStream;
import kotlin.Pair;
import okhttp3.Call;
import okhttp3.Dispatcher;
import okhttp3.Headers;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.OkHttpClient;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.RequestBody;
import java.util.concurrent.ExecutorService;
import java.util.regex.Pattern;

import okio.Buffer;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Okio;
import okio.GzipSource;
import okio.BufferedSource;
import org.brotli.dec.BrotliInputStream;
import org.tukaani.xz.LZMAInputStream;

public class Fuzzer {
	private static final String TAG = "AndroidNettyFuzzer";
	private static final Pattern FUZZ_PATTERN = Pattern.compile("\\{\\{FUZZ(\\d*)\\}\\}");

	private ExecutorService executor;
	private final int maxConcurrent;
	private final Map<String, List<String>> payloadLists = new HashMap<>();
	private final Map<Integer, ResponseAnalyzer> responseAnalyzers = new ConcurrentHashMap<>();
	private final AtomicInteger completedRequests = new AtomicInteger(0);
	private final AtomicInteger totalRequests = new AtomicInteger(0);
	private final AtomicInteger activeRequests = new AtomicInteger(0);
	private final Handler mainThreadHandler;
	private FuzzProgressListener progressListener;
	private final OkHttpClient client;
	// Add these as class fields 

	private volatile boolean isStopFuzzing = false;

	//	private ExecutorService executor;
	private volatile boolean cancelled = false;

	public Fuzzer(int threads) {
		this.executor = Executors.newFixedThreadPool(threads);

		this.maxConcurrent = threads;
		this.mainThreadHandler = new Handler(Looper.getMainLooper());

		/*	client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).followRedirects(false)
					.readTimeout(30, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true)
					.build();*/
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(maxConcurrent); // Or even 2 * maxConcurrent
		dispatcher.setMaxRequestsPerHost(maxConcurrent);

		client = new OkHttpClient.Builder().dispatcher(dispatcher).connectTimeout(5, TimeUnit.SECONDS)
				.readTimeout(30, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true)
				.followRedirects(false).build();

		//	client.dispatcher().setMaxRequests(50);
	}

	public void stopFuzzing() {
		isStopFuzzing = true;
		if (client != null) {

			// Cancel all HTTP calls
			client.dispatcher().cancelAll();

			// Shutdown executor
			executor.shutdownNow();

		}
	}

	public void setProgressListener(FuzzProgressListener listener) {
		this.progressListener = listener;
	}

	public void addPayloadList(String name, List<String> payloads) {
		payloadLists.put(name, payloads);
	}

	private int calculateTotalRequestCount(Map<Integer, String> fuzzPoints) {
		int total = 1;
		for (Map.Entry<Integer, String> entry : fuzzPoints.entrySet()) {
			String key = entry.getValue();
			List<String> payloads = payloadLists.getOrDefault(key, List.of("MISSING_PAYLOAD"));
			total *= payloads.size();
		}
		return total;
	}

	public void addResponseAnalyzer(int statusCode, ResponseAnalyzer analyzer) {
		responseAnalyzers.put(statusCode, analyzer);
	}

	public void fuzz(String requestTemplate, String targetUrl, final FuzzResultCallback callback) {
		FuzzResults results = new FuzzResults();
		isStopFuzzing = false;

		try {
			URI uri = new URI(targetUrl);
			String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
			String host = uri.getHost();
			int port = uri.getPort() != -1 ? uri.getPort() : (scheme.equals("https") ? 443 : 80);

			Map<Integer, String> fuzzPoints = extractFuzzPoints(requestTemplate);
			if (fuzzPoints.isEmpty()) {
				throw new IllegalArgumentException("No fuzzing points found in template.");
			}

			for (String fuzzKey : fuzzPoints.values()) {
				if (!payloadLists.containsKey(fuzzKey)) {
					Log.w(TAG, "No payload list for: " + fuzzKey);
					payloadLists.put(fuzzKey, List.of("MISSING_PAYLOAD"));
				}
			}

			//List<Map<Integer, String>> payloadCombinations = generatePayloadCombinations(fuzzPoints);
			PayloadCombinationIterator combinationIterator = new PayloadCombinationIterator(fuzzPoints, payloadLists);

			int totalCount = calculateTotalRequestCount(fuzzPoints);
			totalRequests.set(totalCount);
			updateProgress(0, totalRequests.get());
			//RequestBuilderTemplate requestBuilderTemplate = new RequestBuilderTemplate(requestTemplate, host);
			Semaphore semaphore = new Semaphore(maxConcurrent);
			while (combinationIterator.hasNext()) {
				Map<Integer, String> payloadMap = combinationIterator.next();

				//for (Map<Integer, String> payloadMap : payloadCombinations) {
				executor.submit(() -> {
					try {
						if (isStopFuzzing) {
							return;
						}
						semaphore.acquire();
						activeRequests.incrementAndGet();

						String processedRequest = replacePayloadsEfficient(requestTemplate, payloadMap);
						Request request = constructRequest(processedRequest, host);
						//	Request request = requestBuilderTemplate.build(payloadMap);
						long startTime = System.currentTimeMillis();

						client.newCall(request).enqueue(new Callback() {
							@Override
							public void onFailure(Call call, IOException e) {
								
								completeRequest(null);
							}

							@Override
							public void onResponse(Call call, Response response) {
								long endTime = System.currentTimeMillis();
								long responseTime = endTime - startTime;

								try {
									FuzzResult result = new FuzzResult(request, response, payloadMap, responseTime);
									results.addResult(result);

									mainThreadHandler.post(() -> callback.onNewResponse(result));
								} catch (Exception ex) {
									
								} finally {
									completeRequest(response);
								}
							}

							private void completeRequest(Response response) {
								int completed = completedRequests.incrementAndGet();
								activeRequests.decrementAndGet();
								semaphore.release();
								updateProgress(completed, totalRequests.get());

								if (completed == totalRequests.get()) {
									executor.shutdown();
								}

								if (response != null) {
									response.close();
								}
							}
						});

					} catch (Exception e) {
						//	SetLogger.log(TAG + "Error sending request: " + e);
						semaphore.release();
						activeRequests.decrementAndGet();
						int completed = completedRequests.incrementAndGet();
						updateProgress(completed, totalRequests.get());
					}
				});
			}

		} catch (Exception e) {
			SetLogger.log(e.getMessage());
			mainThreadHandler.post(() -> callback.onError(e));
		}
	}

	private void updateProgress(int completed, int total) {
		if (progressListener != null) {

			mainThreadHandler.post(() -> progressListener.onProgressUpdate(completed, total));
		}
	}

	private Map<Integer, String> extractFuzzPoints(String template) {
		Map<Integer, String> fuzzPoints = new HashMap<>();
		Matcher matcher = FUZZ_PATTERN.matcher(template);

		while (matcher.find()) {
			String indexStr = matcher.group(1);
			int index = indexStr.isEmpty() ? 0 : Integer.parseInt(indexStr);
			fuzzPoints.put(index, "FUZZ" + indexStr);
		}

		return fuzzPoints;
	}

	private List<Map<Integer, String>> generatePayloadCombinations(Map<Integer, String> fuzzPoints) {
		List<Map<Integer, String>> combinations = new ArrayList<>();

		List<Integer> fuzzIndices = new ArrayList<>(fuzzPoints.keySet());
		Collections.sort(fuzzIndices);

		List<List<String>> payloadMatrix = new ArrayList<>();
		for (Integer index : fuzzIndices) {
			String fuzzKey = fuzzPoints.get(index);
			List<String> payloads = payloadLists.getOrDefault(fuzzKey, List.of("MISSING_PAYLOAD"));
			payloadMatrix.add(payloads);
		}

		int[] indices = new int[payloadMatrix.size()];
		while (true) {
			Map<Integer, String> combination = new HashMap<>();
			for (int i = 0; i < fuzzIndices.size(); i++) {
				combination.put(fuzzIndices.get(i), payloadMatrix.get(i).get(indices[i]));
			}
			combinations.add(combination);

			// Increment indices
			int position = indices.length - 1;
			while (position >= 0) {
				indices[position]++;
				if (indices[position] < payloadMatrix.get(position).size())
					break;
				indices[position] = 0;
				position--;
			}
			if (position < 0)
				break; // All combinations done
		}

		Log.d(TAG, "Generated combinations: " + combinations.size());
		return combinations;
	}

	private void generateCombinationsRecursive(Map<Integer, String> current, List<Integer> fuzzIndices, int index,
			List<Map<Integer, String>> result, Map<Integer, String> fuzzPoints) {
		if (index >= fuzzIndices.size()) {
			result.add(new HashMap<>(current));
			return;
		}

		int fuzzIndex = fuzzIndices.get(index);
		String fuzzKey = fuzzPoints.get(fuzzIndex); // Get the actual fuzz key for this index
		List<String> payloads = payloadLists.getOrDefault(fuzzKey, List.of("MISSING_PAYLOAD"));

		// Log if we're using very few payloads - might help with debugging
		if (payloads.size() <= 1) {
			Log.d(TAG, "Using only " + payloads.size() + " payloads for " + fuzzKey);
		}

		for (String payload : payloads) {
			current.put(fuzzIndex, payload);
			generateCombinationsRecursive(current, fuzzIndices, index + 1, result, fuzzPoints);
		}
	}

	private String replacePayloadsFast(String template, Map<Integer, String> payloadMap) {
		StringBuilder result = new StringBuilder();
		int i = 0;
		while (i < template.length()) {
			if (template.charAt(i) == '{' && i + 1 < template.length() && template.charAt(i + 1) == '{') {
				int endIndex = template.indexOf("}}", i);
				if (endIndex != -1) {
					String key = template.substring(i + 2, endIndex);
					String payload = payloadMap.getOrDefault(parseFuzzIndex(key), "MISSING_PAYLOAD");
					result.append(payload);
					i = endIndex + 2;
					continue;
				}
			}
			result.append(template.charAt(i));
			i++;
		}
		return result.toString();
	}

	private int parseFuzzIndex(String key) {
		if (key.equals("FUZZ"))
			return 0;
		if (key.startsWith("FUZZ")) {
			try {
				return Integer.parseInt(key.substring(4));
			} catch (NumberFormatException e) {
				return 0;
			}
		}
		return 0;
	}

	private String replacePayloadsEfficient(String template, Map<Integer, String> payloadMap) {
		Matcher matcher = FUZZ_PATTERN.matcher(template);
		StringBuffer sb = new StringBuffer();

		while (matcher.find()) {
			String indexStr = matcher.group(1);
			int index = indexStr.isEmpty() ? 0 : Integer.parseInt(indexStr);
			String payload = payloadMap.getOrDefault(index, "MISSING_PAYLOAD");

			matcher.appendReplacement(sb, Matcher.quoteReplacement(payload));
		}
		matcher.appendTail(sb);
		return sb.toString();
	}

	public Request constructRequest(String rawInput, String host) {
		// Split the raw input into lines
		String[] lines = rawInput.split("\\r?\\n");
		if (lines.length == 0) {
			throw new IllegalArgumentException("Input is empty");
		}

		// Extract the request line (first line)
		String requestLine = lines[0].trim();
		String[] requestParts = requestLine.split(" ");
		if (requestParts.length != 3 || !requestParts[2].startsWith("HTTP/")) {
			throw new IllegalArgumentException("Invalid request line: " + requestLine);
		}
		String method = requestParts[0];
		String uri = requestParts[1];
		String version = requestParts[2];

		// Extract headers (lines until the first empty line)
		Headers.Builder headersBuilder = new Headers.Builder();
		int i = 1;
		for (; i < lines.length; i++) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				break; // End of headers
			}
			String[] headerParts = line.split(":", 2);
			if (headerParts.length == 2) {
				headersBuilder.add(headerParts[0].trim(), headerParts[1].trim());
			}
		}

		// Extract body (everything after the empty line)
		StringBuilder bodyBuilder = new StringBuilder();
		for (i = i + 1; i < lines.length; i++) {
			bodyBuilder.append(lines[i]);
			if (i < lines.length - 1) {
				bodyBuilder.append("\n");
			}
		}

		RequestBody body = null;
		if (!bodyBuilder.toString().trim().isEmpty()) {
			body = RequestBody.create(bodyBuilder.toString(), MediaType.parse(
					headersBuilder.get("Content-Type") != null ? headersBuilder.get("Content-Type") : "text/plain"));
		}

		return new Request.Builder().url("https://" + host + uri).headers(headersBuilder.build()).method(method, body)
				.build();
	}

	// Inner classes and interfaces

	public interface RequestCallback {
		void onResponse(Response response);
	}

	public interface ResponseAnalyzer {
		void analyze(FuzzResult result);
	}

	public interface FuzzResultsCallback {
		void onComplete(FuzzResults results);

		void onError(Exception e);
	}

	public interface FuzzResultCallback {
		void onNewResponse(FuzzResult results);

		void onError(Exception e);
	}

	public interface FuzzProgressListener {
		void onProgressUpdate(int completed, int total);
	}

	public static class Semaphore {
		private final int maxPermits;
		private int permits;

		public Semaphore(int permits) {
			this.maxPermits = permits;
			this.permits = permits;
		}

		public synchronized void acquire() throws InterruptedException {
			while (permits <= 0) {
				wait();
			}
			permits--;
		}

		public synchronized void release() {
			permits = Math.min(permits + 1, maxPermits);
			notify();
		}
	}

	public class FuzzResult {
		private final Request request;
		private final Response response;
		private final Map<Integer, String> payloads;
		private final long responseTimeMillis;

		private final String headers;
		private final String responseBody;
		private final String requestString;
		private final int statusCode;
		private final int contentLength;
		private final String path;

		public FuzzResult(Request request, Response response, Map<Integer, String> payloads, long responseTimeMillis)
				throws IOException {
			this.request = request;
			this.response = response;
			this.payloads = payloads;
			this.responseTimeMillis = responseTimeMillis;

			this.statusCode = response.code();
			this.path = request.url().encodedPath();
			ResponseBody body = response.body();

			this.responseBody = decodeBodyFromEncoding(response);
			contentLength = responseBody.length();
			this.headers = buildResponseHeaderString(response);
			this.requestString = buildRequestString(request, response.protocol());

		}

		public Request getRequest() {
			return request;
		}

		public Response getResponse() {
			return response;
		}

		public Map<Integer, String> getPayloads() {
			return payloads;
		}

		public long getResponseTimeMillis() {
			return responseTimeMillis;
		}

		public int getStatusCode() {
			return statusCode;
		}

		public int getContentLength() {
			return contentLength;
		}

		public String getPath() {
			return path;
		}

		public String getHeaders() {
			return headers;
		}

		public String getResponseBody() {
			return responseBody;
		}

		public String getRequestString() {
			return requestString;
		}

		@Override
		public String toString() {
			return "Status: " + statusCode + "\n" + "Content-Length: " + contentLength + "\n" + "Response Time (ms): "
					+ responseTimeMillis + "\n" + "Payloads: " + payloads;
		}

		public String buildResponseHeaderString(Response response) {
			StringBuilder headerBuilder = new StringBuilder();

			String protocolReadable = "";
			switch (response.protocol()) {
			case HTTP_1_0:
				protocolReadable = "HTTP/1.0";
				break;
			case HTTP_1_1:
				protocolReadable = "HTTP/1.1";
				break;
			case HTTP_2:
				protocolReadable = "HTTP/2";
				break;
			default:
				protocolReadable = response.protocol().toString();
			}

			headerBuilder.append(protocolReadable).append(" ").append(response.code()).append(" ")
					.append(response.message()).append("\n");

			Headers headersRes = response.headers();
			for (int i = 0; i < headersRes.size(); i++) {
				headerBuilder.append(headersRes.name(i)).append(": ").append(headersRes.value(i)).append("\n");
			}

			return headerBuilder.toString().trim();
		}

		public String buildRequestString(Request request, Protocol protocol) throws IOException {
			StringBuilder requestBuilder = new StringBuilder();

			URL url = request.url().url();
			String protocolReadable = "";
			switch (protocol) {
			case HTTP_1_0:
				protocolReadable = "HTTP/1.0";
				break;
			case HTTP_1_1:
				protocolReadable = "HTTP/1.1";
				break;
			case HTTP_2:
				protocolReadable = "HTTP/2";
				break;
			default:
				protocolReadable = protocol.toString();
			}

			requestBuilder.append(request.method()).append(" ").append(url.getPath()).append(" ")
					.append(protocolReadable).append("\n");

			Headers headers = request.headers();
			for (int i = 0; i < headers.size(); i++) {
				requestBuilder.append(headers.name(i)).append(": ").append(headers.value(i)).append("\n");
			}

			if (request.body() != null) {
				Buffer buffer = new Buffer();
				request.body().writeTo(buffer);
				requestBuilder.append("\n").append(buffer.readUtf8());
			}

			return requestBuilder.toString().trim();
		}

	}

	public String formatHttpResponse(Response response) {
		StringBuilder builder = new StringBuilder();

		// Status line (e.g., HTTP/1.1 200 OK)
		builder.append(response.protocol().toString().toUpperCase()) // HTTP/1.1 or H2_PRIOR_KNOWLEDGE
				.append(" ").append(response.code()).append(" ").append(response.message()).append("\n");

		// Headers
		for (String name : response.headers().names()) {
			for (String value : response.headers(name)) {
				builder.append(name).append(":  ").append(value).append("\n");
			}
		}

		return builder.toString();
	}

	public static class FuzzResults {
		private final List<FuzzResult> results = new ArrayList<>();

		public synchronized void addResult(FuzzResult result) {
			results.add(result);
		}

		public List<FuzzResult> getResults() {
			return results;
		}

	}

	public static String decodeBodyFromEncoding(Response response) throws IOException {
		ResponseBody body = response.body();

		if (body == null)
			return "";

		String encoding = response.header("Content-Encoding", "").toLowerCase();
		BufferedSource source;

		switch (encoding) {
		case "gzip":
			source = Okio.buffer(new GzipSource(body.source()));

			return source.readUtf8();

		case "deflate":
			InflaterInputStream inflaterInputStream = new InflaterInputStream(body.byteStream());
			return Body.readAll(new InputStreamReader(inflaterInputStream, StandardCharsets.UTF_8));

		case "br":
			BrotliInputStream inputStream = new BrotliInputStream(body.byteStream());
			// Brotli support requires an external library like Brotli4j
			return Body.readAll(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
		case "lzma":
			LZMAInputStream lZMAInputStream = new LZMAInputStream(body.byteStream(), -1);
			return Body.readAll(new InputStreamReader(lZMAInputStream, StandardCharsets.UTF_8));

		case "compress":
			return "[Unsupported: Content-Encoding=compress (LZW)]";

		case "":
		case "identity":
		default:
			return body.string(); // fallback
		}
	}

	public class PayloadCombinationIterator implements Iterator<Map<Integer, String>> {
		private final List<Integer> fuzzIndices;
		private final List<List<String>> payloadMatrix;
		private final int[] indices;

		private boolean hasNext = true;

		public PayloadCombinationIterator(Map<Integer, String> fuzzPoints, Map<String, List<String>> payloadLists) {
			this.fuzzIndices = new ArrayList<>(fuzzPoints.keySet());
			Collections.sort(fuzzIndices);
			this.payloadMatrix = new ArrayList<>();

			for (Integer index : fuzzIndices) {
				String key = fuzzPoints.get(index);
				List<String> payloads = payloadLists.getOrDefault(key, List.of("MISSING_PAYLOAD"));
				payloadMatrix.add(payloads);
			}

			this.indices = new int[payloadMatrix.size()];
		}

		@Override
		public boolean hasNext() {
			return hasNext;
		}

		@Override
		public Map<Integer, String> next() {
			Map<Integer, String> combination = new HashMap<>();
			for (int i = 0; i < fuzzIndices.size(); i++) {
				combination.put(fuzzIndices.get(i), payloadMatrix.get(i).get(indices[i]));
			}

			// Increment indices
			int pos = indices.length - 1;
			while (pos >= 0) {
				indices[pos]++;
				if (indices[pos] < payloadMatrix.get(pos).size())
					break;
				indices[pos] = 0;
				pos--;
			}
			if (pos < 0)
				hasNext = false;

			return combination;
		}
	}

	/*	public class RequestBuilderTemplate {
			private final String method;
			private final String uriTemplate;
			private final Headers headers;
			private final String bodyTemplate;
			private final String contentType;
			private final String host;
	
			public RequestBuilderTemplate(String rawRequest, String host) {
				this.host = host;
	
				String[] lines = rawRequest.split("\\r?\\n");
				if (lines.length == 0)
					throw new IllegalArgumentException("Empty request");
	
				// Parse method, URI, version
				String[] requestLine = lines[0].trim().split(" ");
				if (requestLine.length != 3)
					throw new IllegalArgumentException("Invalid request line");
	
				this.method = requestLine[0];
				this.uriTemplate = requestLine[1];
	
				// Parse headers
				Headers.Builder headersBuilder = new Headers.Builder();
				int i = 1;
				for (; i < lines.length; i++) {
					String line = lines[i].trim();
					if (line.isEmpty())
						break;
					String[] parts = line.split(":", 2);
					if (parts.length == 2) {
						headersBuilder.add(parts[0].trim(), parts[1].trim());
					}
				}
				this.headers = headersBuilder.build();
				this.contentType = headers.get("Content-Type") != null ? headers.get("Content-Type") : "text/plain";
	
				// Parse body
				StringBuilder bodyBuilder = new StringBuilder();
				for (i = i + 1; i < lines.length; i++) {
					bodyBuilder.append(lines[i]);
					if (i < lines.length - 1)
						bodyBuilder.append("\n");
				}
				this.bodyTemplate = bodyBuilder.toString();
			}
	
			public Request build(Map<Integer, String> payloadMap) {
				String uri = replaceEfficient(uriTemplate, payloadMap);
				String finalBody = replaceEfficient(bodyTemplate, payloadMap);
	
				RequestBody requestBody = null;
				if (!finalBody.trim().isEmpty()) {
					requestBody = RequestBody.create(finalBody, MediaType.parse(contentType));
				}
	
				return new Request.Builder().url("https://" + host + uri).headers(headers).method(method, requestBody)
						.build();
			}
		}
	
		public static String replaceEfficient(String template, Map<Integer, String> payloadMap) {
			StringBuilder result = new StringBuilder(template.length() + 50); // small extra buffer
			int idx = 0;
			while (idx < template.length()) {
				int start = template.indexOf("{{FUZZ", idx);
				if (start == -1) {
					result.append(template, idx, template.length());
					break;
				}
				result.append(template, idx, start);
				int end = template.indexOf("}}", start);
				if (end == -1) {
					// malformed placeholder, just append rest
					result.append(template.substring(start));
					break;
				}
	
				String placeholder = template.substring(start + 2, end); // e.g., FUZZ2 or FUZZ
				String replacement = "";
	
				if (placeholder.equals("FUZZ")) {
					replacement = payloadMap.getOrDefault(0, "");
				} else if (placeholder.startsWith("FUZZ")) {
					try {
						int index = Integer.parseInt(placeholder.substring(4));
						replacement = payloadMap.getOrDefault(index, "");
					} catch (NumberFormatException e) {
						// invalid placeholder, leave it as is
						replacement = "{{" + placeholder + "}}";
					}
				}
	
				result.append(replacement);
				idx = end + 2;
			}
			return result.toString();
		}*/

}
