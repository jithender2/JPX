package com.proxy.ui.Fragments;

import android.graphics.Color;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.EditText;
import android.text.Spannable;
import android.os.Bundle;
import android.text.style.ForegroundColorSpan;
import android.text.SpannableStringBuilder;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.JsonBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.FormEncodedBeautifier;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.proxy.beautifier.Beautifier;
import com.proxy.data.ContentType;
import com.proxy.data.Http2Message;
import com.proxy.data.Http1Message;
import com.proxy.data.HttpHeaders;
import com.proxy.data.HttpMessage;
import com.proxy.databinding.BottomSheetFilterBinding;
import com.proxy.databinding.HttpHistoryBinding;
import com.proxy.listener.SetLogger;
import com.proxy.store.Body;
import com.proxy.utils.Utils;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import androidx.recyclerview.widget.SimpleItemAnimator;
import java.nio.charset.Charset;
import io.netty.util.CharsetUtil;
import com.proxy.utils.BodyType;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import com.proxy.R;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View;

import java.util.Collections;
import com.proxy.ui.Adapter.HttpRecyclerAdapter;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import com.proxy.data.Message;
import java.util.ArrayList;
import com.proxy.ViewModel.SharedViewModel;
import androidx.fragment.app.Fragment;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class HttpHistory extends Fragment {

	private HttpHistoryBinding binding; // ViewBinding reference
	String[] sortOptions = { "Status Code", "Length", "Method", "Mime Type", "Status Code (DESC)", "Length (DESC)",
			"Method (DESC)", "Mime Type (DESC)" };
	String sortType;
	SharedViewModel sharedViewModel;
	HttpRecyclerAdapter adapter;
	HttpMessage httpMessage;
	int start = 0;
	int currentType = 0;
	PopupMenu cachedPopupMenu;
	List<Message> allMessages = new ArrayList<>();
	List<Message> filteredResults = new ArrayList<>();
	

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		binding = HttpHistoryBinding.inflate(inflater, container, false);
		sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
		initializeViews();
		clickListeners();
		setupSpinner();

		adapter.setOnItemClickListener(position -> {
			Message message = adapter.getMessages().get(position);
			if (message instanceof HttpMessage) {
				httpMessage = (HttpMessage) message;
				if (binding.httpHistoryFramelayoutBottom.getVisibility() == View.GONE) {
					binding.httpHistoryFramelayoutBottom.setVisibility(View.VISIBLE);
				}
				setHttpMessage(httpMessage, 0);
			}
		});

		showMessagesToUi();
		binding.httpHistoryFilter.setOnClickListener(v -> {
			showBottomSheet();
		});
		return binding.getRoot();
	}

	

	private void showMessagesToUi() {
		if(sharedViewModel!=null){
		sharedViewModel.getMainRequests().observe(getViewLifecycleOwner(), messages -> {
			List<Message> safeMessages = new ArrayList<>(messages);
			/*	for (Message message : safeMessages)
					adapter.addMessage(message);
			});*/

			if (start < safeMessages.size()) {
				for (int i = start; i < safeMessages.size(); i++) {
					start++;
					if (safeMessages.get(i) instanceof HttpMessage) {
						adapter.addMessage(safeMessages.get(i));

					}
				}
			}
		});
}
	}

	public void applyScope() {
		if (adapter == null)
			return;
		if (adapter.isScopeEmpty()) {
			Toast.makeText(getContext(), "Scope is empty", Toast.LENGTH_SHORT).show();
			return;
		}
		adapter.applyScope(true);
	}

	public void clearScope() {
		if (adapter != null)
			adapter.clearScope();
	}

	public boolean isScopeApplied() {
		return adapter != null && adapter.isScopeApplied();
	}

	public void addToScope(String domain) {
		if (adapter != null)
			adapter.addToScope(domain);
	}

	private void clickListeners() {
		setupButtonListeners();
	}

	private void setHttpMessage(HttpMessage message, int type) {
		StringBuilder httpMessageBuilder = new StringBuilder();
		if (type == Utils.REQUEST) {
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
			httpMessageBuilder.append(headers).append("\n").append(body);

		} else if (type == Utils.RESPONSE) {
			HttpHeaders httpHeaders = message.responseHeader();
			if (httpHeaders != null) {
				String httpResponse = String.join("\n", httpHeaders.rawLines()) + "\n\n"
						+ Utils.getBody(message.responseBody());
				httpMessageBuilder.append(httpResponse);
			} else {
				httpMessageBuilder.append("empty response");
			}
		}

		binding.historyHttpmessageView.setText(Utils.highlightAndFormatHttpRequest(httpMessageBuilder.toString()));
	}

	private void initializeViews() {
		adapter = new HttpRecyclerAdapter(requireContext());
		binding.httpHistoryRecyclerview.setLayoutManager(new LinearLayoutManager(requireContext()));
		binding.httpHistoryRecyclerview.setAdapter(adapter);
	}

	private void toggleFrameLayoutVisibility(FrameLayout frameLayout) {
		frameLayout.setVisibility(frameLayout.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
	}

	private void setupButtonListeners() {
		binding.historyShowFullScreen
				.setOnClickListener(v -> toggleFrameLayoutVisibility(binding.httpHistoryFramelayoutTop));
		binding.historyMore.setOnClickListener(v -> ShowPopUpMenu());
		binding.historyCancel.setOnClickListener(v -> {
			if (binding.httpHistoryFramelayoutTop.getVisibility() == View.GONE) {
				binding.httpHistoryFramelayoutTop.setVisibility(View.VISIBLE);
			}
			binding.httpHistoryFramelayoutBottom.setVisibility(View.GONE);
		});
		binding.httpHistoryRefresh.setOnClickListener(v -> {
			adapter.refresh();
		});
		//	sortByMethodClick();
		//sortByStatusClick();
	}

	private void ShowPopUpMenu() {
		if (cachedPopupMenu == null) {
			cachedPopupMenu = new PopupMenu(getActivity(), binding.historyMore);
			cachedPopupMenu.getMenuInflater().inflate(R.menu.menu, cachedPopupMenu.getMenu());
			cachedPopupMenu.setOnMenuItemClickListener(item -> {
				if (item.getItemId() == R.id.send_to_repeator) {
					sharedViewModel.addToRepeater(httpMessage);
				}
				return true;
			});
		}
		cachedPopupMenu.show();
	}

	private void setupSpinner() {
		initializeSpinnerOptions();
		setupSpinnerListener();
	}

	private void initializeSpinnerOptions() {
		String[] options = getResources().getStringArray(R.array.spinner_options);
		ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(getContext(), R.layout.spinner_text, options);
		spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_item);
		binding.historySpinner.setAdapter(spinnerAdapter);
	}

	private void setupSpinnerListener() {
		binding.historySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				handleSpinnerSelection(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
			}
		});
	}

	private void handleSpinnerSelection(int selectedPosition) {
		if (httpMessage != null && currentType != selectedPosition) {
			currentType = selectedPosition;
			setHttpMessage(httpMessage, currentType);
		}
	}

	private void showBottomSheet() {
		BottomSheetFilterBinding binding = BottomSheetFilterBinding.inflate(LayoutInflater.from(requireContext()));
		View view = binding.getRoot();
		BottomSheetDialog dialog = new BottomSheetDialog(requireContext());
		dialog.setContentView(view);
		dialog.show();

		// Initialize sorting dropdown

		ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line,
				sortOptions);
		binding.sortDropdown.setAdapter(adapter);
		if (sortType != null)
			binding.sortDropdown.setText(sortType, false);
		else
			binding.sortDropdown.setText(sortOptions[0], false); // Set default sorting option
		binding.bottomSheetResponseTimeLayout.setVisibility(View.GONE);
		binding.bottomSheetResponseTimePlaceholder.setVisibility(View.GONE);
		//binding.bottomSheetResponseMustPlaceHolder.setVisibility(View.GONE);
		binding.lengthRangeSlider.setVisibility(View.GONE);
		//	binding.containsTextEditText.setVisibility(View.GONE);
		float minValue = 0f; // Use 0 or your actual minimum
		float maxValue = 10000f; // Your maximum value
		binding.lengthRangeSlider.setValueFrom(minValue);
		binding.lengthRangeSlider.setValueTo(maxValue);
		binding.lengthRangeSlider.setValues(minValue, maxValue); // Set initial slider values

		// Apply Filters Button Click Listener
		binding.applyFilterButton.setOnClickListener(v -> {
			// Get selected status codes from ChipGroup
			Set<Integer> selectedCodes = new HashSet<>();
			for (int i = 0; i < binding.chipGroupStatus.getChildCount(); i++) {
				Chip chip = (Chip) binding.chipGroupStatus.getChildAt(i);
				if (chip.isChecked()) {
					selectedCodes.add(Integer.parseInt(chip.getText().toString()));
				}
			}

			// Get selected range values from the slider
			List<Float> sliderValues = binding.lengthRangeSlider.getValues();
			float minLen = sliderValues.get(0);
			float maxLen = sliderValues.get(1);

			// Get text to search in the response body
			//	String containsText = binding.containsTextEditText.getText().toString().trim();

			// Get sorting type
			sortType = binding.sortDropdown.getText().toString().trim();

			applyFilters(allMessages, selectedCodes, 0, Integer.MAX_VALUE, "", sortType);
			// Dismiss the bottom sheet after applying filters 

			dialog.dismiss();
		});
	}

	public void applyFilters(List<Message> originalMessages, Set<Integer> selectedCodes, int minLen, int maxLen,
			String containsText, String sortType) {

		List<Message> filteredMessages = new ArrayList<>();

		if (!selectedCodes.isEmpty()) {
			adapter.filterByStatusCodes(selectedCodes);
		}

		switch (sortType.trim()) {
		case "Status Code":
			adapter.SortAsc();
			adapter.sortByStatus();
			break;
		case "Length":
			adapter.SortAsc();
			adapter.sortByLength();
			break;
		case "Method":
			adapter.SortAsc();
			adapter.sortByMethod();
			break;
		case "MimeType":
			adapter.SortAsc();
			adapter.sortByMimeType();
			break;
		case "Status Code (DESC)":
			adapter.SortDesc();
			adapter.sortByStatus();
			break;
		case "Length (DESC)":
			adapter.SortDesc();
			adapter.sortByLength();
			break;
		case "Method (DESC)":
			adapter.SortDesc();
			adapter.sortByMethod();
			break;
		case "MimeType (DESC)":
			adapter.SortDesc();
			adapter.sortByMimeType();
			break;
		default:

			adapter.sortByMimeType();
		}

	}

	// Modified to accept the list to sort
	public List<Message> sortByMethod(List<Message> messagesToSort) {
		List<Message> sortedList = new ArrayList<>(messagesToSort);
		new Thread(() -> {

			// Define custom method order
			List<String> methodPriority = List.of("GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS");

			sortedList.sort((m1, m2) -> {
				String method1 = getRequestMethod(m1);
				String method2 = getRequestMethod(m2);

				int index1 = method1 != null ? methodPriority.indexOf(method1.toUpperCase()) : Integer.MAX_VALUE;
				int index2 = method2 != null ? methodPriority.indexOf(method2.toUpperCase()) : Integer.MAX_VALUE;

				// If method not found in priority list, treat it as last in order
				if (index1 == -1)
					index1 = Integer.MAX_VALUE;
				if (index2 == -1)
					index2 = Integer.MAX_VALUE;

				if (index1 == index2) {
					return Comparator.nullsLast(String::compareToIgnoreCase).compare(method1, method2);
				}

				return Integer.compare(index1, index2);
			});
		});

		return sortedList;
	}

	private String getRequestMethod(Message message) {
		if (message instanceof Http1Message) {
			Http1Message httpMessage = (Http1Message) message;
			if (httpMessage.requestHeader() != null) {
				return httpMessage.requestHeader().method();
			}
		} else if (message instanceof Http2Message) {
			Http2Message httpMessage = (Http2Message) message;
			if (httpMessage.requestHeader() != null) {
				return httpMessage.requestHeader().method();
			}
		}
		return null;
	}

	private String getHost(Message message) {
		if (message instanceof Http1Message) {
			Http1Message http1Message = (Http1Message) message;
			return http1Message.host();
		} else if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			return http2Message.host();
		}
		return "";
	}

	private int getContentLength(Message message) {

		if (message instanceof Http1Message) {
			Http1Message http1Message = (Http1Message) message;
			return (int) http1Message.length();
		} else if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			return (int) http2Message.length();
		}
		return 0;
	}

	private int getStatusCode(Message message) {
		if (message instanceof Http1Message) {
			Http1Message httpMessage = (Http1Message) message;
			return httpMessage.code();
		} else if (message instanceof Http2Message) {
			Http2Message httpMessage = (Http2Message) message;
			return httpMessage.code();
		}
		return 0; // Default if message type is unknown
	}

	private String getMimeType(Message message) {
		if (message instanceof Http1Message) {
			Http1Message http1Message = (Http1Message) message;
			return http1Message.MimeType();
		} else if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			return http2Message.MimeType();
		}
		return "";
	}

	private String getMimeTypeFromHttp2(Http2Message httpMessage) {
		try {
			if (httpMessage.responseHeader() != null) {
				Optional<String> contentTypeHeader = httpMessage.responseHeader().getHeader(Utils.CONTENT_TYPE_HEADER);
				if (contentTypeHeader.isPresent()) {
					String fullType = contentTypeHeader.get(); // e.g., "text/html; charset=utf-8"
					return fullType.split(";")[0].trim(); // Only "text/html"
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String getMimeTypeFromHttp1(Http1Message httpMessage) {
		try {
			if (httpMessage.responseHeader() != null && httpMessage.responseHeader().contentType().isPresent()) {
				ContentType contentType = httpMessage.responseHeader().contentType().get();
				return contentType.mimeType().toString(); // Returns like "text/html"
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	private String safeText(String value) {
		return (value != null && !value.isEmpty()) ? value : "-";
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		cleanupMemory();
	}

	private void cleanupMemory() {
		// Nullify ViewBinding to release all view references
		binding = null;

		// Clear adapter to help GC
		if (adapter != null) {
			adapter = null;
		}

		// Clear popup menu
		cachedPopupMenu = null;

		// Clear message lists
		if (allMessages != null) {
			allMessages.clear();
			allMessages = null;
		}

		if (filteredResults != null) {
			filteredResults.clear();
			filteredResults = null;
		}

		// Nullify HttpMessage object
		httpMessage = null;

		// Nullify ViewModel reference (if it’s not shared outside this Activity)
		sharedViewModel = null;

		// Nullify sortType if needed
		sortType = null;
	}

	public class RequestMessage {
		long id;
		Message message;

		public RequestMessage(Message message) {
			this.message = message;
			id++;
		}

	}
}
