package com.proxy;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.os.Looper;
import android.os.Handler;
import android.provider.MediaStore;
import android.net.Uri;
import android.util.Log;
import com.proxy.data.Message;
import com.proxy.setting.Settings;
import com.proxy.ssl.KeyStoreGenerator;
import com.proxy.ssl.RootKeyStoreGenerator;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import android.widget.Toast;
import com.proxy.setting.KeyStoreSetting;
import com.proxy.setting.ServerSetting;
import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

/**
 * `InitContext` handles the initialization of the application context,
 * specifically dealing with the creation and storage of a root certificate.
 * It uses an ExecutorService to perform these operations asynchronously.
 */

/*
public class InitContext {
	private final Context context;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	public InitContext(Context context) {
		this.context = context;
	}

	public void init() {
		executorService.execute(() -> {
			try {
				// Check if the directory already exists
				if (!isDirectoryExists("JPX")) {

					AppContext appContext = new AppContext(context);
					KeyStoreGenerator generator = appContext.getSslContextManager().getKeyStoreGenerator();
					byte[] data = generator.exportRootCert(false);
					saveCertificateToExternalStorage("JPX", data);

				}
			} catch (Exception e) {
				Log.e("InitContext", "Error during initialization", e);
				showToast("Failed to initalize");
			}
		});
	}

	private boolean isDirectoryExists(String directoryName) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
		String[] selectionArgs = new String[] { Environment.DIRECTORY_DOCUMENTS + "/" + directoryName + "/" };

		try (Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
			return cursor != null && cursor.getCount() > 0;
		}
	}

	public void saveCertToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> {
			saveToExternalStorage(fileName + ".p12", data, "application/x-pkcs12",
					Environment.DIRECTORY_DOCUMENTS + "/JPX");
		});
	}

	public void saveCertificateToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> {
			saveToExternalStorage(fileName + ".crt", data, "application/x-x509-ca-cert",
					Environment.DIRECTORY_DOCUMENTS + "/JPX");
		});
	}

	public void saveExportedData(String fileName, String data) {
		executorService.execute(() -> saveTextToExternalStorage(fileName + ".txt", data, "text/plain",
				Environment.DIRECTORY_DOCUMENTS + "/JPX"));
	}

	private void saveTextToExternalStorage(String fileName, String data, String mimeType, String directory) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME
				+ "=?";
		String[] selectionArgs = { directory, fileName };

		

		// Insert new file metadata
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		contentValues.put(MediaStore.MediaColumns.TITLE, "Exported Data");
		contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
		contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());

		Uri uri = context.getContentResolver().insert(contentUri, contentValues);
		if (uri != null) {
			try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
					OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
				writer.write(data);
				showToast("Data exported successfully to external storage.");
			} catch (IOException e) {
				Log.e("InitContext", "Error writing file to external storage", e);
				showToast("Error while writing file to external storage.");
			}
		} else {
			showToast("Failed to insert content values into MediaStore.");
		}
	}

	private void saveToExternalStorage(String fileName, byte[] data, String mimeType, String directory) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=? AND " + MediaStore.MediaColumns.DISPLAY_NAME
				+ "=?";
		String[] selectionArgs = { directory, fileName };

		// Check if the file already exists
		try (Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
			if (cursor != null && cursor.getCount() > 0) {
				showToast("File already exists in external storage.");
				return;
			}
		}

		// Create the file
		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		contentValues.put(MediaStore.MediaColumns.TITLE, "Cert File");
		contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
		contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());

		Uri uri = context.getContentResolver().insert(contentUri, contentValues);
		if (uri != null) {
			try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
				if (outputStream != null) {
					outputStream.write(data);
					showToast("File saved successfully to external storage.");
				} else {
					showToast("Failed to open output stream.");
				}
			} catch (IOException e) {
				Log.e("InitContext", "Error writing file to external storage", e);
				showToast("Error while writing file to external storage.");
			}
		} else {
			showToast("Failed to insert content values into MediaStore.");
		}
	}

	private void showToast(String message) {
		mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
	}
}*/
public class InitContext {
	private final Context context;
	private final ExecutorService executorService = Executors.newSingleThreadExecutor();
	private final Handler mainHandler = new Handler(Looper.getMainLooper());

