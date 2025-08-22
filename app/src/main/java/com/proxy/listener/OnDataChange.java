package com.proxy.listener;
import com.proxy.data.Message;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface OnDataChange {

	public void onDataExport(List<String> groups, HashMap<String, List<Message>> items);
	
}