package com.proxy.ui;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.os.Bundle;
import android.text.Editable;
import android.text.Layout;
import android.text.style.BackgroundColorSpan;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.proxy.Fuzzer.Fuzzer;
import com.proxy.R;
import com.proxy.ViewModel.SharedViewModel;
import com.proxy.beautifier.Beautifier;
import com.proxy.beautifier.HtmlBeautifier;
import com.proxy.beautifier.XmlBeautifier;
import com.proxy.beautifier.JsonBeautifier;
import com.proxy.databinding.BottomSheetFilterBinding;
import com.proxy.databinding.DialogBinding;
import com.proxy.databinding.FuzzingLayoutBinding;
import com.proxy.databinding.ItemPayloadGroupBinding;
import com.proxy.store.Body;
import com.proxy.data.HttpMessage;
import com.proxy.ui.Adapter.ResultsAdapter;
import com.proxy.utils.Utils;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import com.proxy.utils.BodyType;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.nio.charset.Charset;
import java.net.URISyntaxException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FuzzingActivity extends AppCompatActivity implements Fuzzer.FuzzProgressListener {

	private Fuzzer fuzzer;
	private Map<String, List<String>> payloads = new HashMap<>();
	private ResultsAdapter resultsAdapter;
	private static final int Request_Code_Picker = 2;

	private LinearLayout containerPayloadGroups;

	private static final List<Beautifier> beautifiers = List.of(new XmlBeautifier(), new HtmlBeautifier(),
			new JsonBeautifier());
	FuzzingLayoutBinding binding;
	Fuzzer.FuzzResult fuzzResult;
	int currentType;

	private int currentMatchIndex = 0, currentChunkIndex = 0;
	String contentEncoding;
	String fullResponse;
	private List<Pair<Integer, Integer>> matchPositions = new ArrayList<>();
	private List<String> textChunks;
	// Constants
	private static final int DEFAULT_CHUNK_SIZE = 10000;

	List<Fuzzer.FuzzResult> originalResults = new ArrayList<>();
	List<Fuzzer.FuzzResult> filteredResults = new ArrayList<>();
	int total = 0;
	private ActivityResultLauncher<Intent> filePickerLauncher;
	private final Map<String, List<String>> externalPayloadCache = new HashMap<>();
	private static final Pattern FUZZ_PATTERN = Pattern.compile("\\{\\{FUZZ(\\d*)\\}\\}");

	String[] sortOptions = { "Status Code", "Length", "ResponseTime","Status Code (DESC)", "Length (DESC)", "ResponseTime (DESC)" };
	String sortType;
	private ViewTreeObserver.OnScrollChangedListener scrollListener;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = FuzzingLayoutBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		/*	fuzzer = new Fuzzer(4);
			fuzzer.setProgressListener(this);
		
			// Load sample request template
		
			// Setup results adapter
			resultsAdapter = new ResultsAdapter(this, new ArrayList<>());
			binding.recyclerResults.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		
			binding.recyclerResults.setAdapter(resultsAdapter);
		
			// Setup click listeners
			binding.btnAddPayload.setOnClickListener(v -> showAddPayloadDialog());
			binding.btnStartFuzzing.setOnClickListener(v -> startFuzzing());
			HttpMessage message = (HttpMessage) getIntent().getSerializableExtra("message");
			if (message != null)
				setHttpMessage(message);
			String url = message.url();
			binding.etTargetUrl.setText(url);
		
			// Setup item click listener for results
			resultsAdapter.setOnResultClickListener(v -> {
		
				showResultDetailsDialog(v, 1);
			});
			binding.intruderCancel.setOnClickListener(v -> {
				if (binding.layoutInputSection.getVisibility() == View.GONE) {
					binding.layoutInputSection.setVisibility(View.VISIBLE);
				}
			});
			binding.intruderResponseCancel.setOnClickListener(v -> {
				if (binding.fuzzingLayoutFramelayout.getVisibility() == View.VISIBLE) {
					binding.fuzzingLayoutFramelayout.setVisibility(View.GONE);
				}
			});
			initializeSpinnerOptions();
			setupSpinnerListener();
		
			binding.intruderNextArrow.setOnClickListener(v -> goToNextMatch(binding.intruderResponse.getText().toString()));
			binding.intruderPreviousArrow
					.setOnClickListener(v -> goToPreviousMatch(binding.intruderResponse.getText().toString()));
		
			binding.intruderSearch.setOnEditorActionListener((v, actionId, event) -> {
				if (actionId == EditorInfo.IME_ACTION_SEARCH) {
					searchInFullText(binding.intruderSearch.getText().toString(), fullResponse);
					return true;
				}
				return false;
			});
			binding.fuzzingFilter.setOnClickListener(v -> {
				showBottomSheet();
			});
		
			Spinner spinnerThreadCount = findViewById(R.id.spinner_thread_count);
		
			List<String> threadOptions = new ArrayList<>();
			threadOptions.add("Auto");
			for (int i = 1; i <= 10; i++) {
				threadOptions.add(String.valueOf(i));
			}
		
			ArrayAdapter<String> threadAdapter = new ArrayAdapter<>(this, R.layout.spinner_text, threadOptions);
			threadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			binding.spinnerThreadCount.setAdapter(threadAdapter);
		
			filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
				if (result.getResultCode() == RESULT_OK && result.getData() != null && currentPayloadGroupView != null) {
					Uri uri = result.getData().getData();
					if (uri != null) {
						Executors.newSingleThreadExecutor().execute(() -> {
							String text = readTextFromUri(uri);
							if (text != null) {
								List<String> payloadList = Arrays.asList(text.split("\\n"));
		
								runOnUiThread(() -> {
									try {
										ItemPayloadGroupBinding payloadBinding = ItemPayloadGroupBinding
												.bind(currentPayloadGroupView);
										String payloadType = payloadBinding.spinnerPayloadType.getSelectedItem().toString();
		
										// Store full content in memory
										externalPayloadCache.put(payloadType, payloadList);
		
										// Show preview (e.g., first 20 lines)
										StringBuilder previewBuilder = new StringBuilder();
										for (int i = 0; i < Math.min(50, payloadList.size()); i++) {
											previewBuilder.append(payloadList.get(i)).append("\n");
										}
										payloadBinding.etPayloadValues.setText(previewBuilder.toString().trim());
										payloadBinding.etPayloadValues.setEnabled(false);
										Toast.makeText(this, "File loaded successfully", Toast.LENGTH_SHORT).show();
									} catch (Exception e) {
										Log.e("FilePicker", "View bind failed: " + e.getMessage(), e);
										Toast.makeText(this, "Failed to bind view", Toast.LENGTH_SHORT).show();
									}
								});
							} else {
								runOnUiThread(() -> Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show());
							}
						});
					}
				}
			});*/
		initFuzzer();
		initRecyclerView();
		initUIListeners();
		initThreadSpinner();
		initFilePickerLauncher();

		loadHttpMessage();
		initializeSpinnerOptions();
		setupSpinnerListener();

	}

	private void initFuzzer() {
		fuzzer = new Fuzzer(4);
		fuzzer.setProgressListener(this);
	}

	private void initRecyclerView() {
		resultsAdapter = new ResultsAdapter(this, new ArrayList<>());
		binding.recyclerResults.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		binding.recyclerResults.setAdapter(resultsAdapter);
		resultsAdapter.setOnResultClickListener(v -> showResultDetailsDialog(v, 1));
	}

	private void initUIListeners() {
		binding.btnAddPayload.setOnClickListener(v -> showAddPayloadDialog());
		binding.btnStartFuzzing.setOnClickListener(v -> startFuzzing());

		binding.intruderCancel.setOnClickListener(v -> {
			if (binding.layoutInputSection.getVisibility() == View.GONE) {
				binding.layoutInputSection.setVisibility(View.VISIBLE);
			}
			fuzzer.stopFuzzing();
			binding.btnAddPayload.setEnabled(true);
			binding.btnStartFuzzing.setEnabled(true);
		});

		binding.intruderResponseCancel.setOnClickListener(v -> {
			if (binding.fuzzingLayoutFramelayout.getVisibility() == View.VISIBLE) {
				binding.fuzzingLayoutFramelayout.setVisibility(View.GONE);
			}
			
		});

		binding.intruderNextArrow.setOnClickListener(v -> goToNextMatch(binding.intruderResponse.getText().toString()));
		binding.intruderPreviousArrow
				.setOnClickListener(v -> goToPreviousMatch(binding.intruderResponse.getText().toString()));

		binding.intruderSearch.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH) {
				searchInFullText(binding.intruderSearch.getText().toString(), fullResponse);
				return true;
			}
			return false;
		});

		binding.fuzzingFilter.setOnClickListener(v -> showBottomSheet());
	}

	private void initThreadSpinner() {
		List<String> threadOptions = new ArrayList<>();
		threadOptions.add("Auto");
		for (int i = 1; i <= 10; i++)
			threadOptions.add(String.valueOf(i));

		ArrayAdapter<String> threadAdapter = new ArrayAdapter<>(this, R.layout.spinner_text, threadOptions);
		threadAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.spinnerThreadCount.setAdapter(threadAdapter);
	}

	private void initFilePickerLauncher() {
		filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
			if (result.getResultCode() == RESULT_OK && result.getData() != null && currentPayloadGroupView != null) {
				Uri uri = result.getData().getData();
				if (uri != null) {
					Executors.newSingleThreadExecutor().execute(() -> {
						String text = readTextFromUri(uri);
						if (text != null) {
							List<String> payloadList = Arrays.asList(text.split("\\n"));
							runOnUiThread(() -> bindExternalPayloadView(payloadList));
						} else {
							runOnUiThread(() -> Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show());
						}
					});
				}
			}
		});
	}

	private void bindExternalPayloadView(List<String> payloadList) {
		try {
			ItemPayloadGroupBinding payloadBinding = ItemPayloadGroupBinding.bind(currentPayloadGroupView);
			String payloadType = payloadBinding.spinnerPayloadType.getSelectedItem().toString();

			externalPayloadCache.put(payloadType, payloadList);

			StringBuilder previewBuilder = new StringBuilder();
			for (int i = 0; i < Math.min(50, payloadList.size()); i++) {
				previewBuilder.append(payloadList.get(i)).append("\n");
			}

			payloadBinding.etPayloadValues.setText(previewBuilder.toString().trim());
			payloadBinding.etPayloadValues.setEnabled(false);
			Toast.makeText(this, "File loaded successfully", Toast.LENGTH_SHORT).show();
		} catch (Exception e) {
			Log.e("FilePicker", "View bind failed: " + e.getMessage(), e);
			Toast.makeText(this, "Failed to bind view", Toast.LENGTH_SHORT).show();
		}
	}

	private void loadHttpMessage() {
		HttpMessage message = (HttpMessage) getIntent().getSerializableExtra("message");
		if (message != null) {
			setHttpMessage(message);
			binding.etTargetUrl.setText(message.url());
		}
	}

	private void initializeSpinnerOptions() {
		String[] options = getResources().getStringArray(R.array.spinner_options);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_text, options);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		binding.intruderSpinner.setAdapter(adapter);
		binding.intruderSpinner.setSelection(1);
	}

	private void setupSpinnerListener() {
		binding.intruderSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
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
		if (fuzzResult != null && currentType != selectedPosition) {
			currentType = selectedPosition;
			showResultDetailsDialog(fuzzResult, currentType);
		}
	}

	private void showAddPayloadDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		DialogBinding dialogBinding = DialogBinding.inflate(LayoutInflater.from(this));
		// Initialize container and buttons
		containerPayloadGroups = dialogBinding.containerPayloadGroups;

		if (dialogBinding.containerPayloadGroups.getChildCount() == 0) {
			addNewPayloadGroup();
		}

		// Set up click listeners
		dialogBinding.btnAddPayloadGroup.setOnClickListener(v -> addNewPayloadGroup());

		// Save all button should collect data from all payload groups
		dialogBinding.btnSaveAll.setOnClickListener(v -> {
			saveAllPayloads();
			// No need to dismiss the dialog here as it will be handled by the AlertDialog buttons
		});

		builder.setTitle("Add Payload List").setView(dialogBinding.getRoot())
				.setPositiveButton("Add", (dialog, which) -> {
					// Process all payload groups when dialog is confirmed
					saveAllPayloads();
				}).setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).show();
	}

	private void addNewPayloadGroup() {
		// Inflate a new payload group layout using View Binding
		ItemPayloadGroupBinding itemPayloadGroupBinding = ItemPayloadGroupBinding.inflate(getLayoutInflater(),
				containerPayloadGroups, false);

		// Set up the spinner with payload types
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item,
				new String[] { "FUZZ", "FUZZ1", "FUZZ2", "FUZZ3", "FUZZ4" });
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		itemPayloadGroupBinding.spinnerPayloadType.setAdapter(adapter);

		// Set up remove button
		itemPayloadGroupBinding.btnRemovePayload
				.setOnClickListener(v -> containerPayloadGroups.removeView(itemPayloadGroupBinding.getRoot()));

		// Set up the "Add From External" button for each payload group
		itemPayloadGroupBinding.dialogAddExternalButton
				.setOnClickListener(v -> openFile(itemPayloadGroupBinding.getRoot()));

		// Add the new view to the container
		containerPayloadGroups.addView(itemPayloadGroupBinding.getRoot());
	}

	private void saveAllPayloads() {
		for (int i = 0; i < containerPayloadGroups.getChildCount(); i++) {
			View payloadView = containerPayloadGroups.getChildAt(i);
			ItemPayloadGroupBinding payloadGroupBinding = ItemPayloadGroupBinding.bind(payloadView);

			String payloadType = payloadGroupBinding.spinnerPayloadType.getSelectedItem().toString();

			List<String> payloadList;

			if (externalPayloadCache.containsKey(payloadType)) {
				payloadList = externalPayloadCache.get(payloadType); // use cached full payload
			} else {
				String payloadValuesText = payloadGroupBinding.etPayloadValues.getText().toString().trim();
				payloadList = Arrays.asList(payloadValuesText.split("\\n"));
			}

			if (!payloadList.isEmpty()) {
				payloads.put(payloadType, payloadList);
				Toast.makeText(this, "Added " + payloadList.size() + " payloads for " + payloadType, Toast.LENGTH_SHORT)
						.show();
			}
		}
	}

	// Modified to handle opening files for specific payload groups
	private void openFile(View payloadGroupView) {
		// Store the current payload group view to use in onActivityResult
		currentPayloadGroupView = payloadGroupView;

		Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
		intent.setType("text/plain");
		filePickerLauncher.launch(Intent.createChooser(intent, "Select Payload File"));

	}

	// Add this as a class member field
	private View currentPayloadGroupView;

	private String readTextFromUri(Uri uri) {
		StringBuilder stringBuilder = new StringBuilder();
		try (InputStream inputStream = getContentResolver().openInputStream(uri);
				BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
			String line;
			while ((line = reader.readLine()) != null) {
				stringBuilder.append(line).append("\n");
			}
		} catch (IOException e) {
			Log.e("FilePicker", "Error reading file", e);
			Toast.makeText(this, "Error reading file", Toast.LENGTH_SHORT).show();
			return null;
		}
		return stringBuilder.toString();
	}

	private void startFuzzing() {
		String template = binding.etRequestTemplate.getText().toString().trim();
		String targetUrl = binding.etTargetUrl.getText().toString().trim();

		if (template.isEmpty() || targetUrl.isEmpty()) {
			Toast.makeText(this, "Please enter both request template and target URL", Toast.LENGTH_SHORT).show();
			return;
		}

		if (payloads.isEmpty()) {
			Toast.makeText(this, "Please add at least one payload list", Toast.LENGTH_SHORT).show();
			return;
		}
		if (extractFuzzPoints(binding.etRequestTemplate.getText().toString())) {
			showToast("No fuzz points found");
			return;
		}

		// Reset UI
		binding.progressBar.setProgress(0);
		resultsAdapter.clearResults();
		binding.tvStatus.setText("Starting fuzzing...");
		binding.btnStartFuzzing.setEnabled(false);
		binding.btnAddPayload.setEnabled(false);
		int threadCount = 4;
		String threadMode = binding.spinnerThreadCount.getSelectedItem().toString();

		if (threadMode.equalsIgnoreCase("Auto")) {

			if (total <= 500) {
				threadCount = 8;
			} else if (total <= 1000) {
				threadCount = 10;
			} else if (total <= 1500) {
				threadCount = 12;
			} else {
				int maxThreads = Runtime.getRuntime().availableProcessors() * 2;
				threadCount = Math.min(14, maxThreads);
			}
		} else {
			try {
				String threadText = binding.spinnerThreadCount.getSelectedItem().toString().trim();
				int manualThreads = Integer.parseInt(threadText);
				threadCount = Math.max(1, manualThreads); // ensure at least 1
			} catch (Exception e) {
				Toast.makeText(this, "Invalid thread count. Using default (4)", Toast.LENGTH_SHORT).show();
			}
		}

		fuzzer = new Fuzzer(threadCount);
		fuzzer.setProgressListener(this);

		for (Map.Entry<String, List<String>> entry : payloads.entrySet()) {
			fuzzer.addPayloadList(entry.getKey(), entry.getValue());
		}

		fuzzer.addResponseAnalyzer(200, result -> {

		});

		fuzzer.addResponseAnalyzer(500, result -> {

		});
		binding.fuzzingLayoutOptionsPlayout.setVisibility(View.VISIBLE);
		binding.layoutInputSection.setVisibility(View.GONE);
		fuzzer.fuzz(template, targetUrl, new Fuzzer.FuzzResultCallback() {
			@Override
			public void onNewResponse(Fuzzer.FuzzResult results) {
				if (resultsAdapter != null) {
					resultsAdapter.addMewResult(results);
					originalResults.add(results);
				}
			}

			@Override
			public void onError(Exception e) {
				binding.tvStatus.setText("Error: " + e.getMessage());

				binding.btnStartFuzzing.setEnabled(true);
				binding.btnAddPayload.setEnabled(true);
			}
		});

	}

	private boolean extractFuzzPoints(String template) {
		Map<Integer, String> fuzzPoints = new HashMap<>();
		Matcher matcher = FUZZ_PATTERN.matcher(template);

		while (matcher.find()) {
			String indexStr = matcher.group(1);
			int index = indexStr.isEmpty() ? 0 : Integer.parseInt(indexStr);
			fuzzPoints.put(index, "FUZZ" + indexStr);
		}

		return fuzzPoints.isEmpty();
	}

	private void showToast(String message) {
		Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
	}

	private void showResultDetailsDialog(Fuzzer.FuzzResult result, int pos) {
		fuzzResult = result;

		binding.fuzzingLayoutFramelayout.setVisibility(View.VISIBLE);
		StringBuilder response = new StringBuilder();

		response.append(result.getHeaders()).append("\n\n").append(result.getResponseBody());
		String request = result.getRequestString();
		//	result.getRequest().headers()
		//	.forEach(entry -> stringBuilder.append(String.format("%s: %s\n", entry.getKey(), entry.getValue())));

		if (pos == 0) {
			// Remove previous scroll listener if it exists
			if (scrollListener != null) {
				binding.intruderScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
				scrollListener = null;
			}

			binding.intruderResponse.setText(Utils.highlightAndFormatHttpRequest(request));
		} else if (pos == 1) {
			startLazyLoad(response.toString());
			//	binding.intruderResponse.setText(Utils.highlightAndFormatHttpRequest(response.toString()));

		}
		this.fullResponse = response.toString();

	}

	@Override
	public void onProgressUpdate(int completed, int total) {
		binding.progressBar.setMax(total);
		binding.progressBar.setProgress(completed);
		this.total = total;
		binding.tvStatus.setText("Progress: " + completed + "/" + total);
		if (completed == total) {
			binding.btnAddPayload.setEnabled(true);
			binding.btnStartFuzzing.setEnabled(true);
		}

	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (fuzzer != null) {
			fuzzer.setProgressListener(null);
		}
		cleanupMemory();

	}

	private void cleanupMemory() {
		// Nullify large response data
		fullResponse = null;

		if (textChunks != null) {
			textChunks.clear();
			textChunks = null;
		}

		if (matchPositions != null) {
			matchPositions.clear();
			matchPositions = null;
		}

		// Clear payload-related data
		if (payloads != null)
			payloads.clear();
		if (externalPayloadCache != null)
			externalPayloadCache.clear();

		// Clear result lists
		if (originalResults != null)
			originalResults.clear();
		if (filteredResults != null)
			filteredResults.clear();

		// Shut down fuzzer if it has active resources or threads

		// Nullify ViewBinding to release references to views
		binding = null;

		// Optionally clear adapter if it’s still attached
		if (resultsAdapter != null) {
			resultsAdapter = null;
		}
	}

	private static class PayloadData {
		String type;
		String[] values;

		PayloadData(String type, String[] values) {
			this.type = type;
			this.values = values;
		}
	}

	private void showBottomSheet() {
		BottomSheetFilterBinding binding = BottomSheetFilterBinding.inflate(LayoutInflater.from(this));
		View view = binding.getRoot();
		BottomSheetDialog dialog = new BottomSheetDialog(this);
		dialog.setContentView(view);
		dialog.show();

		// Initialize sorting dropdown

		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
				sortOptions);
		binding.sortDropdown.setAdapter(adapter);
		if (sortType != null)
			binding.sortDropdown.setText(sortType, false);
		else
			binding.sortDropdown.setText(sortOptions[0], false); // Set default sorting option

		// Initialize response time comparison dropdown
		String[] comparisonOptions = { "<", ">", "=" };
		ArrayAdapter<String> comparisonAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line,
				comparisonOptions);
		binding.responseTimeComparisonDropdown.setAdapter(comparisonAdapter);
		binding.responseTimeComparisonDropdown.setText(comparisonOptions[0], false); // Set default comparison option

		// Set initial range for the RangeSlider
		float minValue = 0f; // Use 0 or your actual minimum
		float maxValue = 10000f; // Your maximum value
		binding.lengthRangeSlider.setValueFrom(minValue);
		binding.lengthRangeSlider.setValueTo(maxValue);
		binding.lengthRangeSlider.setValues(minValue, maxValue); // Set initial slider values

		// Apply Filters Button Click Listener
		binding.applyFilterButton.setOnClickListener(v -> {
			// Get selected status codes from ChipGroup
			List<Integer> selectedCodes = new ArrayList<>();
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
		//String containsText = binding.containsTextEditText.getText().toString().trim();

			// Get sorting type
			sortType = binding.sortDropdown.getText().toString().trim();

			// Get response time comparison type and value
			String comparisonType = binding.responseTimeComparisonDropdown.getText().toString().trim();
			String responseTimeInput = binding.responseTimeEditText.getText().toString().trim();
			long responseTimeFilterValue = responseTimeInput.isEmpty() ? -1 : Long.parseLong(responseTimeInput);

			// Call applyFilters with all the parameters
			applyFilters(selectedCodes, minLen, maxLen, sortType, comparisonType,
					responseTimeFilterValue);

			// Dismiss the bottom sheet after applying filters
			dialog.dismiss();
		});
	}

	private void applyFilters(List<Integer> selectedStatusCodes, float minLen, float maxLen,
			String sortBy, String comparisonType, long responseTimeFilterValue) {
		List<Fuzzer.FuzzResult> filtered = new ArrayList<>();

		if (originalResults == null) {
			Log.e("FilterError", "originalResults is null");
			return;
		}

		for (Fuzzer.FuzzResult result : originalResults) {
			if (result == null) {
				Log.e("FilterError", "Null result in originalResults");
				continue;
			}

			int status = result.getStatusCode();
			int length = result.getContentLength();
			String body = result.getResponseBody();
			long responseTime = result.getResponseTimeMillis();

			boolean statusMatch = selectedStatusCodes.isEmpty() || selectedStatusCodes.contains(status);
			boolean lengthMatch = true; // Default to true

			// Only apply length filter if minLen or maxLen are not default values.
			if (minLen != 0 || maxLen != 10000f) {
				lengthMatch = length >= minLen && length <= maxLen;
			}

			
			// Response time filter logic
			boolean responseTimeMatch = true; // Default to true
			if (responseTimeFilterValue != -1) {
				switch (comparisonType) {
				case "<":
					responseTimeMatch = responseTime < responseTimeFilterValue;
					break;
				case ">":
					responseTimeMatch = responseTime > responseTimeFilterValue;
					break;
				case "=":
					responseTimeMatch = responseTime == responseTimeFilterValue;
					break;
				}
			}

			if (statusMatch && lengthMatch && responseTimeMatch) {
				filtered.add(result);
			}
		}

		// Safe sorting logic
		if (sortBy != null && !filtered.isEmpty()) {
			try {
				String sortByNormalized = sortBy.trim();
				switch (sortByNormalized) {
				case "Status Code":
					Collections.sort(filtered, Comparator.comparingInt(Fuzzer.FuzzResult::getStatusCode));
					break;
				case "Length":
					Collections.sort(filtered, Comparator.comparingInt(Fuzzer.FuzzResult::getContentLength));
					break;
				case "ResponseTime":
					Collections.sort(filtered, Comparator.comparingLong(Fuzzer.FuzzResult::getResponseTimeMillis));
					break; 
				case "Status Code (DESC)":
					Collections.sort(filtered, Comparator.comparingInt(Fuzzer.FuzzResult::getStatusCode).reversed());
					break;
				case "Length (DESC)":
					Collections.sort(filtered, Comparator.comparingInt(Fuzzer.FuzzResult::getContentLength).reversed());
					break;
				case "ResponseTime (DESC)":
					Collections.sort(filtered, Comparator.comparingLong(Fuzzer.FuzzResult::getResponseTimeMillis).reversed());
					break;
				}
			} catch (Exception e) {
				Log.e("SORT", "Sorting failed: " + e.getMessage());
			}
		}

		if (filteredResults != null) {
			filteredResults.clear();
			filteredResults.addAll(filtered);
		} else {
			Log.e("FilterError", "filteredResults is null");
		}

		if (resultsAdapter != null) {
			try {
				resultsAdapter.updateResults(filteredResults);
			} catch (Exception e) {
				Log.e("AdapterError", "Error updating adapter: " + e.getMessage());
			}
		} else {
			Log.e("AdapterError", "resultsAdapter is null");
		}
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
			highlightMatch(currentMatchIndex, fullText);
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
			binding.intruderResponse.setText(""); // Clear the text view
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

		binding.intruderResponse.setText(spannableBuilder);

		// Scroll to the match
		scrollToMatch(start);
	}

	private void scrollToMatch(int matchStart) {

		Layout layout = binding.intruderResponse.getLayout();
		if (layout != null) {
			int line = layout.getLineForOffset(matchStart);
			int y = layout.getLineTop(line);

			// Add an offset to scroll slightly before the match
			int scrollOffset = Math.max(y - 200, 0); // Ensure we don't scroll past the top
			binding.intruderScrollView.post(() -> binding.intruderScrollView.smoothScrollTo(0, scrollOffset));
		}
	}

	/*	public void startLazyLoad(String fullResponse) {
			if (fullResponse == null || fullResponse.isEmpty()) {
				binding.intruderResponse.setText(""); // Clear the text view
				return;
			}
	
			// Split the response into chunks on a background thread
			new Thread(() -> {
				List<String> chunks = splitIntoChunks(fullResponse, DEFAULT_CHUNK_SIZE);
	
				// Update UI on the main thread
				runOnUiThread(() -> {
					textChunks = chunks;
					currentChunkIndex = 0;
					binding.intruderResponse.setText(""); // Clear the text view
	
					loadNextChunk(); // Load the first chunk
					
					// Set scroll listener
					binding.intruderScrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
						if (binding.intruderScrollView.getChildAt(0).getBottom() <= (binding.intruderScrollView.getHeight()
								+ binding.intruderScrollView.getScrollY())) {
							// User reached the bottom
							loadNextChunk();
						}
					});
				});
			}).start();
		}
	*/
	public void startLazyLoad(String fullResponse) {
		if (fullResponse == null || fullResponse.isEmpty()) {
			binding.intruderResponse.setText("");
			return;
		}

		// Clear previous scroll listener if any
		if (scrollListener != null) {
			binding.intruderScrollView.getViewTreeObserver().removeOnScrollChangedListener(scrollListener);
		}

		new Thread(() -> {
			List<String> chunks = splitIntoChunks(fullResponse, DEFAULT_CHUNK_SIZE);

			runOnUiThread(() -> {
				textChunks = chunks;
				currentChunkIndex = 0;
				binding.intruderResponse.setText("");

				loadNextChunk();

				// Setup new scroll listener
				scrollListener = () -> {
					if (binding.intruderScrollView.getChildAt(0).getBottom() <= (binding.intruderScrollView.getHeight()
							+ binding.intruderScrollView.getScrollY())) {
						loadNextChunk();
					}
				};

				binding.intruderScrollView.getViewTreeObserver().addOnScrollChangedListener(scrollListener);
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

	private void loadNextChunk() {
		if (currentChunkIndex < textChunks.size()) {
			String chunk = textChunks.get(currentChunkIndex);
			SpannableStringBuilder highlightedChunk = new SpannableStringBuilder(chunk);
			if (currentChunkIndex == 0)
				highlightedChunk = Utils.highlightAndFormatHttpRequest(chunk);
			binding.intruderResponse.append(highlightedChunk);
			currentChunkIndex++;
		}
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
			bodyText = "";
		}

		if (requestStr.startsWith(":")) {
	
				String http1Request = Utils.convertHttp2ToHttp1(requestStr);
				binding.etRequestTemplate.setText(Utils.highlightAndFormatHttpRequest(http1Request + "\n" + bodyText));
			
		} else {
			request.append("\n").append(bodyText);
			binding.etRequestTemplate.setText(Utils.highlightAndFormatHttpRequest(request.toString()));
		}
	}

}