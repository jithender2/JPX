package com.proxy.ui.Adapter;

import android.content.Context;
import android.os.Looper;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.proxy.R;
import com.proxy.data.ContentType;
import com.proxy.data.Http1Message;
import com.proxy.data.Http2Message;
import com.proxy.data.HttpMessage;
import com.proxy.data.Message;

import com.proxy.databinding.RecyclerLayoutHistoryBinding;
import com.proxy.listener.SetLogger;
import com.proxy.utils.Networks;
import com.proxy.utils.Utils;
import java.net.URISyntaxException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Predicate;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
/*
public class HttpRecyclerAdapter extends RecyclerView.Adapter<HttpRecyclerAdapter.ViewHolder> {
	private final Context context;
	private final List<MessageWrapper> allMessages = new ArrayList<>();
	private List<MessageWrapper> filteredMessages = new ArrayList<>();

	private Function<MessageWrapper, Comparable> currentSortingKey = this::extractStatusCode;
	private boolean currentSortAscending = true;

	private boolean isScopeApplied = false;
	private final Set<String> scopeItems = new HashSet<>();
	private final Set<Integer> statusCodeFilters = new HashSet<>();

	private final Map<String, String> domainCache = new HashMap<>();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler uiHandler = new Handler(Looper.getMainLooper());

	public HttpRecyclerAdapter(Context context) {
		this.context = context;
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		RecyclerLayoutHistoryBinding binding;

		public ViewHolder(@NonNull RecyclerLayoutHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		RecyclerLayoutHistoryBinding binding = RecyclerLayoutHistoryBinding.inflate(inflater, parent, false);
		return new ViewHolder(binding);
	}

	private OnItemClickListener onItemClickListener;

	public interface OnItemClickListener {
		void onItemClick(int position);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		MessageWrapper wrapper = filteredMessages.get(position);

		// Use pre-computed values
		holder.binding.recyclerHostTextView.setText(wrapper.host);
		holder.binding.recyclerMethodTextView.setText(wrapper.method);
		holder.binding.recyclerPathTextView.setText(wrapper.path);
		holder.binding.recyclerStatusTextView.setText(wrapper.status);
		holder.binding.recyclerContentLengthTextView.setText("Length: " + wrapper.lengthStr);
		holder.binding.recyclerMimeTextView.setText("Mime: " + wrapper.mime);

		// Avoid creating a new OnClickListener for each bind
		holder.itemView.setOnClickListener(v -> {
			if (position != RecyclerView.NO_POSITION && onItemClickListener != null) {
				onItemClickListener.onItemClick(position);
			}
		});
	}

	public List<Message> getMessages() {
		return filteredMessages.stream().map(wrapper -> wrapper.message).collect(Collectors.toList());
	}

	public void SortDesc() {
		currentSortAscending = false;
		sortFilteredMessages();
	}

	public void SortAsc() {
		currentSortAscending = true;
		sortFilteredMessages();
	}

	public void sortByLength() {
		currentSortingKey = this::extractResponseLength;
		sortFilteredMessages();
	}

	public void filterByStatusCodes(Set<Integer> allowedStatusCodes) {
		statusCodeFilters.clear();
		if (allowedStatusCodes != null) {
			statusCodeFilters.addAll(allowedStatusCodes);
		}
		rebuildFilteredList();
	}

	public void sortByMimeType() {
		currentSortingKey = this::extractMimeType;
		sortFilteredMessages();
	}

	private Integer extractResponseLength(MessageWrapper wrapper) {
		return wrapper.length;
	}

	private String extractMimeType(MessageWrapper wrapper) {
		return wrapper.mime;
	}

	/*private void sortFilteredMessages() {
		filteredMessages.sort((m1, m2) -> {
			Comparable key1 = currentSortingKey.apply(m1);
			Comparable key2 = currentSortingKey.apply(m2);
	
			if (key1 == null && key2 == null)
				return 0;
			if (key1 == null)
				return currentSortAscending ? 1 : -1;
			if (key2 == null)
				return currentSortAscending ? -1 : 1;
	
			return currentSortAscending ? key1.compareTo(key2) : key2.compareTo(key1);
		});
	
		notifyDataSetChanged();
	}*//*
		
		private void sortFilteredMessages() {
		List<MessageWrapper> oldList = new ArrayList<>(filteredMessages);
		executorService.execute(() -> {
			filteredMessages.sort((m1, m2) -> {
				Comparable key1 = currentSortingKey.apply(m1);
				Comparable key2 = currentSortingKey.apply(m2);
		
				if (key1 == null && key2 == null)
					return 0;
				if (key1 == null)
					return currentSortAscending ? 1 : -1;
				if (key2 == null)
					return currentSortAscending ? -1 : 1;
		
				return currentSortAscending ? key1.compareTo(key2) : key2.compareTo(key1);
			});
		
			DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(oldList, filteredMessages));
			uiHandler.post(() -> {
				diffResult.dispatchUpdatesTo(this);
			});
		});
		
		}
		
		public void refresh() {
		notifyDataSetChanged();
		}
		
		public void sortByStatus() {
		currentSortingKey = this::extractStatusCode;
		sortFilteredMessages();
		}
		
		public void sortByMethod() {
		currentSortingKey = this::extractMethod;
		sortFilteredMessages();
		}
		
		private Integer extractStatusCode(MessageWrapper wrapper) {
		return wrapper.statusCode;
		}
		
		private String extractMethod(MessageWrapper wrapper) {
		return wrapper.method;
		}
		
		/*	public void addAll(List<Message> newMessages) {
		if (newMessages == null || newMessages.isEmpty())
			return;
		executorService.execute(()->{
		
		List<MessageWrapper> newWrappers = newMessages.stream().map(MessageWrapper::new).collect(Collectors.toList());
		
		DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(allMessages, newWrappers));
		
		allMessages.addAll(newWrappers);
		
		
		rebuildFilteredList();
		
		//	diffResult.dispatchUpdatesTo(this);
		});
		}*//*
			
			public void addAll(List<Message>newMessages){if(newMessages==null||newMessages.isEmpty())return;
			
			executorService.execute(()->{List<MessageWrapper>newWrappers=newMessages.stream().map(MessageWrapper::new).collect(Collectors.toList());
			
			List<MessageWrapper>newAllMessages=new ArrayList<>(allMessages);newAllMessages.addAll(newWrappers);
			
			List<MessageWrapper>newFilteredList=newAllMessages.stream().filter(wrapper->{boolean isInScope=!isScopeApplied||scopeItems.contains(wrapper.baseDomain);boolean matchesStatus=statusCodeFilters.isEmpty()||statusCodeFilters.contains(wrapper.statusCode);return isInScope&&matchesStatus;}).collect(Collectors.toList());
			
			newFilteredList.sort((m1,m2)->{Comparable key1=currentSortingKey.apply(m1);Comparable key2=currentSortingKey.apply(m2);if(key1==null&&key2==null)return 0;if(key1==null)return currentSortAscending?1:-1;if(key2==null)return currentSortAscending?-1:1;return currentSortAscending?key1.compareTo(key2):key2.compareTo(key1);});
			
			List<MessageWrapper>oldFiltered=new ArrayList<>(filteredMessages);DiffUtil.DiffResult diffResult=DiffUtil.calculateDiff(new MessageDiffCallback(oldFiltered,newFilteredList));
			
			uiHandler.post(()->{allMessages.addAll(newWrappers); // safe now
			filteredMessages=newFilteredList;diffResult.dispatchUpdatesTo(this);});});}
			
			public void addMessage(Message message){if(message==null)return;
			
			MessageWrapper wrapper=new MessageWrapper(message);allMessages.add(wrapper);
			
			// Only add to filtered list if it passes filters
			String genericHost=Networks.genericMultiCDNS(message.host());boolean isInScope=!isScopeApplied||scopeItems.contains(wrapper.baseDomain);boolean matchesStatus=statusCodeFilters.isEmpty()||statusCodeFilters.contains(wrapper.statusCode);
			
			if(isInScope&&matchesStatus){filteredMessages.add(wrapper);sortFilteredMessages();notifyItemInserted(filteredMessages.size()-1);}}
			
			private void rebuildFilteredList(){List<MessageWrapper>oldList=new ArrayList<>(filteredMessages);
			
			filteredMessages=allMessages.stream().filter(wrapper->{boolean isInScope=!isScopeApplied||scopeItems.contains(wrapper.baseDomain)|scopeItems.contains(wrapper.host);boolean matchesStatus=statusCodeFilters.isEmpty()||statusCodeFilters.contains(wrapper.statusCode);return isInScope&&matchesStatus;}).collect(Collectors.toList());
			
			sortFilteredMessages();
			
			DiffUtil.DiffResult diffResult=DiffUtil.calculateDiff(new MessageDiffCallback(oldList,filteredMessages));diffResult.dispatchUpdatesTo(this);}
			
			private String getBaseDomain(String host){if(host==null||host.isEmpty())return"";
			
			// Check cache first
			String cachedDomain=domainCache.get(host);if(cachedDomain!=null){return cachedDomain;}
			
			// Compute and cache
			String result;try{Uri uri=Uri.parse("http://"+host);String domain=uri.getHost();
			
			if(domain==null){result=host;}else{String[]parts=domain.split("\\.");if(parts.length>=2){result=parts[parts.length-2]+"."+parts[parts.length-1];}else{result=domain;}}}catch(Exception e){
			// Fallback for any parsing issues
			String[]parts=host.split("\\.");if(parts.length>=2){result=parts[parts.length-2]+"."+parts[parts.length-1];}else{result=host;}}
			
			domainCache.put(host,result);return result;}
			
			public void applyScope(boolean scope){isScopeApplied=scope;rebuildFilteredList();}
			
			public void clearScope(){scopeItems.clear();isScopeApplied=false;rebuildFilteredList();}
			
			public boolean isScopeApplied(){return isScopeApplied;}
			
			public boolean isScopeEmpty(){return scopeItems.isEmpty();}
			
			public void addToScope(String item){scopeItems.add(item);}
			
			@Override public int getItemCount(){return filteredMessages.size();}
			
			// Cache computed values for each Message
			private class MessageWrapper {
			final Message message;
			final String host;
			final String path;
			final String method;
			final String status;
			final Integer statusCode;
			final int length;
			final String lengthStr;
			final String mime;
			final String baseDomain;
			
			MessageWrapper(Message message) {
			this.message = message;
			
			// Extract all values once
			String url = "", p = "", m = "", s = "-", contentType = "";
			int l = 0;
			Integer sc = null;
			
			if (message instanceof Http1Message) {
			Http1Message httpMessage = (Http1Message) message;
			url = httpMessage.host() != null ? httpMessage.host() : "";
			p = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
					: "";
			m = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
					: "";
			if (httpMessage.responseHeader() != null) {
				sc = httpMessage.responseHeader().statusCode();
				s = String.valueOf(sc);
				contentType = httpMessage.MimeType();
			}
			l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;
			} else if (message instanceof Http2Message) {
			Http2Message httpMessage = (Http2Message) message;
			url = httpMessage.host() != null ? httpMessage.host() : "";
			p = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
					: "";
			m = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
					: "";
			if (httpMessage.responseHeader() != null) {
				sc = httpMessage.responseHeader().statusCode();
				s = String.valueOf(sc);
				contentType = httpMessage.MimeType();
			}
			l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;
			}
			
			this.host = url;
			this.path = p;
			this.method = m;
			this.status = s;
			this.statusCode = sc;
			this.length = l;
			this.lengthStr = String.valueOf(l); // Pre-compute string representation
			this.mime = contentType != null ? contentType : "";
			
			// Pre-compute base domain
			String genericHost = Networks.genericMultiCDNS(url);
			this.baseDomain = getBaseDomain(genericHost);
			}
			
			@Override
			public boolean equals(Object obj) {
			if (this == obj)
			return true;
			if (obj == null || getClass() != obj.getClass())
			return false;
			
			MessageWrapper that = (MessageWrapper) obj;
			return message.equals(that.message);
			}
			
			@Override
			public int hashCode() {
			return message.hashCode();
			}
			}
			
			// DiffUtil callback implementation
			private static class MessageDiffCallback extends DiffUtil.Callback {
			private final List<MessageWrapper> oldList;
			private final List<MessageWrapper> newList;
			
			MessageDiffCallback(List<MessageWrapper> oldList, List<MessageWrapper> newList) {
			this.oldList = oldList;
			this.newList = newList;
			}
			
			@Override
			public int getOldListSize() {
			return oldList.size();
			}
			
			@Override
			public int getNewListSize() {
			return newList.size();
			}
			
			@Override
			public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			// Check if they refer to the same Message object
			return oldList.get(oldItemPosition).message.equals(newList.get(newItemPosition).message);
			}
			
			@Override
			public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			// Check if all displayed content is the same
			MessageWrapper oldItem = oldList.get(oldItemPosition);
			MessageWrapper newItem = newList.get(newItemPosition);
			
			return oldItem.host.equals(newItem.host) && oldItem.path.equals(newItem.path)
				&& oldItem.method.equals(newItem.method) && oldItem.status.equals(newItem.status)
				&& oldItem.length == newItem.length && oldItem.mime.equals(newItem.mime);
			}
			
			}
			
			public void cleanUoMemory() {
			allMessages.clear();
			executorService.shutdown();
			filteredMessages.clear();
			}
			}*/

