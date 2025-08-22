package com.proxy.ViewModel;

import com.proxy.data.HttpMessage;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import com.proxy.data.Message;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.util.Set;

/**
 * `SharedViewModel` is a ViewModel class used to share data between different
 * fragments or activities within the application.  It holds LiveData objects
 * for repeater requests and main requests, allowing observers to react to changes
 * in these data.
 */
public class SharedViewModel extends ViewModel {
	/**
	 * LiveData object holding the latest HTTP message for repeater requests.
	 * Uses MutableLiveData so the value can be modified.
	 */
	private final MutableLiveData<HttpMessage> repeaterRequests = new MutableLiveData<>();

	private final MutableLiveData<List<Message>> mainRequests = new MutableLiveData<>(new ArrayList<>());
	

	public LiveData<HttpMessage> getRepeaterRequests() {
		return repeaterRequests;
	}

	public LiveData<List<Message>> getMainRequests() {
		  
		return mainRequests;
	}

	public void addToRepeater(HttpMessage message) {
		repeaterRequests.postValue(message);
	}

	public void addToMainRequests(Message message) {
		List<Message> currentMainRequests = mainRequests.getValue();
		
		if (currentMainRequests != null) {
			currentMainRequests.add(message);
			mainRequests.postValue(currentMainRequests);
		}
	}

	

}