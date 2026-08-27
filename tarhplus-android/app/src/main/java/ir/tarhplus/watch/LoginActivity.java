package ir.tarhplus.watch;

import android.annotation.SuppressLint;
import android.net.http.SslError;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;

public final class LoginActivity extends ComponentActivity {
    private WebView web;
    private TextView address;
    @SuppressLint("SetJavaScriptEnabled")
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        LinearLayout root = Ui.column(this);
        setContentView(root); Ui.insets(this, root);
        LinearLayout top = Ui.column(this); top.setPadding(Ui.dp(this, 14), Ui.dp(this, 10), Ui.dp(this, 14), 0); root.addView(top);
        address = Ui.text(this, top, "ورود فقط در سایت رسمی وزارت بهداشت", 14, true);
        Ui.text(this, top, "وارد حساب شو، گزارش اعلام نیاز را باز کن، سپس دکمه پایین را بزن. رمز را این برنامه دریافت نمی‌کند.", 12, false);
        web = new WebView(this); root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        WebSettings options = web.getSettings();
        options.setJavaScriptEnabled(true); options.setDomStorageEnabled(true);
        options.setAllowFileAccess(false); options.setAllowContentAccess(false);
        options.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        options.setSafeBrowsingEnabled(true); options.setSaveFormData(false);
        options.setSupportMultipleWindows(false); options.setBuiltInZoomControls(true); options.setDisplayZoomControls(false);
        options.setLoadWithOverviewMode(true); options.setUseWideViewPort(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        new Settings(this).prefs.edit().putString("user_agent", options.getUserAgentString()).apply();
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (UrlPolicy.ministry(request.getUrl().toString())) return false;
                Toast.makeText(LoginActivity.this, "برای امنیت، فقط صفحه HTTPS وزارت بهداشت داخل برنامه باز می‌شود.", Toast.LENGTH_LONG).show();
                return true;
            }
            @Override public void onPageFinished(WebView view, String url) {
                try { address.setText(new java.net.URI(url).getHost()); } catch (Exception ignored) { address.setText("طرح‌پلاس"); }
                CookieManager.getInstance().flush();
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel(); address.setText("گواهی امنیتی سایت معتبر نیست؛ اتصال متوقف شد.");
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) address.setText("صفحه باز نشد؛ اینترنت و دسترسی به سایت را بررسی کن.");
            }
        });
        LinearLayout bottom = Ui.column(this); bottom.setPadding(Ui.dp(this, 14), Ui.dp(this, 8), Ui.dp(this, 14), 0); root.addView(bottom);
        Ui.button(this, bottom, "همین گزارش را انتخاب و بررسی کن", this::selectReport, true);
        Ui.button(this, bottom, "بازکردن نشانی گزارش / برگشت", () -> new android.app.AlertDialog.Builder(this)
                .setItems(new String[]{"رفتن به گزارش ذخیره‌شده", "صفحه اصلی سامانه", "برگشت به برنامه"}, (d, i) -> {
                    if (i == 0) web.loadUrl(new Settings(this).reportUrl()); else if (i == 1) web.loadUrl(UrlPolicy.HOME); else finish();
                }).show(), false);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { goBack(); }
        });
        web.loadUrl(getIntent().getBooleanExtra("report", false) ? new Settings(this).reportUrl() : UrlPolicy.HOME);
    }

    private void selectReport() {
        String url = web.getUrl();
        if (url == null || !UrlPolicy.report(url)) {
            Toast.makeText(this, "هنوز صفحه WaitingQueueReport باز نیست؛ از منوی سامانه وارد گزارش اعلام نیاز شو.", Toast.LENGTH_LONG).show(); return;
        }
        try {
            Settings s = new Settings(this);
            Schedule.stopAll(this); s.invalidateReport(); s.putSecret("report_url", url);
            CookieManager.getInstance().flush();
            s.prefs.edit().putBoolean("chosen", true).apply();
            Schedule.now(this); finish();
        } catch (Exception e) { Toast.makeText(this, "ذخیره امن نشانی انجام نشد؛ دوباره تلاش کن.", Toast.LENGTH_LONG).show(); }
    }
    private void goBack() { if (web.canGoBack()) web.goBack(); else finish(); }
    @Override protected void onPause() { CookieManager.getInstance().flush(); super.onPause(); }
    @Override protected void onDestroy() {
        if (web != null) { web.stopLoading(); web.destroy(); }
        super.onDestroy();
    }
}
