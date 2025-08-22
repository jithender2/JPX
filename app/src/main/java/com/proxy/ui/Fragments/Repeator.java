
package com.proxy.ui.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Looper;
import android.os.Handler;
import android.text.Editable;
import android.text.Layout;
import android.text.Spannable;
import android.text.style.BackgroundColorSpan;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;

import android.util.Log;
import android.util.Pair;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import com.proxy.AppContext;
import com.proxy.R;
import android.view.View;
import android.view.LayoutInflater;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.JsonBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.Beautifier;
import com.proxy.beautifier.FormEncodedBeautifier;
import com.proxy.data.ContentType;
import com.proxy.data.Header;
import com.proxy.data.Http1Message;
import com.proxy.data.Http1ResponseHeaders;
import com.proxy.data.Http1RequestHeaders;
import com.proxy.data.Http2Message;
import android.widget.Button;
import android.widget.EditText;
import androidx.fragment.app.Fragment;
import com.proxy.ViewModel.SharedViewModel;
import android.os.Bundle;
import com.proxy.data.RequestLine;
import com.proxy.data.StatusLine;
import com.proxy.databinding.RepeatorBinding;
import com.proxy.listener.MessageListener;
import com.proxy.listener.RedirectHandler;
import com.proxy.listener.SetLogger;
import com.proxy.netty.ProxyHandlerSupplier;
import com.proxy.netty.codec.detector.HttpConnectProxyMatcher;
import com.proxy.netty.codec.detector.ProtocolDetector;
import com.proxy.netty.codec.CloseTimeoutChannelHandler;
import com.proxy.netty.codec.handler.HttpConnectProxyInitializer;
import com.proxy.netty.codec.handler.ServerSSLContextManager;
import com.proxy.ui.FuzzingActivity;
import com.proxy.setting.KeyStoreSetting;
import com.proxy.utils.Chunk;
import com.proxy.utils.DataStore;
import com.proxy.utils.DataStoreManager;
import com.proxy.utils.DataStoreManager;
import com.proxy.utils.HtmlHighlighter;
import com.proxy.utils.Networks;
import com.proxy.utils.Utils;
import io.netty.buffer.ByteBuf;
import com.proxy.store.Body;
import com.proxy.utils.BodyType;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpObject;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.bootstrap.Bootstrap;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http2.Http2DataFrame;
import io.netty.handler.codec.http2.Http2FrameCodec;
import io.netty.handler.codec.http2.Http2FrameCodecBuilder;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.ApplicationProtocolNames;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import io.netty.handler.codec.http2.Http2StreamFrame;
import io.netty.handler.codec.http2.DefaultHttp2HeadersFrame;
import io.netty.handler.codec.http2.Http2MultiplexHandler;
import io.netty.handler.codec.http2.Http2HeadersFrame;
import io.netty.handler.codec.http2.Http2Headers;
import com.proxy.data.HttpMessage;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;

import io.netty.util.concurrent.GenericFutureListener;
import java.io.EOFException;
import java.io.InputStreamReader;
import java.io.Reader;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.zip.GZIPInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URI;
import java.net.HttpURLConnection;
import javax.net.ssl.SSLException;
import io.netty.handler.codec.http2.DefaultHttp2Headers;
import javax.xml.parsers.DocumentBuilder;
import java.util.zip.Inflater;
import javax.xml.parsers.DocumentBuilderFactory;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.Dispatcher;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;
import okio.GzipSource;
import org.tukaani.xz.LZMAInputStream;
import org.xml.sax.InputSource;
import org.brotli.dec.BrotliInputStream;
import java.util.zip.InflaterInputStream;

public class Repeator extends Fragment implements RedirectHandler {
	SharedViewModel sharedViewModel; // ViewModel for sharing data between components.

	Protocol protocol;
	private int currentMatchIndex = 0, currentChunkIndex = 0, PORT = DEFAULT_PORT;
	String contentEncoding, fullResponse;
	private List<Pair<Integer, Integer>> matchPositions = new ArrayList<>();
	private List<MatchPair> positions = new ArrayList<>();
	private List<String> textChunks;
	// Constants
	private static final int DEFAULT_CHUNK_SIZE = 10000, DEFAULT_PORT = 443;

	PopupMenu cachedPopupMenu;

	HttpMessage message;
	private final ExecutorService executorService = Executors.newCachedThreadPool();
	private ViewTreeObserver.OnScrollChangedListener scrollListener;
	private OkHttpClient client;
	RepeatorBinding binding;

	public Repeator() {
	}

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		binding = RepeatorBinding.inflate(arg0, arg1, false);
		View view = binding.getRoot();
		init(view);

		sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
		sharedViewModel.getRepeaterRequests().observe(getViewLifecycleOwner(), new Observer<HttpMessage>() {
			@Override
			public void onChanged(HttpMessage arg0) {
				if (arg0 != null) {
					message = arg0;
					setHttpMessage(arg0);
					PORT = arg0.url().startsWith("https") ? PORT : 80;

					String http1 = binding.repeaterRequest.getText().toString();
					runOnUiThread(() -> binding.repeaterRequest.setText(Utils.highlightAndFormatHttpRequest(http1)));

				}
			}
		});

		return view;
	}

	private void init(View view) {

		binding.repeaterWebview.setWebViewClient(new WebViewClient());

		client = new OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS)
				.writeTimeout(10, TimeUnit.SECONDS).retryOnConnectionFailure(true).followRedirects(false).build();

		setupButtons();
	}

	private void setHttpMessage(HttpMessage message) {
		Body body = message.requestBody();
		StringBuilder request = new StringBuilder();

		for (String line : message.requestHeader().rawLines()) {
			request.append(line).append("\n");
		}

		String requestStr = request.toString();

		String bodyText = Utils.getBody(body);
		if (bodyText == null) {
			bodyText = " ";
		}

		if (requestStr.startsWith(":")) {

			String http1Request = Utils.convertHttp2ToHttp1(requestStr);

			binding.repeaterRequest.setText(http1Request + "\n" + bodyText);

		} else {
			request.append("\n").append(bodyText);
			binding.repeaterRequest.setText(request.toString());
		}
	}

	private void sendRequest(String rawRequest) {
		binding.repeaterButtonFollowRedirect.setVisibility(View.GONE);
		binding.repeaterResponse.setText("");

		if (textChunks != null)
			textChunks.clear();

		final String request = rawRequest;
		String host = Utils.extractHost(rawRequest);
		//HttpRequest httpRequest = constructRequest(request);
		executorService.execute(() -> {
			try {
				replay(constructRequest(request, host), host);

			} catch (IllegalArgumentException e) {
				showToast("error :" + e.getMessage());
			}
		});

	}

	private String extractBody(String text) {
		String[] parts = text.split("\n\n", 2);
		String extractedRequest = parts.length > 0 ? parts[0] : "";
		String extractedBody = parts.length > 1 ? parts[1] : "";
		return extractedBody;
	}

	private String formatHeaders(Map<String, List<String>> headerFields) {
		if (headerFields == null || headerFields.isEmpty()) {
			return ""; // Return empty string if no headers are present
		}

		StringBuilder stringBuilder = new StringBuilder();
		headerFields.forEach((headerName, headerValues) -> {
			if (headerValues == null || headerValues.isEmpty()) {
				return; // Skip empty header values
			}

			if (headerName != null) {
				// Join multiple header values with a comma
				String combinedValues = String.join(", ", headerValues);
				stringBuilder.append(headerName).append(": ").append(combinedValues).append("\n");
			} else {
				// Handle status line (e.g., HTTP version and status code)
				headerValues.forEach(value -> stringBuilder.append(value).append("\n"));
			}
		});

		return stringBuilder.toString();
	}

	private void showToast(String message) {
		runOnUiThread(() -> Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show());
	}

	private static String readStreams(InputStream inputStream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
		StringBuilder response = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			response.append(line.trim()).append("\n");
		}
		reader.close();
		return response.toString();
	}

	private void setupButtons() {
		binding.repeaterSendButton.setOnClickListener(v -> sendRequest(binding.repeaterRequest.getText().toString()));

		binding.repeaterNextArrow.setOnClickListener(v -> goToNextMatch(binding.repeaterResponse.getText().toString()));
		binding.repeaterPreviousArrow
				.setOnClickListener(v -> goToPreviousMatch(binding.repeaterResponse.getText().toString()));

		binding.repeaterSearch.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				executorService.execute(() -> {
					searchInFullText(binding.repeaterSearch.getText().toString(),
							binding.repeaterResponse.getText().toString());
				});
				return true;
			}
			return false;
		});

		binding.repeaterShowFullScreen.setOnClickListener(v -> {
			if (binding.repeaterTopFrame.getVisibility() == View.GONE)
				binding.repeaterTopFrame.setVisibility(View.VISIBLE);
			else {
				binding.repeaterTopFrame.setVisibility(View.GONE);
			}
		});
		binding.repeatorMore.setOnClickListener(v -> {
			showAlert();

		});
		binding.reapeaterResponseMore.setOnClickListener(v -> {
			ShowPopUpMenu();
		});
		binding.repeaterRequest.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				return false;
			}

			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				return false;
			}

			public void onDestroyActionMode(ActionMode mode) {
				// Don’t reset focus here if you want selection to persist
			}
		});

	}

	private void ShowPopUpMenu() {

		if (cachedPopupMenu == null) {
			cachedPopupMenu = new PopupMenu(getActivity(), binding.reapeaterResponseMore);
			cachedPopupMenu.getMenuInflater().inflate(R.menu.repeater_menu, cachedPopupMenu.getMenu());
			cachedPopupMenu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
				case R.id.show_raw_response:
					showAsTextView();
					break;
				case R.id.render_html:
					renderHtmlResponse();
					break;
				case R.id.send_to_intruder:
					Intent intent = new Intent(requireContext(), FuzzingActivity.class);
					intent.putExtra("message", message);

					startActivity(intent);
					break;
				default:
					showContentTypeDialog();
				}

				return true;
			});
		}
		cachedPopupMenu.show();

	}

	private void showAsTextView() {
		binding.repeaterWebview.setVisibility(View.GONE);
		binding.repeaterResponse.setVisibility(View.VISIBLE);
	}

	private void renderHtmlResponse() {
		if (fullResponse == null || fullResponse.trim().isEmpty()) {
			showToast("Response is empty.");
			return;
		}

		String htmlBody = extractHtmlBody(fullResponse);

		if (htmlBody.isEmpty()) {
			showToast("Response does not contain valid HTML.");
			return;
		}
		binding.repeaterWebview.getSettings().setJavaScriptEnabled(true);
		binding.repeaterWebview.getSettings().setDomStorageEnabled(true);
		String formattedResponse = "<meta charset=\"UTF-8\">" + htmlBody;
		binding.repeaterWebview.loadDataWithBaseURL(null, formattedResponse, "text/html", "UTF-8", null);

		binding.repeaterWebview.setVisibility(View.VISIBLE);
		binding.repeaterResponse.setVisibility(View.GONE);
	}

	private String extractHtmlBody(String response) {
		if (response == null || response.trim().isEmpty()) {
			return "";
		}

		// Locate the first "<html" tag, ignoring extra headers or newlines
		Matcher matcher = Pattern.compile("(?i)<\\s*html").matcher(response);
		if (matcher.find()) {
			return response.substring(matcher.start()).trim(); // Extract from "<html" onwards
		}

		return ""; // No valid HTML found
	}

	private void replay(Request request, String host) {
		client.newCall(request).enqueue(new Callback() {
			@Override
			public void onFailure(Call arg0, IOException arg1) {
			}

			@Override
			public void onResponse(Call arg0, Response arg1) {
				protocol = arg1.protocol();
				/*
								try {
									if (arg1.header("content-type", "") != null|arg1.header("content-type", "") !="" ) {
										ContentType contentType = new ContentType(arg1.header("content-type", ""));
										BodyType type = Body.getHttpBodyType(contentType);
										String response = buildResponseHeaderString(arg1) + "\n\n" + decodeBodyFromEncoding(arg1);
										fullResponse = response;
										if (arg1.code() / 100 == 3) {
											String location = arg1.headers().get("Location");
											
											if (location != null) {
												URI redirectUri = new URI(location);
												if (redirectUri.getHost() == null) {
				
													redirectUri = new URI("https://" + host + "/" + location); // Ensure full URL
				
												}
												onRedirectDetected(request, redirectUri, arg1.code());
				
											}
				
										}
										startLazyLoad(response);
									}else {
										String response=buildResponseHeaderString(arg1);
										fullResponse = response;
										new Handler(Looper.getMainLooper()).post(() -> {
											Toast.makeText(getContext(), response, Toast.LENGTH_SHORT).show();
										});
										startLazyLoad(response);
									}
				
								} catch (IOException | URISyntaxException e) {
									if (e.getMessage() != null) {
										
										new Handler(Looper.getMainLooper()).post(() -> {
											Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
										});
									}
									new Handler(Looper.getMainLooper()).post(() -> {
											Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
										});
								}
								;
							}*/

				try {
					String contentTypeHeader = arg1.header("content-type");

					StringBuilder responseBuilder = new StringBuilder();
					responseBuilder.append(buildResponseHeaderString(arg1)).append("\n\n");

					ResponseBody body = arg1.body();

					try {
						ContentType contentType = new ContentType(contentTypeHeader);
						BodyType type = Body.getHttpBodyType(contentType);
						String decodedBody = decodeBodyFromEncoding(arg1);

						responseBuilder.append(decodedBody != null ? decodedBody : "[Empty Body]");

					} catch (Exception e) {
						//	responseBuilder.append("[Body read error: ").append(e.getMessage()).append("]");
					}

					String response = responseBuilder.toString();
					fullResponse = response;

					if (arg1.code() / 100 == 3) {
						String location = arg1.header("Location");
						if (location != null) {
							URI redirectUri = new URI(location);
							if (redirectUri.getHost() == null) {
								redirectUri = new URI("https://" + host + "/" + location);
							}
							onRedirectDetected(request, redirectUri, arg1.code());
						}
					}

					startLazyLoad(response);

				} catch (URISyntaxException e) {
					new Handler(Looper.getMainLooper()).post(() -> {
						Toast.makeText(getContext(), e.toString(), Toast.LENGTH_SHORT).show();
					});
				}

			}
		});
	}

	private void handleRedirect(Request originalRequest, URI redirectUri, int statusCode) {
		// Get original host
		String originalHost = originalRequest.headers().get(HttpHeaderNames.HOST.toString());
		boolean isRelative = redirectUri.getHost() == null; // Check if it's a relative URL

		// Resolve the new host and path
		String newHost = isRelative ? originalHost : redirectUri.getHost();
		String newPath = redirectUri.getRawPath()
				+ (redirectUri.getRawQuery() != null ? "?" + redirectUri.getRawQuery() : "");

		// Determine the new request method based on the status code
		String newMethod = originalRequest.method();
		boolean shouldRemoveBodyHeaders = false;

		if (statusCode == 301 || statusCode == 302 || statusCode == 303) {
			newMethod = HttpMethod.GET.toString(); // Convert to GET for these redirect codes
			shouldRemoveBodyHeaders = true;
		}
		newMethod = HttpMethod.GET.toString(); // Convert to GET for these redirect codes
		shouldRemoveBodyHeaders = true;
		Headers.Builder headersBuilder = new Headers.Builder();

		// Copy headers, removing body-related headers if switching to GET
		for (int i = 0; i < originalRequest.headers().size(); i++) {
			String name = originalRequest.headers().name(i);
			String value = originalRequest.headers().value(i);

			if (shouldRemoveBodyHeaders && (name.equalsIgnoreCase("Content-Length")
					|| name.equalsIgnoreCase("Content-Type") || name.equalsIgnoreCase("Transfer-Encoding"))) {
				continue; // skip these if changing to GET
			}
			headersBuilder.add(name, value);
		}

		// Update the Host header to reflect the new destination
		headersBuilder.set(HttpHeaderNames.HOST.toString(), newHost);

		// Re-invoke the request with the new host
		String url = originalRequest.isHttps() ? "https" : "http" + "://" + newHost + newPath;
		Request request = new Request.Builder().url("https://" + newHost + newPath).headers(headersBuilder.build())
				.method(newMethod, null).build();

		runOnUiThread(() -> {
			try {
				binding.repeaterRequest
						.setText(Utils.highlightAndFormatHttpRequest(buildRequestString(request, protocol)));
			} catch (IOException e) {
			}
		});
		replay(request, newHost);
	}

	String decodeBodyFromEncoding(Response response) throws IOException {
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
		if (protocol != null) {
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
		} else {
			protocolReadable = "HTTP/1.1";
		}
		requestBuilder.append(request.method()).append(" ").append(url.getPath()).append(" ").append(protocolReadable)
				.append("\n");

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
		//String url="https://" + host + uri;
		String scheme = PORT == 80 ? "http" : "https";
		String url = scheme + "://" + host + uri;//"https://" + host + uri;
		return new Request.Builder().url(url).headers(headersBuilder.build()).method(method, body).build();
	}

	private String processEncodingTags(String input) {
		String output = input;

		// Process <base64> tags
		Pattern base64Pattern = Pattern.compile("<base64>(.*?)</base64>", Pattern.DOTALL);
		Matcher base64Matcher = base64Pattern.matcher(output);
		StringBuffer sb = new StringBuffer();
		while (base64Matcher.find()) {
			String original = base64Matcher.group(1);
			String encoded = Base64.encodeToString(original.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
			base64Matcher.appendReplacement(sb, Matcher.quoteReplacement(encoded));
		}
		base64Matcher.appendTail(sb);
		output = sb.toString();

		// Process <hex> tags
		Pattern hexPattern = Pattern.compile("<hex>(.*?)</hex>", Pattern.DOTALL);
		Matcher hexMatcher = hexPattern.matcher(output);
		sb = new StringBuffer();
		while (hexMatcher.find()) {
			String original = hexMatcher.group(1);
			String encoded = encodeToHex(original);
			hexMatcher.appendReplacement(sb, Matcher.quoteReplacement(encoded));
		}
		hexMatcher.appendTail(sb);
		output = sb.toString();

		return output;
	}

	private String convertRequestToString(HttpRequest request) {
		StringBuilder requestString = new StringBuilder();

		// Start line
		requestString.append(request.method()).append(" ").append(request.uri()).append(" ")
				.append(request.protocolVersion()).append("\r\n");

		// Headers
		for (Map.Entry<String, String> header : request.headers()) {
			requestString.append(header.getKey()).append(": ").append(header.getValue()).append("\r\n");
		}

		requestString.append("\r\n"); // End of headers

		return requestString.toString();
	}

	private String[] parseRequestLine(String requestLine) {
		String[] parts = requestLine.split(" ");
		if (parts.length != 3 || !parts[2].startsWith("HTTP/")) {
			throw new IllegalArgumentException("Invalid request line: " + requestLine);
		}
		return parts;
	}

	private Map<String, String> parseHeaders(String[] lines) {
		Map<String, String> headers = new LinkedHashMap<>();
		int i = 1;
		while (i < lines.length) {
			String line = lines[i].trim();
			if (line.isEmpty()) {
				break; // End of headers
			}

			String[] headerParts = line.split(":", 2);
			if (headerParts.length == 2) {
				headers.put(headerParts[0].trim(), headerParts[1].trim());
			} else {
				throw new IllegalArgumentException("Invalid header format: " + line);
			}
			i++;
		}
		return headers;
	}

	private String extractBody(String[] lines) {
		StringBuilder bodyBuilder = new StringBuilder();
		boolean bodyStarted = false;
		for (int i = lines.length - 1; i >= 0; i--) {
			if (lines[i].trim().isEmpty()) {
				bodyStarted = true;
			} else if (bodyStarted) {
				bodyBuilder.insert(0, lines[i] + "\n");
			}
		}
		return bodyBuilder.toString().trim();
	}

	private void searchInFullText(String searchTerm, String fullText) {
		matchPositions.clear(); // Clear previous matches  
		currentMatchIndex = -1; // Reset the current match index  

		// Find all matches and store their positions  
		int index = fullText.indexOf(searchTerm);
		while (index != -1) {
			matchPositions.add(new Pair<>(index, index + searchTerm.length()));
			index = fullText.indexOf(searchTerm, index + searchTerm.length());
		}

		// Highlight the first match if any matches are found  
		if (!matchPositions.isEmpty()) {
			currentMatchIndex = 0;
			runOnUiThread(() -> {

				highlightMatch(currentMatchIndex, fullText);
			});
		}

		if (matchPositions.isEmpty()) {
			showToast("No matches");
		}

	}

	private void goToNextMatch(String fullText) {
		if (matchPositions.isEmpty())
			return;

		currentMatchIndex++;

		// Wrap around to the first match if at the end  
		if (currentMatchIndex >= matchPositions.size()) {
			currentMatchIndex = 0;
		}

		// Load the chunk containing the match if it's not already loaded  
		loadChunkForMatch(currentMatchIndex, fullText);
	}

	private void goToPreviousMatch(String fullText) {
		if (matchPositions.isEmpty())
			return;
		currentMatchIndex--;

		// Wrap around to the last match if at the beginning  
		if (currentMatchIndex < 0) {
			currentMatchIndex = matchPositions.size() - 1;
		}
		// Load the chunk containing the match if it's not already loaded  
		loadChunkForMatch(currentMatchIndex, fullText);
	}

	private void loadChunkForMatch(int matchIndex, String fullText) {
		if (matchIndex < 0 || matchIndex >= matchPositions.size())
			return; // Invalid index  

		// Get the start and end indices of the match  
		int start = matchPositions.get(matchIndex).first;
		int end = matchPositions.get(matchIndex).second;

		// Calculate the chunk index for the match  
		int chunkIndex = start / DEFAULT_CHUNK_SIZE;

		// Load the chunk if it's not already loaded  
		if (chunkIndex != currentChunkIndex) {
			currentChunkIndex = chunkIndex;
			binding.repeaterResponse.setText(""); // Clear the text view  
			loadNextChunk(); // Load the chunk containing the match  
		}

		// Highlight the match  
		highlightMatch(matchIndex, fullText);
	}

	private void highlightMatch(int matchIndex, String fullText) {
		if (matchIndex < 0 || matchIndex >= matchPositions.size())
			return; // Invalid index  

		// Get the start and end indices of the match  
		int start = matchPositions.get(matchIndex).first;
		int end = matchPositions.get(matchIndex).second;

		// Create a SpannableStringBuilder for the full text  
		SpannableStringBuilder spannableBuilder = new SpannableStringBuilder(fullText);

		// Highlight the current match  
		spannableBuilder.setSpan(new BackgroundColorSpan(getResources().getColor(android.R.color.holo_orange_light)),
				start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

		// Update the TextView  
		binding.repeaterResponse.setText(spannableBuilder);

		// Scroll to the match  
		scrollToMatch(start);
	}

	private void scrollToMatch(int matchStart) {

		Layout layout = binding.repeaterResponse.getLayout();
		if (layout != null) {
			int line = layout.getLineForOffset(matchStart);
			int y = layout.getLineTop(line);

			// Add an offset to scroll slightly before the match  
			int scrollOffset = Math.max(y - 200, 0); // Ensure we don't scroll past the top  
			binding.repeaterScrollView.post(() -> binding.repeaterScrollView.smoothScrollTo(0, scrollOffset));
		}
	}

	public void startLazyLoad(String fullResponse) {
		if (fullResponse == null || fullResponse.isEmpty()) {
			binding.repeaterResponse.setText(""); // Clear the text view  
			return;
		}

		// Split the response into chunks on a background thread  
		new Thread(() -> {
			List<String> chunks = splitIntoChunks(fullResponse, DEFAULT_CHUNK_SIZE);

			// Update UI on the main thread  
			runOnUiThread(() -> {
				textChunks = chunks;
				currentChunkIndex = 0;
				binding.repeaterResponse.setText(""); // Clear the text view  

				loadNextChunk(); // Load the first chunk  

				// Set scroll listener  
				scrollListener = () -> {
					if (binding.repeaterScrollView.getChildAt(0).getBottom() <= (binding.repeaterScrollView.getHeight()
							+ binding.repeaterScrollView.getScrollY()))
						// User reached the bottom  
						loadNextChunk();
				};
				binding.repeaterScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
			});
		}).start();
	}

	private List<String> splitIntoChunks(String text, int chunkSize) {
		List<String> chunks = new ArrayList<>();
		if (text == null || text.isEmpty() || chunkSize <= 0) {
			return chunks;
		}

		int length = text.length();
		for (int i = 0; i < length; i += chunkSize) {
			chunks.add(text.substring(i, Math.min(length, i + chunkSize)));
		}
		return chunks;
	}

	private void runOnUiThread(Runnable action) {
		new Handler(Looper.getMainLooper()).post(action);
	}

	private void loadNextChunk() {
		if (currentChunkIndex < textChunks.size()) {
			String chunk = textChunks.get(currentChunkIndex);
			SpannableStringBuilder highlightedChunk = new SpannableStringBuilder(chunk);
			if (currentChunkIndex == 0)
				highlightedChunk = Utils.highlightAndFormatHttpRequest(chunk);
			//	highlightedChunk = new SpannableStringBuilder(highlightedChunk);  

			// Check if any matches fall within this chunk  
			int chunkStart = currentChunkIndex * DEFAULT_CHUNK_SIZE;
			int chunkEnd = chunkStart + chunk.length();

			for (Pair<Integer, Integer> match : matchPositions) {
				int matchStart = match.first;
				int matchEnd = match.second;

				if (matchStart >= chunkStart && matchEnd <= chunkEnd) {
					// Adjust match positions relative to the chunk  
					int relativeStart = matchStart - chunkStart;
					int relativeEnd = matchEnd - chunkStart;

					// Highlight the match  
					highlightedChunk.setSpan(
							new BackgroundColorSpan(
									ContextCompat.getColor(getContext(), android.R.color.holo_orange_light)),
							relativeStart, relativeEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
				}
			}

			// Append the chunk to the TextView  
			binding.repeaterResponse.append(highlightedChunk);
			currentChunkIndex++;
		}
	}

	@Override
	public void onRedirectDetected(Request originalRequest, URI redirectUri, int code) {
		runOnUiThread(() -> {
			binding.repeaterButtonFollowRedirect.setVisibility(View.VISIBLE);
			binding.repeaterButtonFollowRedirect.setOnClickListener(v -> {
				handleRedirect(originalRequest, redirectUri, code);
				binding.repeaterButtonFollowRedirect.setVisibility(View.GONE);
			});
		});
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
		}

		if (binding.repeaterWebview != null) {
			binding.repeaterWebview.loadUrl("about:blank");
			binding.repeaterWebview.stopLoading();

			binding.repeaterWebview.setWebViewClient(null);
			binding.repeaterWebview.destroy();

		}

		binding = null;

		cachedPopupMenu = null;
		matchPositions.clear();
		textChunks = null;
		fullResponse = null;

	}

	private void showAlert() {

		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext()); // Apply theme here

		final String[] items = { "Change Content-Type", "Change method", "Encode/Decode" };

		builder.setItems(items, (dialog, which) -> {
			// which: the position of the selected item
			String selectedOption = items[which];
			switch (selectedOption) {
			case "Change Content-Type":
				showContentTypeDialog();
				break;
			case "Change method":
				showMethodDialog();
				break;
			case "Encode/Decode":
				showEncodingDialog();
				break;

			default:
				showToast("Selected: " + selectedOption);
				break;
			}

			dialog.dismiss(); // Dismiss the dialog after selection
		});

		AlertDialog dialog = builder.create();
		dialog.getWindow().setDimAmount(0.2f);
		dialog.show();
	}

	private void showEncodingDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle("Select Encoding/Decoding Method");

		final String[] methods = { "Base64 Encode", "Set Base64 Positions", "Base64 Decode", "Hex Encode",
				"Set Hex Positions", "Hex Decode", "Url encode", "Url decode" };

		builder.setItems(methods, (dialog, which) -> {
			String selectedMethod = methods[which];

			int start = binding.repeaterRequest.getSelectionStart();
			int end = binding.repeaterRequest.getSelectionEnd();

			if (start == end) {
				Toast.makeText(requireContext(), "No text selected", Toast.LENGTH_SHORT).show();
				return;
			}

			String selectedText = binding.repeaterRequest.getText().toString().substring(start, end);
			Editable editable = binding.repeaterRequest.getText();

			// Handle "Set Position" methods separately
			if (selectedMethod.equals("Set Base64 Positions")) {
				editable.replace(start, end, "<base64>" + selectedText + "</base64>");
			} else if (selectedMethod.equals("Set Hex Positions")) {
				editable.replace(start, end, "<hex>" + selectedText + "</hex>");
			} else {
				// For encode/decode methods
				String result = performEncodingDecoding(selectedText, selectedMethod);
				if (result != null) {
					editable.replace(start, end, result);
				}
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private String performEncodingDecoding(String text, String method) {
		try {
			switch (method) {
			case "Base64 Encode":
				return Base64.encodeToString(text.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP); // Use NO_WRAP to avoid line breaks
			case "Base64 Decode":
				return new String(Base64.decode(text, Base64.NO_WRAP), StandardCharsets.UTF_8);
			case "Hex Encode":
				return encodeToHex(text); // Use your hex encoding function
			case "Hex Decode":
				return decodeFromHex(text); // Implement hex decoding 
			case "Url encode":
				return encodeURL(text);
			case "Url decode":
				return decodeURL(text);
			default:
				return null; // Or throw an exception for unknown method
			}
		} catch (IllegalArgumentException | IllegalStateException ex) { // Catch Base64 decoding errors
			Toast.makeText(requireContext(), "Encoding/Decoding error: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
			return null; // Indicate error
		}
	}

	// Your hex encoding function (from previous example)
	private String encodeToHex(String input) {
		StringBuilder sb = new StringBuilder();
		for (char c : input.toCharArray()) {
			sb.append("&#x");
			sb.append(String.format("%04X", (int) c)); // Convert to hex with leading zeros
			sb.append(";");
		}
		return sb.toString();
	}

	// Implement Hex Decoding function
	private String decodeFromHex(String input) {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		while (i < input.length()) {
			if (input.startsWith("&#x", i)) {
				int semicolonIndex = input.indexOf(';', i);
				if (semicolonIndex > i + 3) {
					String hex = input.substring(i + 3, semicolonIndex);
					try {
						int codePoint = Integer.parseInt(hex, 16);
						sb.append((char) codePoint);
						i = semicolonIndex + 1;
						continue;
					} catch (NumberFormatException e) {
						// malformed, fall through
					}
				}
			}
			// append character as-is if not a hex entity
			sb.append(input.charAt(i));
			i++;
		}
		return sb.toString();
	}

	public static String encodeURL(String input) {
		try {
			return URLEncoder.encode(input, StandardCharsets.UTF_8.toString()); // Use UTF-8 for consistency
		} catch (UnsupportedEncodingException e) {
			// Handle the exception (shouldn't happen with UTF-8)
			e.printStackTrace(); // Or log the error
			return null; // Or throw a RuntimeException if you prefer
		}
	}

	public static String decodeURL(String input) {
		try {
			return URLDecoder.decode(input, StandardCharsets.UTF_8.toString()); // Use UTF-8 for consistency
		} catch (UnsupportedEncodingException e) {
			// Handle the exception
			e.printStackTrace(); // Or log the error
			return null; // Or throw a RuntimeException
		} catch (IllegalArgumentException e) {
			// Handle the case of invalid URL encoding (e.g., % without following characters)
			e.printStackTrace(); // Or log the error
			return null;
		}
	}

	private String changeContentType(String requestText, String newContentType) {
		Pattern contentTypePattern = Pattern.compile("(?i)(^Content-Type:\\s*)(.+)$", Pattern.MULTILINE);
		Matcher matcher = contentTypePattern.matcher(requestText);

		if (matcher.find()) {
			// Replace only the value of Content-Type without changing the format
			return matcher.replaceFirst(matcher.group(1) + newContentType);
		}

		return requestText; // Return unchanged if Content-Type is not found
	}

	private void showContentTypeDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle("Select or Enter Content-Type");

		// Predefined list of Content-Types
		final String[] contentTypes = { "application/json", "application/x-www-form-urlencoded", "text/plain",
				"text/html", "multipart/form-data", "application/xml", "Custom..." // Last option allows custom input
		};

		builder.setItems(contentTypes, (dialog, which) -> {
			String selectedType = contentTypes[which];

			if (selectedType.equals("Custom...")) {
				// Open input dialog for custom Content-Type
				showCustomContentTypeDialog();
			} else {
				// Update Content-Type with the selected value
				updateContentType(selectedType);
			}
		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void showCustomContentTypeDialog() {
		AlertDialog.Builder inputDialog = new AlertDialog.Builder(requireContext());
		inputDialog.setTitle("Enter Custom Content-Type");

		final EditText input = new EditText(requireContext());
		input.setHint("e.g., application/vnd.api+json");
		inputDialog.setView(input);

		inputDialog.setPositiveButton("OK", (dialog, which) -> {
			String newContentType = input.getText().toString().trim();
			if (!newContentType.isEmpty()) {
				updateContentType(newContentType);
			} else {
				showToast("Content-Type cannot be empty!");
			}
		});

		inputDialog.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		inputDialog.show();
	}

	private void showMethodDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
		builder.setTitle("Select HTTP Method");

		// List of common HTTP methods
		final String[] methods = { "GET", "POST", "PUT", "PATCH", "DELETE" };

		builder.setItems(methods, (dialog, which) -> {
			String selectedMethod = methods[which];

			updateHttpMethod(selectedMethod); // Apply selected method

		});

		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss());

		AlertDialog dialog = builder.create();
		dialog.show();
	}

	private void updateHttpMethod(String newMethod) {
		String requestText = binding.repeaterRequest.getText().toString();

		// Change only the first word (method) in the request line
		String[] lines = requestText.split("\n", 2);
		if (lines.length > 1) {
			lines[0] = newMethod + lines[0].substring(lines[0].indexOf(" ")); // Replace method
			requestText = String.join("\n", lines);
		}

		// If method is POST or PATCH, ensure Content-Type is present
		if (newMethod.equals("POST") || newMethod.equals("PATCH")) {
			requestText = ensureContentType(requestText);
		}
		if (newMethod.equals("GET") || newMethod.equals("DELETE")) {
			requestText = removeBodyAndContentLength(requestText);
		}

		binding.repeaterRequest.setText(Utils.highlightAndFormatHttpRequest(requestText));
	}

	private String removeBodyAndContentLength(String requestText) {
		String[] parts = requestText.split("\n\n", 2); // Split headers and body
		String headers = parts[0];

		// Remove Content-Length header (case-insensitive)
		StringBuilder cleanedHeaders = new StringBuilder();
		String[] headerLines = headers.split("\n");
		for (String line : headerLines) {
			if (!line.toLowerCase().startsWith("content-length:")) {
				cleanedHeaders.append(line).append("\n");
			}
		}

		return cleanedHeaders.toString().trim(); // No body, return headers only
	}

	private String ensureContentType(String requestText) {
		Pattern contentTypePattern = Pattern.compile("(?i)^Content-Type: (.+)$", Pattern.MULTILINE);
		Matcher matcher = contentTypePattern.matcher(requestText);

		if (matcher.find()) {
			// Replace existing Content-Type
			return matcher.replaceFirst("Content-Type: application/json");
		} else {
			// Add Content-Type at the correct position
			int headersEnd = requestText.indexOf("\n\n"); // Find header-body separator
			if (headersEnd == -1)
				headersEnd = requestText.length(); // No body, add at the end

			return requestText.substring(0, headersEnd) + "\nContent-Type: application/json"
					+ requestText.substring(headersEnd);
		}
	}

	private void updateContentType(String newContentType) {
		String updatedRequest = changeContentType(binding.repeaterRequest.getText().toString(), newContentType);
		binding.repeaterRequest.setText(Utils.highlightAndFormatHttpRequest(updatedRequest));
	}

	protected class MatchPair {
		int chunkIndex;
		int start;
		int end;

		public MatchPair(int chunkIndex, int start, int end) {
			this.chunkIndex = chunkIndex;
			this.start = start;
			this.end = end;
		}

		public int getChunkIndex() {
			return chunkIndex;
		}

		public int getEndIndex() {
			return end;
		}

		public int getStartIndex() {
			return start;
		}
	}

}