	public InitContext(Context context) {
		this.context = context;
	}

	public void init() {
		executorService.execute(() -> {
			try {
				// Check if the directory exists
				if (!isDirectoryExists("JPX")) {
					AppContext appContext = new AppContext(context);
					KeyStoreGenerator generator = appContext.getSslContextManager().getKeyStoreGenerator();
					byte[] data = generator.exportRootCert(true);
					saveCertificateToExternalStorage("JPX", data);
				}
			} catch (Exception e) {
				Log.e("InitContext", "Error during initialization", e);
				showToast("Failed to initialize");
			}
		});
	}

	private boolean isDirectoryExists(String directoryName) {
		Uri contentUri = MediaStore.Files.getContentUri("external");
		String selection = MediaStore.MediaColumns.RELATIVE_PATH + "=?";
		String[] selectionArgs = { Environment.DIRECTORY_DOCUMENTS + "/" + directoryName + "/" };

		try (Cursor cursor = context.getContentResolver().query(contentUri, null, selection, selectionArgs, null)) {
			return cursor != null && cursor.moveToFirst(); // Safer way to check if the directory exists
		} catch (Exception e) {
			Log.e("InitContext", "Error checking directory existence", e);
			return false;
		}
	}

	public void saveCertToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> saveToExternalStorage(fileName + ".p12", data, "application/x-pkcs12",
				Environment.DIRECTORY_DOCUMENTS + "/JPX"));
	}

	public void saveCertificateToExternalStorage(String fileName, byte[] data) {
		executorService.execute(() -> saveToExternalStorage(fileName + ".crt", data, "application/x-x509-ca-cert",
				Environment.DIRECTORY_DOCUMENTS + "/JPX"));
	}

	public void saveExportedData(String fileName, String data) {
		executorService.execute(() -> saveTextToExternalStorage(fileName + ".txt", data, "text/plain",
				Environment.DIRECTORY_DOCUMENTS + "/JPX"));
	}

	private void saveTextToExternalStorage(String fileName, String data, String mimeType, String directory) {
		Uri contentUri = MediaStore.Files.getContentUri("external");

		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		contentValues.put(MediaStore.MediaColumns.TITLE, "Exported Data");
		contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
		contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());

		Uri uri = context.getContentResolver().insert(contentUri, contentValues);
		if (uri != null) {
			try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
				if (outputStream != null) {
					try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
						writer.write(data);
						showToast("Data exported successfully to external storage.");
					}
				} else {
					showToast("Failed to open output stream.");
				}
			} catch (IOException e) {
				Log.e("InitContext", "Error writing text file to external storage", e);
				showToast("Error while writing file to external storage.");
			}
		} else {
			showToast("Failed to insert content values into MediaStore.");
		}
	}

	

	private void saveToExternalStorage(String fileName, byte[] data, String mimeType, String directory) {
		Uri contentUri = MediaStore.Files.getContentUri("external");

		ContentValues contentValues = new ContentValues();
		contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
		contentValues.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
		contentValues.put(MediaStore.MediaColumns.TITLE, "Cert File");
		contentValues.put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000);
		contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, directory);
		contentValues.put(MediaStore.MediaColumns.DATE_TAKEN, System.currentTimeMillis());

		Uri uri = context.getContentResolver().insert(contentUri, contentValues);
		if (uri != null) {
			try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
				if (outputStream != null) {
					outputStream.write(data);
					showToast("File saved successfully to external storage.");
				} else {
					showToast("Failed to open output stream.");
				}
			} catch (IOException e) {
				Log.e("InitContext", "Error writing file to external storage", e);
				showToast("Error while writing file to external storage.");
			}
		} else {
			showToast("Failed to insert content values into MediaStore.");
		}
	}

	private void showToast(String message) {
		mainHandler.post(() -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show());
	}

	// Safely shut down executor service when it's no longer needed
	public void shutdown() {
		executorService.shutdown();
	}
}
