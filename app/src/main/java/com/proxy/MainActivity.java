package com.proxy;

import android.app.Dialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Build;
import android.content.Intent;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.Menu;
import android.widget.EditText;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.activity.result.ActivityResultLauncher;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import androidx.fragment.app.FragmentPagerAdapter;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.android.material.tabs.TabLayout;
import com.proxy.data.Message;
import com.proxy.databinding.ActivityMainBinding;
import com.proxy.listener.MessageListener;
import com.proxy.listener.OnDataChange;
import com.proxy.netty.Server;
import com.proxy.ui.Fragments.HttpHistory;
import com.proxy.ui.Fragments.Intercept;
import com.proxy.ui.Fragments.Repeator;
import com.proxy.ui.Fragments.Target;
import com.proxy.ui.FuzzingActivity;
import java.io.IOException;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements OnDataChange {

	private static final String SELECTED_TAB_KEY = "selected_tab_key";
	private ActivityMainBinding binding;
	private ViewPagerAdapter adapter;
	private boolean isProcessingMenu = false;

	private final Map<Integer, Fragment> fragmentMap = new HashMap<>();

	private final ActivityResultLauncher<Intent> storagePermissionLauncher = registerForActivityResult(
			new ActivityResultContracts.StartActivityForResult(), result -> {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && Environment.isExternalStorageManager()) {
					showToast("Permission granted");
				} else {
					showToast("Permission denied");
				}
			});

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		binding = ActivityMainBinding.inflate(getLayoutInflater());
		setContentView(binding.getRoot());

		setSupportActionBar(binding.toolbar);

		initializeViewPagerAndTabs();
		askForPermissions();

		if (savedInstanceState != null) {
			int selectedTab = savedInstanceState.getInt(SELECTED_TAB_KEY, 0);
			binding.viewPager.setCurrentItem(selectedTab, false);
		}
	}

	private void initializeViewPagerAndTabs() {
		adapter = new ViewPagerAdapter(this);
		adapter.addFragment(getString(R.string.target), new Target(this));
		adapter.addFragment(getString(R.string.interceptor), new Intercept());
		adapter.addFragment(getString(R.string.repeater), new Repeator());
		adapter.addFragment(getString(R.string.http_history), new HttpHistory());

		binding.viewPager.setAdapter(adapter);
		binding.viewPager.setOffscreenPageLimit(2);

		new TabLayoutMediator(binding.tabLayout, binding.viewPager,
				(tab, position) -> tab.setText(adapter.getTitle(position))).attach();
	}

	public void askForPermissions() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
			Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
			intent.setData(Uri.parse("package:" + getPackageName()));
			storagePermissionLauncher.launch(intent);
		}
	}

	@Override
	protected void onSaveInstanceState(@NonNull Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt(SELECTED_TAB_KEY, binding.viewPager.getCurrentItem());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (isProcessingMenu)
			return true;
		isProcessingMenu = true;
		new Handler(Looper.getMainLooper()).postDelayed(() -> isProcessingMenu = false, 500);

		int id = item.getItemId();
		Target target = (Target) adapter.getFragment(0);
		HttpHistory history = (HttpHistory) adapter.getFragment(3);

		if (target == null || history == null) {
			showToast("Something went wrong initializing fragments");
			return super.onOptionsItemSelected(item);
		}

		switch (id) {
		case R.id.action_export_menu:
			target.exportData();
			break;
		case R.id.action_scope_menu:
			if (target.isScopeApplied()) {

				item.setTitle("Apply scope");
				target.clearScope();
				history.clearScope();
			} else if (!target.isScopeApplied()) {
				item.setTitle("Clear scope");
				target.applyScope();
				history.applyScope();
			}
			break;
		case R.id.add_scope_menu:

			showAlertDialogBox(domain -> {
				target.addToScope(domain);
				history.addToScope(domain);
			});
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
	}

	@Override
	public void onDataExport(List<String> groups, HashMap<String, List<Message>> items) {
		new Thread(() -> {
			StringBuilder fileContent = new StringBuilder();
			for (String group : groups) {
				fileContent.append(group).append("\n");
				List<Message> messages = items.get(group);
				if (messages != null) {
					for (Message message : messages) {
						fileContent.append(message.url()).append("\n");
					}
				}
				fileContent.append("\n");
			}
			String fileName = "exported_data_" + System.currentTimeMillis();
			InitContext context = new InitContext(getApplicationContext());
			context.saveExportedData(fileName, fileContent.toString());
		}).start();
	}

	private void showToast(String msg) {
		Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
	}

	private void showAlertDialogBox(OnDomainEnteredListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Enter domain");

		final EditText input = new EditText(this);
		input.setInputType(InputType.TYPE_CLASS_TEXT);
		builder.setView(input);

		builder.setPositiveButton("OK", (dialog, which) -> {
			String domain = input.getText().toString().trim();
			if (!domain.isEmpty()) {
				listener.onDomainEntered(domain);
			}
		});
		builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
		builder.show();
	}

	public interface OnDomainEnteredListener {
		void onDomainEntered(String domain);
	}

	public class ViewPagerAdapter extends FragmentStateAdapter {
		private final List<String> titles = new ArrayList<>();
		private final List<Fragment> fragmentList = new ArrayList<>();

		public ViewPagerAdapter(@NonNull FragmentActivity activity) {
			super(activity);
		}

		public void addFragment(String title, Fragment fragment) {
			titles.add(title);
			fragmentList.add(fragment);
			fragmentMap.put(fragmentList.size() - 1, fragment);
		}

		@NonNull
		@Override
		public Fragment createFragment(int position) {
			return fragmentList.get(position);
		}

		@Override
		public int getItemCount() {
			return fragmentList.size();
		}

		public Fragment getFragment(int position) {
			return fragmentMap.get(position);
		}

		public String getTitle(int position) {
			return position >= 0 && position < titles.size() ? titles.get(position) : "Untitled";
		}
	} 
	@Override
	protected void onDestroy() {
		super.onDestroy();
		fragmentMap.clear();
		adapter=null;
	}
	
}