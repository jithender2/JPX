package com.proxy.ui.Fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import com.proxy.AppContext;
import com.proxy.InitContext;
import com.proxy.MainActivity;
import com.proxy.R;
import android.view.View;
import android.view.LayoutInflater;
import com.proxy.ViewModel.SharedViewModel;
import com.proxy.beautifier.Beautifier;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.JsonBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.FormEncodedBeautifier;
import com.proxy.data.Http2Message;
import com.proxy.data.HttpHeaders;
import com.proxy.data.HttpMessage;
import com.proxy.data.HttpRequestHeaders;
import com.proxy.data.Message;
import com.proxy.data.WebSocketMessage;
import com.proxy.databinding.TargetBinding;
import com.proxy.listener.OnDataChange;

import com.proxy.listener.SetLogger;
import com.proxy.netty.Server;
import androidx.fragment.app.Fragment;
import com.proxy.netty.codec.frame.Http2StreamEvent;
import com.proxy.store.Body;
import com.proxy.ui.FuzzingActivity;
import com.proxy.ui.Adapter.CustomTreeAdapter;
import com.proxy.ui.MainRequestRepository;
import com.proxy.utils.Networks;
import com.proxy.utils.Utils;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import com.proxy.utils.BodyType;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import org.bouncycastle.util.encoders.UTF8;
import java.lang.RuntimeException;

public class Target extends Fragment {
	TargetBinding binding;

	CustomTreeAdapter adapter;

	HttpMessage httpMessage;

	SharedViewModel sharedViewModel;
	int currentType = 0;
	private int currentChunkIndex;
	private OnDataChange exportDataListener;
	private final Set<String> processedMessages = new HashSet<>();
	PopupMenu cachedPopupMenu;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();

	public Target(OnDataChange listener) {
		exportDataListener = listener;
	}

	private List<String> textChunks;
	int start = 0;
	private static final int DEFAULT_CHUNK_SIZE = 8000, DEFAULT_PORT = 443;
	private boolean applyScope = false;
	Server server;

	public Target() {
	}

