package com.poolyar.app;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import android.content.ContentValues;
import android.os.Environment;
import android.widget.Toast;
import android.util.Base64;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient.FileChooserParams;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;

public class MainActivity extends Activity {
    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int FILE_CHOOSER = 2201;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(16,75,61));
        getWindow().setNavigationBarColor(Color.rgb(16,75,61));
        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(243,246,244));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setDatabaseEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");
                try { startActivityForResult(intent, FILE_CHOOSER); return true; }
                catch (Exception ex) { fileCallback = null; return false; }
            }
        });
        webView.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveFile(String filename, String mime, String base64) {
                runOnUiThread(() -> {
                    try {
                        byte[] data = Base64.decode(base64, Base64.DEFAULT);
                        android.content.ContentResolver resolver = getContentResolver();
                        ContentValues values = new ContentValues();
                        values.put(android.provider.MediaStore.Downloads.DISPLAY_NAME, filename);
                        values.put(android.provider.MediaStore.Downloads.MIME_TYPE, mime);
                        if (android.os.Build.VERSION.SDK_INT >= 29) {
                            values.put(android.provider.MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                            Uri uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                            if (uri != null) {
                                try (java.io.OutputStream out = resolver.openOutputStream(uri)) { if (out != null) out.write(data); }
                                Toast.makeText(MainActivity.this, "نسخه پشتیبان در پوشه دانلود ذخیره شد", Toast.LENGTH_LONG).show();
                            }
                        }
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "ذخیره فایل انجام نشد", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }, "PoolYarAndroid");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FILE_CHOOSER && fileCallback != null) {
            Uri[] results = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) results = new Uri[]{data.getData()};
            fileCallback.onReceiveValue(results);
            fileCallback = null;
        }
    }
    @Override public void onBackPressed() {
        if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
}