public class HttpRecyclerAdapter
		extends ListAdapter<Message, HttpRecyclerAdapter.ViewHolder> {
	private final Context context;
	private final List<Message> allMessages = new ArrayList<>();

	private List<Message> filteredMessages = new ArrayList<>();
	private Function<Message, Comparable> currentSortingKey = this::extractStatusCode;
	private boolean currentSortAscending = true;

	private boolean isScopeApplied = false;
	private final Set<String> scopeItems = new HashSet<>();
	private final Set<Integer> statusCodeFilters = new HashSet<>();

	private final Map<String, String> domainCache = new HashMap<>();

	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler uiHandler = new Handler(Looper.getMainLooper());
	private final Map<String, Integer> messageKeyToIndex = new HashMap<>();
	static final MessageDiffCallback f = new MessageDiffCallback();

	private final Map<Integer, Message> pendingMessages = new ConcurrentHashMap<>();

	public HttpRecyclerAdapter(Context c) {

		super(f);
		this.context = c;
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		RecyclerLayoutHistoryBinding binding;

		public ViewHolder(@NonNull RecyclerLayoutHistoryBinding binding) {
			super(binding.getRoot());
			this.binding = binding;
		}
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(parent.getContext());
		RecyclerLayoutHistoryBinding binding = RecyclerLayoutHistoryBinding.inflate(inflater, parent, false);
		return new ViewHolder(binding);
	}

	private OnItemClickListener onItemClickListener;

	public interface OnItemClickListener {
		void onItemClick(int position);
	}

	public void setOnItemClickListener(OnItemClickListener listener) {
		this.onItemClickListener = listener;
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		Message message = filteredMessages.get(position);

		String url = "-", p = "-", m = "-", s = "-", contentType = "-";
		int l = 0;
		Integer sc = null;
		if (message instanceof Http1Message) {
			Http1Message httpMessage = (Http1Message) message;
			url = httpMessage.host() != null ? httpMessage.host() : "";
			p = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
					: "";
			m = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
					: "";
			if (httpMessage.responseHeader() != null) {
				sc = httpMessage.responseHeader().statusCode();
				s = String.valueOf(sc);
				contentType = httpMessage.MimeType();

				l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;
			}
		} else if (message instanceof Http2Message) {
			Http2Message httpMessage = (Http2Message) message;
			url = httpMessage.host() != null ? httpMessage.host() : "";
			p = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
					: "";
			m = httpMessage.requestHeader() != null
					? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
					: "";
			if (httpMessage.responseHeader() != null) {
				sc = httpMessage.responseHeader().statusCode();
				s = String.valueOf(sc);
				contentType = httpMessage.MimeType();
			}
			l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;

		}

		// Use pre-computed values  
		holder.binding.recyclerHostTextView.setText(url);
		holder.binding.recyclerMethodTextView.setText(m);
		holder.binding.recyclerPathTextView.setText(p);
		holder.binding.recyclerStatusTextView.setText(s);
		holder.binding.recyclerContentLengthTextView.setText("Length: " + sc);
		holder.binding.recyclerMimeTextView.setText("Mime: " + contentType);

		// Avoid creating a new OnClickListener for each bind  
		holder.itemView.setOnClickListener(v -> {
			if (holder.getAdapterPosition() != RecyclerView.NO_POSITION && onItemClickListener != null) {
				onItemClickListener.onItemClick(position);
			}
		});
	}

		public List<Message> getMessages() {

		return filteredMessages;
		}

	public void SortDesc() {
		currentSortAscending = false;
		//	sortFilteredMessages();
		rebuildFilteredList();
	}

	public void SortAsc() {
		currentSortAscending = true;
		rebuildFilteredList();
	}

	public void sortByLength() {
		currentSortingKey = this::extractResponseLength;
		rebuildFilteredList();
	}

	public void filterByStatusCodes(Set<Integer> allowedStatusCodes) {
		statusCodeFilters.clear();
		if (allowedStatusCodes != null) {
			statusCodeFilters.addAll(allowedStatusCodes);
		}
		sortFilteredMessages();
	}

	public void sortByMimeType() {
		currentSortingKey = this::extractMimeType;
		sortFilteredMessages();
	}

	private Integer extractResponseLength(Message message) {
		if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			return http2Message.responseBody() != null ? (int) http2Message.length() : 0;
		} else if (message instanceof Http1Message) {
			Http1Message http2Message = (Http1Message) message;
			return http2Message.responseBody() != null ? (int) http2Message.length() : 0;
		};
		return 0;
	}

	private String extractMimeType(Message message) {
		if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			if (http2Message.responseHeader() != null) {
				return http2Message.MimeType();
			}
		} else if (message instanceof Http1Message) {
			Http1Message http2Message = (Http1Message) message;
			if (http2Message.responseHeader() != null) {
				return http2Message.MimeType();
			}
		}
		return "";
	}

	public void refresh() {
		rebuildFilteredList();
	}

	public void sortByStatus() {
		currentSortingKey = this::extractStatusCode;
		rebuildFilteredList();
	}

	public void sortByMethod() {
		currentSortingKey = this::extractMethod;
		rebuildFilteredList();
	}

	private Integer extractStatusCode(Message message) {
		if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;
			if (http2Message.responseHeader() != null) {
				return http2Message.responseHeader().statusCode();
			}
		} else if (message instanceof Http1Message) {
			Http1Message http2Message = (Http1Message) message;
			if (http2Message.responseHeader() != null) {
				return http2Message.responseHeader().statusCode();
			}
		}
		return 0;

	}

	private String extractMethod(Message message) {
		if (message instanceof Http2Message) {
			Http2Message http2Message = (Http2Message) message;

			return http2Message.requestHeader() != null
					? (http2Message.requestHeader().method() != null ? http2Message.requestHeader().method() : "")
					: "";

		} else if (message instanceof Http1Message) {
			Http1Message http2Message = (Http1Message) message;

			return http2Message.requestHeader() != null
					? (http2Message.requestHeader().method() != null ? http2Message.requestHeader().method() : "")
					: "";
		}
		return "-";
	}

	/*	public void addMessage(Message message) {
			if (message == null)
				return;
	
			MessageWrapper wrapper = new MessageWrapper(message);
			allMessages.add(wrapper);
	
			// Only add to filtered list if it passes filters  
			String genericHost = Networks.genericMultiCDNS(message.host());
			boolean isInScope = !isScopeApplied || scopeItems.contains(wrapper.baseDomain);
			boolean matchesStatus = statusCodeFilters.isEmpty() || statusCodeFilters.contains(wrapper.statusCode);
	
			if (isInScope && matchesStatus) {
				filteredMessages.add(wrapper);
				//	rebuildFilteredList();
				sortFilteredMessages();
				//	notifyItemInserted(filteredMessages.size() - 1);
			}
	
		}*/
	public void addMessage(Message message) {
		if (message == null)
			return;
		
	//	MessageWrapper wrapper = new MessageWrapper(message);
		String genericHost = Networks.genericMultiCDNS(message.host());
		boolean isInScope = !isScopeApplied || scopeItems.contains(message.host());
		boolean matchesStatus = statusCodeFilters.isEmpty() || statusCodeFilters.contains(extractStatusCode(message));

		if (isInScope && matchesStatus) {
			allMessages.add(message);
		}

		// Rebuild & sort filtered list then submit to adapter
		rebuildFilteredList(); // This will do submitList internally
	}

	private void rebuildFilteredList() {

		List<Message> newFiltered = allMessages.stream().filter(wrapper -> {
			String genericHost = Networks.genericMultiCDNS(wrapper.host());
			;
			boolean isInScope = !isScopeApplied || scopeItems.contains(getBaseDomain(genericHost))
					|| scopeItems.contains(wrapper.host());
			boolean matchesStatus = statusCodeFilters.isEmpty() || statusCodeFilters.contains(extractStatusCode(wrapper));
			return isInScope && matchesStatus;
		}).collect(Collectors.toList());

		newFiltered.sort((m1, m2) -> {
			Comparable key1 = currentSortingKey.apply(m1);
			Comparable key2 = currentSortingKey.apply(m2);
			if (key1 == null && key2 == null)
				return 0;
			if (key1 == null)
				return currentSortAscending ? 1 : -1;
			if (key2 == null)
				return currentSortAscending ? -1 : 1;
			return currentSortAscending ? key1.compareTo(key2) : key2.compareTo(key1);
		});

		// Apply the new filtered+sorted list
		filteredMessages = newFiltered;
		submitList(new ArrayList<>(filteredMessages));
	}

	private void sortFilteredMessages() {
		filteredMessages.sort((m1, m2) -> {
			Comparable key1 = currentSortingKey.apply(m1);
			Comparable key2 = currentSortingKey.apply(m2);

			if (key1 == null && key2 == null)
				return 0;
			if (key1 == null)
				return currentSortAscending ? 1 : -1;
			if (key2 == null)
				return currentSortAscending ? -1 : 1;

			return currentSortAscending ? key1.compareTo(key2) : key2.compareTo(key1);
		});
	}

	private String getBaseDomain(String host) {
		if (host == null || host.isEmpty())
			return "";

		// Check cache first  
		String cachedDomain = domainCache.get(host);
		if (cachedDomain != null) {
			return cachedDomain;
		}

		// Compute and cache  
		String result;
		try {
			Uri uri = Uri.parse("http://" + host);
			String domain = uri.getHost();

			if (domain == null) {
				result = host;
			} else {
				String[] parts = domain.split("\\.");
				if (parts.length >= 2) {
					result = parts[parts.length - 2] + "." + parts[parts.length - 1];
				} else {
					result = domain;
				}
			}
		} catch (Exception e) {
			// Fallback for any parsing issues  
			String[] parts = host.split("\\.");
			if (parts.length >= 2) {
				result = parts[parts.length - 2] + "." + parts[parts.length - 1];
			} else {
				result = host;
			}
		}

		domainCache.put(host, result);
		return result;
	}

	public int size() {
		return allMessages.size();
	}

	public void applyScope(boolean scope) {
		isScopeApplied = scope;
		//sortFilteredMessages();
		rebuildFilteredList();
	}

	public void clearScope() {
		scopeItems.clear();
		isScopeApplied = false;
		//sortFilteredMessages();
		rebuildFilteredList();
	}

	public boolean isScopeApplied() {
		return isScopeApplied;
	}

	public boolean isScopeEmpty() {
		return scopeItems.isEmpty();
	}

	public void addToScope(String item) {
		scopeItems.add(item);
	}

	@Override
	public int getItemCount() {
		return getCurrentList().size();
	}

	// Cache computed values for each Message  
	public class MessageWrapper {
		final Message message;
		final String host;
		final String path;
		final String method;
		final String status;
		final Integer statusCode;
		final int length;
		final String lengthStr;
		final String mime;
		final String baseDomain;
		long id;

		MessageWrapper(Message message) {
			this.message = message;

			// Extract all values once  
			String url = "-", p = "-", m = "-", s = "-", contentType = "-";
			int l = 0;
			Integer sc = null;

			if (message instanceof Http1Message) {
				Http1Message httpMessage = (Http1Message) message;
				url = httpMessage.host() != null ? httpMessage.host() : "";
				p = httpMessage.requestHeader() != null
						? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
						: "";
				m = httpMessage.requestHeader() != null
						? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
						: "";
				if (httpMessage.responseHeader() != null) {
					sc = httpMessage.responseHeader().statusCode();
					s = String.valueOf(sc);
					contentType = httpMessage.MimeType();

					l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;
				}
			} else if (message instanceof Http2Message) {
				Http2Message httpMessage = (Http2Message) message;
				url = httpMessage.host() != null ? httpMessage.host() : "";
				p = httpMessage.requestHeader() != null
						? (httpMessage.requestHeader().path() != null ? httpMessage.requestHeader().path() : "")
						: "";
				m = httpMessage.requestHeader() != null
						? (httpMessage.requestHeader().method() != null ? httpMessage.requestHeader().method() : "")
						: "";
				if (httpMessage.responseHeader() != null) {
					sc = httpMessage.responseHeader().statusCode();
					s = String.valueOf(sc);
					contentType = httpMessage.MimeType();
				}
				l = httpMessage.responseBody() != null ? (int) httpMessage.length() : 0;

			}

			this.host = url;
			this.path = p;
			this.method = m;
			this.status = s;
			this.statusCode = sc;
			this.length = l;
			this.lengthStr = String.valueOf(l); // Pre-compute string representation  
			this.mime = contentType != null ? contentType : "";
			this.id = (host + path + url).hashCode();
			// Pre-compute base domain  
			String genericHost = Networks.genericMultiCDNS(url);
			this.baseDomain = getBaseDomain(genericHost);

		}

		public long getId() {
			return id;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null || getClass() != obj.getClass())
				return false;

			MessageWrapper that = (MessageWrapper) obj;
			return message.equals(that.message);
		}

		@Override
		public int hashCode() {
			return message.hashCode();
		}
	}

	public static class MessageDiffCallback extends DiffUtil.ItemCallback<Message> {
		@Override
		public boolean areItemsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
			//
			return oldItem.hashCode()==newItem.hashCode();
		}

		@Override
		public boolean areContentsTheSame(@NonNull Message oldItem, @NonNull Message newItem) {
			// Deep equality
			return oldItem.hashCode() == newItem.hashCode();
		}
	}

	// DiffUtil callback implementation  
	/*private static class MessageDiffCallback extends DiffUtil.Callback {
		private final List<MessageWrapper> oldList;
		private final List<MessageWrapper> newList;
	
		MessageDiffCallback(List<MessageWrapper> oldList, List<MessageWrapper> newList) {
			this.oldList = oldList;
			this.newList = newList;
		}
	
		@Override
		public int getOldListSize() {
			return oldList.size();
		}
	
		@Override
		public int getNewListSize() {
			return newList.size();
		}
	
		@Override
		public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
			// Check if they refer to the same Message object  
			return oldList.get(oldItemPosition).message.equals(newList.get(newItemPosition).message);
		}
	
		@Override
		public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
			// Check if all displayed content is the same  
			MessageWrapper oldItem = oldList.get(oldItemPosition);
			MessageWrapper newItem = newList.get(newItemPosition);
	
			return oldItem.host.equals(newItem.host) && oldItem.path.equals(newItem.path)
					&& oldItem.method.equals(newItem.method) && oldItem.status.equals(newItem.status)
					&& oldItem.length == newItem.length && oldItem.mime.equals(newItem.mime);
		}
	}
	
	public void cleanUoMemory() {
		allMessages.clear();
		executorService.shutdown();
		filteredMessages.clear();
	}
	
	public interface MessageComplete {
	
		void onResponseReady();
	
	}*/

	/*	public void addAll(List<Message> newMessages) {
			if (newMessages == null || newMessages.isEmpty())
				return;
			executorService.execute(() -> {
	
				List<MessageWrapper> newWrappers = newMessages.stream().map(MessageWrapper::new)
						.collect(Collectors.toList());
	
				DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new MessageDiffCallback(allMessages, newWrappers));
	
				allMessages.addAll(newWrappers);
	
				rebuildFilteredList();
	
				//	diffResult.dispatchUpdatesTo(this);  
			});
		}
	*/
	/*	private void sortFilteredMessages() {
		filteredMessages.sort((m1, m2) -> {
			Comparable key1 = currentSortingKey.apply(m1);
			Comparable key2 = currentSortingKey.apply(m2);
	
			if (key1 == null && key2 == null)
				return 0;
			if (key1 == null)
				return currentSortAscending ? 1 : -1;
			if (key2 == null)
				return currentSortAscending ? -1 : 1;
	
			return currentSortAscending ? key1.compareTo(key2) : key2.compareTo(key1);
		});
	
		notifyDataSetChanged();
	}
	*/

	/*	public void addMessage(Message message) {
			if (message == null)
				return;
	
			String key = getMessageKey(message);
			MessageWrapper wrapper = new MessageWrapper(message, () -> {
				MessageWrapper wrappe = new MessageWrapper(message, null);
				allMessages.add(wrappe);
	
				String genericHost = Networks.genericMultiCDNS(message.host());
				boolean isInScope = !isScopeApplied || scopeItems.contains(wrappe.baseDomain);
				boolean matchesStatus = statusCodeFilters.isEmpty() || statusCodeFilters.contains(wrappe.statusCode);
	
				if (isInScope && matchesStatus) {
					filteredMessages.add(wrappe);
					int index = filteredMessages.size() - 1;
					messageKeyToIndex.put(key, index);
					notifyItemInserted(index);
	
				}
			});
	
		}*/
}
