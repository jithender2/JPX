package com.proxy.ui.Adapter;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.SparseArray;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.PopupMenu;

import com.proxy.R;

import com.proxy.data.HttpMessage;
import com.proxy.data.Message;

import com.proxy.listener.SetLogger;
import com.proxy.ui.TreeNode;
import java.lang.ref.WeakReference;
import com.proxy.utils.Networks;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

public class CustomTreeAdapter extends BaseExpandableListAdapter {
	private WeakReference<Context> context;
	private final HashMap<String, List<Message>> expandableDetailList = new HashMap<>();
	private final List<String> expandableTitleList = new ArrayList<>();

	private final Set<String> scopeItems = new HashSet<>(); // Faster lookup
	private boolean isScopeApplied = false;
	private List<Message> allMessages = new ArrayList<>(); // Keep a master list
	private final Handler uiHandler = new Handler(Looper.getMainLooper());
boolean isNewGroup;
	private final Runnable notifyRunnable = this::notifyDataSetChanged;

	public void addNewMessage(Message message) {
		if (message == null)
			return;
			if(allMessages.contains(message))return;
	
		allMessages.add(message);
		String genericHost = Networks.genericMultiCDNS(message.host());
		addMessageToData(genericHost, message);
	
		if (isScopeApplied) {
			rebuildLists(); // still safe to batch, see below
		} else {
			uiHandler.removeCallbacks(notifyRunnable); // debounce
			uiHandler.post(notifyRunnable);
		}
	}
	public CustomTreeAdapter(Context context) {
		this.context = new WeakReference<>(context);

	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		String groupTitle = expandableTitleList.get(groupPosition);
		return expandableDetailList.get(groupTitle).get(childPosition);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	/*	@Override
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View view, ViewGroup parent) {
			if (!(getChild(groupPosition, childPosition) instanceof Message)) {
	
				return null;
			}
	
			String displayText = ((Message) getChild(groupPosition, childPosition)).displayText();
			if (view == null) {
				LayoutInflater infalInflater = (LayoutInflater) getContext()
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = infalInflater.inflate(R.layout.tree_item, null);
			}
			TextView childItem = (TextView) view.findViewById(R.id.tree_item_textview);
			childItem.setText(displayText);
	
			return view;
		}
	*/
	private Context getContext() {
		return context.get();
	}

	@Override
	public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView,
			ViewGroup parent) {
		if (!(getChild(groupPosition, childPosition) instanceof Message)) {
			return null;
		}

		ChildViewHolder holder;

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.tree_item, parent, false);
			holder = new ChildViewHolder();
			holder.childTextView = convertView.findViewById(R.id.tree_item_textview);
			convertView.setTag(holder);
		} else {
			holder = (ChildViewHolder) convertView.getTag();
		}

		Message message = (Message) getChild(groupPosition, childPosition);
		holder.childTextView.setText(message.displayText());

		return convertView;
	}

	static class ChildViewHolder {
		TextView childTextView;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
		GroupViewHolder holder;

		if (convertView == null) {
			LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = inflater.inflate(R.layout.tree_list, parent, false);
			holder = new GroupViewHolder();
			holder.groupTextView = convertView.findViewById(R.id.tree_list_textview);
			holder.moreIcon = convertView.findViewById(R.id.tree_list_more);
			convertView.setTag(holder);
		} else {
			holder = (GroupViewHolder) convertView.getTag();
		}

		String headerInfo = (String) getGroup(groupPosition);
		holder.groupTextView.setText(headerInfo);

		holder.moreIcon.setOnClickListener(v -> {
			ShowPopUpMenu(getContext(), holder.moreIcon, headerInfo);
		});

		return convertView;
	}

	static class GroupViewHolder {
		TextView groupTextView;
		ImageView moreIcon;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		String groupTitle = expandableTitleList.get(groupPosition);
		if (expandableDetailList.get(groupTitle) == null) {
			return 0;
		}
		return expandableDetailList.get(groupTitle).size();

	}

	@Override
	public Object getGroup(int groupPosition) {
		return expandableTitleList.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return expandableTitleList.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		if (groupPosition == expandableTitleList.size()) {
			groupPosition -= 1;

		}
		return groupPosition;
	}

	/*	@Override
		public View getGroupView(int groupPosition, boolean isLastChild, View view, ViewGroup parent) {
	
			String headerInfo = (String) getGroup(groupPosition);
			if (view == null) {
				LayoutInflater inf = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				view = inf.inflate(R.layout.tree_list, null);
			}
	
			TextView heading = (TextView) view.findViewById(R.id.tree_list_textview);
			heading.setText(headerInfo);
			ImageView imageView = view.findViewById(R.id.tree_list_more);
			imageView.setOnClickListener(v -> {
				ShowPopUpMenu(getContext(), imageView, headerInfo);
			});
	
			return view;
		}*/

	/*
		public void addNewMessage(Message message) {
			if (message == null) {
				return;
			}
	
			allMessages.add(message); // Add to master list
			String genericHost = Networks.genericMultiCDNS(message.host());
			addMessageToData(genericHost, message); // Add to displayed lists (initially)
	
			if (isScopeApplied && !scopeItems.contains(getBaseDomain(genericHost))) {
				removeHostFromData(genericHost); // If scope is applied and host is NOT in scope remove it
			}
	
			notifyDataSetChanged();
		}*/
	/*	public void addNewMessage(Message message) {
			if (message == null)
				return;
	
			allMessages.add(message);
			String genericHost = Networks.genericMultiCDNS(message.host());
			addMessageToData(genericHost, message);
	
			// Instead of checking per message, batch process at the end
			if (isScopeApplied) {
				rebuildLists();
				//	rebuildLists(); // Scope filtering applied in one go
			} else {
				notifyDataSetChanged();
			}
		}
	*/
	private void addMessageToData(String genericHost, Message message) {
		if (!expandableDetailList.containsKey(genericHost)) {
			expandableTitleList.add(genericHost);
			expandableDetailList.put(genericHost, new ArrayList<>());
		}
		List<Message> messages = expandableDetailList.get(genericHost);
		if (!messages.contains(message)) {
			messages.add(message);
		}
	}

	public void applyScope(boolean scope) {
		if (!scopeItems.isEmpty()) {
			isScopeApplied = scope;
			rebuildLists(); // Crucial: Rebuild the lists
		}
	}

	/*	private void rebuildLists() {
			expandableDetailList.clear();
			expandableTitleList.clear();
	
			for (Message message : allMessages) {
				String genericHost = Networks.genericMultiCDNS(message.host());
				if (!isScopeApplied || scopeItems.contains(getBaseDomain(genericHost))) {
					addMessageToData(genericHost, message);
				}
			}
	
			notifyDataSetChanged();
		}*/

		private void rebuildLists() {
			expandableDetailList.clear();
			expandableTitleList.clear();
	
			for (Message message : allMessages) {
				String genericHost = Networks.genericMultiCDNS(message.host());
	
				if (!isScopeApplied || scopeItems.contains(genericHost)
						|| scopeItems.contains(getBaseDomain(genericHost))) {
					addMessageToData(genericHost, message);
				} // else if (!isScopeApplied || )) {
					//	addMessageToData(genericHost, message);
					//	}
			}
	
			uiHandler.removeCallbacks(notifyRunnable); // debounce
			uiHandler.post(notifyRunnable);
		} 
		

	

	/*	private String getBaseDomain(String host) {
			if (host == null || host.isEmpty())
				return "";
	
			try {
				URI uri = new URI("http://" + host); // Ensure valid format
				String domain = uri.getHost();
				if (domain == null)
					return host;
	
				String[] parts = domain.split("\\.");
				if (parts.length >= 3) {
					return parts[parts.length - 2] + "." + parts[parts.length - 1]; // Extract base domain
				}
				return domain;
			} catch (URISyntaxException e) {
				return extractManually(host); // Use fallback method
			}
		}*/

	private String getBaseDomain(String host) {
		if (host == null || host.isEmpty())
			return "";

		// Remove port if present
		int colonIndex = host.indexOf(':');
		if (colonIndex != -1) {
			host = host.substring(0, colonIndex);
		}

		// Remove trailing dot if present
		if (host.endsWith(".")) {
			host = host.substring(0, host.length() - 1);
		}

		String[] parts = host.split("\\.");
		int length = parts.length;

		if (length >= 2) {
			// Handle cases like co.uk, com.au, etc. (simple TLD check)
			String last = parts[length - 1];
			String secondLast = parts[length - 2];
			if (isSecondLevelTLD(secondLast + "." + last) && length >= 3) {
				return parts[length - 3] + "." + secondLast + "." + last;
			}
			return secondLast + "." + last;
		}

		return host;
	}

	// Minimal list of second-level TLDs for demo purposes. Extend as needed.
	private boolean isSecondLevelTLD(String tld) {
		switch (tld) {
		case "co.uk":
		case "com.au":
		case "co.in":
		case "gov.uk":
			return true;
		default:
			return false;
		}
	}

	// Fallback if URI parsing fails
	private String extractManually(String host) {
		String[] parts = host.split("\\.");
		if (parts.length >= 3) {
			return parts[parts.length - 2] + "." + parts[parts.length - 1]; // Extract base domain
		}
		return host; // Return as-is if no better alternative
	}

	public void clearScope() {
		scopeItems.clear();
		isScopeApplied = false;
		//	executorService.execute(this::rebuildLists);
		rebuildLists(); // Rebuild to show all messages
	}

	/*	public void clearScope() {
			scopeItems.clear();
			isScopeApplied = false;
		}
	*/
	public boolean isScopeApplied() {
		return isScopeApplied;
	}

	public List<String> getGroupList() {
		return expandableTitleList;
	}

	public HashMap<String, List<Message>> getItemList() {
		return expandableDetailList;
	}

	public boolean isScopeEmpty() {
		return scopeItems.isEmpty();
	}

	public void addToScope(String item) {
		scopeItems.add(item);

	}

	public void addLogMessage(String logMessage) {
		expandableTitleList.add(logMessage);
		notifyDataSetChanged();

	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}

	private void ShowPopUpMenu(Context context, ImageView more, String domain) {
		PopupMenu popUpMenu = new PopupMenu(context, more);
		popUpMenu.getMenuInflater().inflate(R.menu.adapter_menu, popUpMenu.getMenu());
		popUpMenu.setOnMenuItemClickListener(item -> {
			if (item.getItemId() == R.id.add_to_scope) {
				scopeItems.add(domain);

			}
			if (item.getItemId() == R.id.copy_to_clipboard) {
				ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
				ClipData clip = ClipData.newPlainText("domain", domain);
				clipboard.setPrimaryClip(clip);

			}

			return true;
		});

		popUpMenu.show();

	}

	public void cleanUpMemory() {
		uiHandler.removeCallbacks(notifyRunnable);
		allMessages.clear();
		scopeItems.clear();
		//executorService.shutdown();
		expandableTitleList.clear();
		expandableDetailList.clear();

	}

}