package com.proxy.ui;

import com.proxy.data.Message;
import com.proxy.listener.SetLogger;
import java.util.ArrayList;
import java.util.List;
import androidx.lifecycle.MutableLiveData;

public class MainRequestRepository {

	private static final MainRequestRepository instance = new MainRequestRepository();
	private static MutableLiveData<List<Message>> mainRequests = new MutableLiveData<>(new ArrayList<>());

	private MainRequestRepository() {
	}

	public static MainRequestRepository getInstance() {

		return instance;
	}

	public MutableLiveData<List<Message>> getMainRequests() {
		return mainRequests;
	}

	public void addToMainRequests(Message message) {
		List<Message> currentMainRequests = mainRequests.getValue();

		if (currentMainRequests != null) {
			currentMainRequests.add(message);
			mainRequests.postValue(currentMainRequests);
			
		} 
	}

}
