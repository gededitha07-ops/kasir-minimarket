
package id.kasirminimarket.app;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.WebViewClient;
import android.webkit.WebChromeClient;
import android.webkit.ValueCallback;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import java.io.OutputStream;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends Activity {

    private WebView webView;
    private ValueCallback<Uri[]> fileCallback;
    private static final int REQUEST_RESTORE = 100;
    private static final int REQUEST_BACKUP = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);

        webView.setWebViewClient(new WebViewClient());
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(
                WebView view,
                ValueCallback<Uri[]> callback,
                FileChooserParams params
            ) {
                if (fileCallback != null) {
                    fileCallback.onReceiveValue(null);
                }

                fileCallback = callback;

                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("application/json");

                startActivityForResult(intent, REQUEST_RESTORE);
                return true;
            }
        });

        webView.addJavascriptInterface(this, "Android");
        setContentView(webView);
        webView.loadUrl("file:///android_asset/index.html");
    }

    @JavascriptInterface
    public void simpanBackup(String isi) {
        runOnUiThread(() -> {
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "backup-kasir-minimarket.json");

            backupContent = isi;
            startActivityForResult(intent, REQUEST_BACKUP);
        });
    }

    private String backupContent = "";

    @Override
    protected void onActivityResult(
        int requestCode, int resultCode, Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RESTORE) {
            if (fileCallback != null) {
                Uri[] results = null;

                if (resultCode == RESULT_OK && data != null) {
                    results = new Uri[]{data.getData()};
                }

                fileCallback.onReceiveValue(results);
                fileCallback = null;
            }

            if (resultCode == RESULT_OK && data != null) {
                bacaFile(data.getData());
            }
        }

        if (requestCode == REQUEST_BACKUP &&
            resultCode == RESULT_OK && data != null) {

            try {
                Uri uri = data.getData();
                OutputStream output = getContentResolver()
                    .openOutputStream(uri);

                output.write(backupContent.getBytes("UTF-8"));
                output.close();

                Toast.makeText(
                    this,
                    "Backup berhasil disimpan",
                    Toast.LENGTH_LONG
                ).show();

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Gagal menyimpan backup",
                    Toast.LENGTH_LONG
                ).show();
            }
        }
    }

    private void bacaFile(Uri uri) {
        try {
            InputStream input = getContentResolver()
                .openInputStream(uri);

            ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

            byte[] data = new byte[4096];
            int jumlah;

            while ((jumlah = input.read(data)) != -1) {
                buffer.write(data, 0, jumlah);
            }

            input.close();

            String isi = buffer.toString("UTF-8");

            String hasil = isi
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\n", "\\n")
                .replace("\r", "");

            webView.post(() -> webView.evaluateJavascript(
                "restoreDariAndroid('" + hasil + "')",
                null
            ));

        } catch (Exception e) {
            Toast.makeText(
                this,
                "Gagal membaca file backup",
                Toast.LENGTH_LONG
            ).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