	@Override
	public View onCreateView(LayoutInflater arg0, ViewGroup arg1, Bundle arg2) {
		binding = TargetBinding.inflate(arg0, arg1, false);
		View view = binding.getRoot();
		initialize();
		AppContext context = new AppContext(requireContext());
		clickListeners();
		setupSpinner();
		showMessagesToUi();
		//	start(context);
		InitContext initContext = new InitContext(requireContext());
		initContext.init();
		setupLogCallBack();
		return view;
	}

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
	}

	private void start(AppContext appContext) {
		executorService.submit(() -> {
			server = new Server(appContext.getServerSetting(), appContext.getSslContextManager(),
					appContext.getProxySetting(), message -> {
						AddToViewModel(message);
						requireActivity().runOnUiThread(() -> {
							adapter.addNewMessage(message);
						});
					});
			try {
				server.start();
			} catch (Exception e) {

			}
		});
	}

	private void initialize() {
		//	initializeViews();
		setupExpandableListView();
	}

	private void setupExpandableListView() {
		adapter = new CustomTreeAdapter(requireContext());
		binding.expandableListView.setAdapter(adapter);
	}

	private void clickListeners() {
		setupExpandableListViewClickListener();
		setupLogCallBack();
		setupButtonListeners();
	}

	private void setupExpandableListViewClickListener() {
		binding.expandableListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View view, int groupPosition, int childPosition,
					long id) {
				Object child = adapter.getChild(groupPosition, childPosition);
				if (child instanceof HttpMessage) {
					httpMessage = (HttpMessage) child;
					if (binding.targetFramelayoutBottom.getVisibility() == View.GONE) {
						binding.targetFramelayoutBottom.setVisibility(View.VISIBLE);
					}
					setHttpMessage(httpMessage, 0);
				} else if (child instanceof WebSocketMessage) {
					if (binding.targetFramelayoutBottom.getVisibility() == View.GONE) {
						binding.targetFramelayoutBottom.setVisibility(View.VISIBLE);
					}
					setWebSocketMessage((WebSocketMessage) child);
					//	Toast.makeText(requireActivity(), "something went wrong", Toast.LENGTH_SHORT).show();
				}
				return false;
			}
		});
	}

	private void AddToViewModel(Message message) {
		//		sharedViewModel.addToMainRequests(message); // Non-blocking
	}

	private void showMessagesToUi() {
		if (sharedViewModel != null) {
			sharedViewModel.getMainRequests().observe(getViewLifecycleOwner(), messages -> {
				List<Message> safeMessages = new ArrayList<>(messages);
				/*	for (Message message : safeMessages)
						adapter.addMessage(message);
				});*/

				if (start < safeMessages.size()) {
					for (int i = start; i < safeMessages.size(); i++) {
						start++;
						if (safeMessages.get(i) instanceof HttpMessage) {
							adapter.addNewMessage(safeMessages.get(i));

						}
					}
				}
			});
		}
	}

	private void setHttpMessage(HttpMessage message, int type) {
		StringBuilder httpMessageBuilder = new StringBuilder();
		if (type == 0) {
			HttpHeaders httpHeaders = message.requestHeader();
			String body = "";
			String headers = String.join("\n", httpHeaders.rawLines());
			if (message.requestBody().finished()) {
				body = Utils.getBody(message.requestBody());
				if (body == null)
					body = "";
			}
			if (headers.startsWith(":"))
				headers = Utils.convertHttp2ToHttp1(headers);
			httpMessageBuilder.append(headers).append("\n").append(body == null ? "" : body);
			binding.targetHttpmessageView.setText(Utils.highlightAndFormatHttpRequest(httpMessageBuilder.toString()));
		} else if (type == 1) {
			HttpHeaders httpHeaders = message.responseHeader();
			if (httpHeaders != null) {
				String httpResponse = String.join("\n", httpHeaders.rawLines()) + "\n\n"
						+ Utils.getBody(message.responseBody());
				httpMessageBuilder.append(httpResponse);
			} else {
				httpMessageBuilder.append("empty response");
			}
			startLazyLoad(httpMessageBuilder.toString());
		}

	}

	private void setWebSocketMessage(WebSocketMessage message) {

		StringBuilder builder = new StringBuilder();

		String direction = message.isRequest() ? "→ Client" : "← Server";
		String type = message.type() == WebSocketMessage.TYPE_TEXT ? "Text" : "Binary";
		String payload;
		int length = 0;

		if (message.body() != null) {
			if (message.type() == WebSocketMessage.TYPE_TEXT) {
				payload = message.body().type().name();
			} else {
				payload = "[Binary Data]";
			}
			if (payload != null)
				length = payload.length();
		} else {
			payload = "[No Body]";
		}

		builder.append(direction).append(" | ").append(type).append(" | Length: ").append(length).append("\n\n")
				.append(payload).append("\n\n");

		binding.targetHttpmessageView.setText(builder.toString());

	}

	private String buildHttpMessage(HttpMessage message, int type) {
		if (message == null) {
			return "No message available";
		}

		if (type == 0) { // Request
			return buildRequestMessage(message);
		} else if (type == 1) { // Response
			return buildResponseMessage(message);
		} else {
			return "Invalid type";
		}
	}

	private String buildRequestMessage(HttpMessage message) {
		HttpHeaders httpHeaders = message.requestHeader();
		if (httpHeaders == null) {
			return "No request headers available";
		}
		String headers = String.join("\n", httpHeaders.rawLines());

		String body = Utils.getBody(message.requestBody());
		String formattedText = headers.startsWith(":") ? Utils.convertHttp2ToHttp1(headers) : headers;

		return formattedText + "\n\n" + (body != null ? body : "");
	}

	private String buildResponseMessage(HttpMessage message) {
		HttpHeaders httpHeaders = message.responseHeader();
		if (httpHeaders == null) {
			return "No response headers available";
		}

		String body = Utils.getBody(message.responseBody());
		return String.join("\n", httpHeaders.rawLines()) + "\n\n" + (body != null ? body : "");
	}

	public void exportData() {
		if (adapter != null) {

			List<String> groups = adapter.getGroupList();
			HashMap<String, List<Message>> items = adapter.getItemList();

			if (exportDataListener != null) {
				exportDataListener.onDataExport(groups, items); // Trigger export in MainActivity
			}
		} else {
			Toast.makeText(requireActivity(), "something went wrong", Toast.LENGTH_SHORT).show();
		}
	}

	public void applyScope() {
		if (adapter == null)
			return;
		if (adapter.isScopeEmpty()) {
			Toast.makeText(requireActivity(), "scope is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		adapter.applyScope(true);
	}

	public void clearScope() {
		if (adapter != null)
			adapter.clearScope();
	}

	public boolean isScopeApplied() {
		if (adapter != null)
			return adapter.isScopeApplied();
		return false;
	}

	public void addToScope(String domain) {
		if (adapter != null)
			adapter.addToScope(domain);
	}

	private void setupButtonListeners() {
		binding.showFullScreen.setOnClickListener(v -> toggleFrameLayoutVisibility(binding.targetFramelayoutTop));
		binding.targetMore.setOnClickListener(v -> ShowPopUpMenu());
		binding.targetCancel.setOnClickListener(v -> {
			if (binding.targetFramelayoutTop.getVisibility() == View.GONE) {
				binding.targetFramelayoutTop.setVisibility(View.VISIBLE);
			}
			binding.targetFramelayoutBottom.setVisibility(View.GONE);
		});
	}

	private void toggleFrameLayoutVisibility(FrameLayout frameLayout) {
		if (frameLayout.getVisibility() == View.VISIBLE) {
			frameLayout.setVisibility(View.GONE);
		} else {
			frameLayout.setVisibility(View.VISIBLE);
		}
	}

	private void ShowPopUpMenu() {
		if (cachedPopupMenu == null) {
			cachedPopupMenu = new PopupMenu(getActivity(), binding.targetMore);
			cachedPopupMenu.getMenuInflater().inflate(R.menu.menu, cachedPopupMenu.getMenu());
			cachedPopupMenu.setOnMenuItemClickListener(item -> {
				switch (item.getItemId()) {
				case R.id.send_to_repeator:
					sharedViewModel.addToRepeater(httpMessage);
					break;
				case R.id.send_to_intruder:
					Intent intent = new Intent(requireContext(), FuzzingActivity.class);
					intent.putExtra("message", httpMessage);
					startActivity(intent);
				}
				return true;
			});
		}
		cachedPopupMenu.show();

	}

	private void setupSpinner() {
		// Initialize the Spinner with options
		initializeSpinnerOptions();

		// Set up item selection listener
		setupSpinnerListener();
	}

	/**
	 * Initializes the Spinner with options "Request" and "Response".
	 */

	private void initializeSpinnerOptions() {
		String[] options = getResources().getStringArray(R.array.spinner_options);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), R.layout.spinner_text, options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.targetSpinner.setAdapter(adapter);
	}

	private void setupSpinnerListener() {
		binding.targetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				handleSpinnerSelection(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// No-op
			}
		});
	}

	private void handleSpinnerSelection(int selectedPosition) {
		if (httpMessage != null && currentType != selectedPosition) {
			currentType = selectedPosition;
			setHttpMessage(httpMessage, currentType);
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		binding = null;
		if (server != null)
			server.stop();

		// Nullify adapter and listener references
		adapter.cleanUpMemory();
		adapter = null;
		exportDataListener = null;
		cachedPopupMenu = null;
		sharedViewModel = null;

		if (processedMessages != null) {
			processedMessages.clear();
		}
		if (textChunks != null) {
			textChunks.clear();

		}

		// Shut down executor to prevent background thread leaks
		if (executorService != null && !executorService.isShutdown()) {
			executorService.shutdownNow();
		}

	}

	public void startLazyLoad(String fullResponse) {
		if (fullResponse == null || fullResponse.isEmpty()) {
			binding.targetHttpmessageView.setText(""); // Clear the text view
			return;
		}

		// Split the response into chunks on a background thread
		executorService.submit(() -> {
			List<String> chunks = splitIntoChunks(fullResponse, DEFAULT_CHUNK_SIZE);

			// Update UI on the main thread
			requireActivity().runOnUiThread(() -> {
				textChunks = chunks;

				currentChunkIndex = 0;
				binding.targetHttpmessageView.setText(""); // Clear the text view

				loadNextChunk(); // Load the first chunk

				// Set scroll listener
				binding.targetScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
					if (binding.targetScrollView.getChildAt(0).getBottom() <= (binding.targetScrollView.getHeight()
							+ binding.targetScrollView.getScrollY())) {
						// User reached the bottom
						loadNextChunk();
					}
				});
			});
		});

	}

	private void setupLogCallBack() {
		SetLogger.setLogCallBack(new SetLogger.LogCallBack() {
			@Override
			public void onLog(String str) {
				requireActivity().runOnUiThread(() -> adapter.addLogMessage(str));
			}
		});
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

	private void loadNextChunk() {
		if (currentChunkIndex < textChunks.size()) {
			String chunk = textChunks.get(currentChunkIndex);
			SpannableStringBuilder highlightedChunk = new SpannableStringBuilder(chunk);
			if (currentChunkIndex == 0)
				highlightedChunk = Utils.highlightAndFormatHttpRequest(chunk);
			binding.targetHttpmessageView.append(highlightedChunk);

			currentChunkIndex++;

		}
	}

}