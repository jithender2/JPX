package com.proxy.ui.Adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.proxy.R;
import com.proxy.Fuzzer.Fuzzer;

import com.proxy.listener.SetLogger;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ResultsAdapter extends RecyclerView.Adapter<ResultsAdapter.ResultViewHolder> {

	private final Context context;
	private List<Fuzzer.FuzzResult> results;
	private OnResultClickListener clickListener;
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

	public ResultsAdapter(Context context, List<Fuzzer.FuzzResult> results) {
		this.context = context;
		this.results = results;
	}

	@NonNull
	@Override
	public ResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(context).inflate(R.layout.item_fuzzing_layout, parent, false);
		return new ResultViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ResultViewHolder holder, int position) {
		Fuzzer.FuzzResult result = results.get(position);

		// Set status code with appropriate color
		int statusCode = result.getStatusCode();//.result.getResponse().status().code();
		holder.tvStatusCode.setText(String.valueOf(statusCode));

		// Color status based on code
		if (statusCode >= 200 && statusCode < 300) {
			holder.tvStatusCode.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
		} else if (statusCode >= 300 && statusCode < 400) {
			holder.tvStatusCode.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
		} else if (statusCode >= 400 && statusCode < 500) {
			holder.tvStatusCode.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
		} else {
			holder.tvStatusCode.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
		}

		// Set URL path
		String path = result.getPath();
		holder.tvPath.setText(path);

		// Set body size
		long contentLength = result.getContentLength();
		holder.tvContentLength.setText(String.format(Locale.getDefault(), "%d bytes", contentLength));

		// Set timestamp
		holder.tvTimestamp.setText(result.getResponseTimeMillis() + "ms");

		// Set payload summary
		StringBuilder payloadBuilder = new StringBuilder();
		for (int key : result.getPayloads().keySet()) {
			String prefix = key == 0 ? "FUZZ" : "FUZZ" + key;
			payloadBuilder.append(prefix).append(": ").append(result.getPayloads().get(key)).append(" | ");
		}

		String payloadText = payloadBuilder.toString();
		if (payloadText.length() > 2) {
			payloadText = payloadText.substring(0, payloadText.length() - 3);
		}
		holder.tvPayloads.setText(payloadText);

		holder.itemView.setOnClickListener(v -> {
			clickListener.onResultClick(result);
		});
	}

	@Override
	public int getItemCount() {
		return results.size();
	}

	public void updateResults(List<Fuzzer.FuzzResult> newResults) {
		this.results = newResults;
		
		notifyDataSetChanged();
	}

	public void addMewResult(Fuzzer.FuzzResult fuzzResult) {
		this.results.add(fuzzResult);
		notifyItemInserted(this.results.size() - 1);

	}

	public void clearResults() {
		this.results.clear();
		notifyDataSetChanged();
	}

	public Fuzzer.FuzzResult getItem(int position) {
		return results.get(position);
	}

	public void setOnResultClickListener(OnResultClickListener listener) {
		this.clickListener = listener;
	}

	public interface OnResultClickListener {
		void onResultClick(Fuzzer.FuzzResult result);
	}

	class ResultViewHolder extends RecyclerView.ViewHolder {
		TextView tvStatusCode;
		TextView tvPath;
		TextView tvContentLength;
		TextView tvTimestamp;
		TextView tvPayloads;

		ResultViewHolder(View itemView) {
			super(itemView);
			tvStatusCode = itemView.findViewById(R.id.tv_status_code);
			tvPath = itemView.findViewById(R.id.tv_path);
			tvContentLength = itemView.findViewById(R.id.tv_content_length);
			tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
			tvPayloads = itemView.findViewById(R.id.tv_payloads);

			itemView.setOnClickListener(v -> {
				int position = getAdapterPosition();
				if (position != RecyclerView.NO_POSITION && clickListener != null) {
					clickListener.onResultClick(results.get(position));
				}
			});
		}
	}

}