package com.proxy.service;

import android.app.Application;

import android.app.NotificationChannel;
import android.os.Build;
import android.app.NotificationManager;
import android.os.IBinder;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import com.proxy.AppContext;
import com.proxy.R;
import com.proxy.MyApp;

import com.proxy.ViewModel.SharedViewModel;
import androidx.core.app.NotificationCompat;
import com.proxy.data.Message;
import com.proxy.listener.SetLogger;
import com.proxy.ui.MainRequestRepository;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import com.proxy.netty.Server;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;

public class ProxyService extends Service {

    private ExecutorService executorService;
    private Server server;

    @Override
    public void onCreate() {
        super.onCreate();
        executorService = Executors.newSingleThreadExecutor();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, "proxy_channel")
                .setContentTitle("Proxy Running")
                .setContentText("Your proxy is active")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();

        startForeground(1, notification);

        
		AppContext context = new AppContext(getApplicationContext());
        executorService.submit(() -> {
            server = new Server(context.getServerSetting(), context.getSslContextManager(),
                    context.getProxySetting(), message -> {
                        MainRequestRepository.getInstance().addToMainRequests(message);
						
                    });
            try {
                server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "proxy_channel",
                    "Proxy Service",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Keeps the proxy service running");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (server != null) {
            server.stop();
        }
        executorService.shutdownNow();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}