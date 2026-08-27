package ir.tarhplus.watch;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.WebStorage;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import java.text.DateFormat;
import java.util.Date;

public final class MainActivity extends Activity implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Settings settings;
    private TextView target, status, times, telegram, notifications;
    private Switch daily;
    private boolean refreshing;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new Settings(this);
        LinearLayout outer = Ui.column(this); setContentView(outer); Ui.insets(this, outer);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); outer.addView(scroll, new LinearLayout.LayoutParams(-1, -1));
        LinearLayout body = Ui.column(this); body.setPadding(Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 22), Ui.dp(this, 20)); scroll.addView(body);
        Ui.text(this, body, "پایش طرح‌پلاس", 28, true);
        target = Ui.text(this, body, "", 18, true); target.setTextColor(Ui.TEAL);
        Ui.text(this, body, "پایش شخصی اعلام نیاز • نسخه آزمایشی ۱.۰\nاین برنامه وابسته به وزارت بهداشت نیست.", 12, false);

        LinearLayout card = Ui.column(this); card.setPadding(Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 18), Ui.dp(this, 8));
        GradientDrawable background = new GradientDrawable(); background.setColor(Color.WHITE); background.setCornerRadius(Ui.dp(this, 18)); card.setBackground(background);
        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(-1, -2); cardParams.bottomMargin = Ui.dp(this, 18); body.addView(card, cardParams);
        Ui.text(this, card, "وضعیت بررسی", 15, true);
        status = Ui.text(this, card, "", 15, false); status.setId(R.id.status_text);
        times = Ui.text(this, card, "", 11, false);
        telegram = Ui.text(this, card, "", 12, false);

        Ui.button(this, body, "۱. ورود به سامانه و انتخاب گزارش", () -> startActivity(new Intent(this, LoginActivity.class)), true).setId(R.id.login_button);
        Ui.button(this, body, "۲. تنظیم و آزمایش تلگرام", this::telegramSettings, false);
        Ui.button(this, body, "بررسی همین حالا", () -> {
            if (!settings.prefs.getBoolean("chosen", false)) { toast("اول از دکمه ورود، صفحه گزارش را انتخاب کن."); return; }
            Schedule.now(this);
        }, false);
        Ui.button(this, body, "دیدن گزارش در سامانه", () -> startActivity(new Intent(this, LoginActivity.class).putExtra("report", true)), false);

        daily = new Switch(this); daily.setText("بررسی خودکار هر روز"); daily.setTextSize(17); daily.setPadding(0, Ui.dp(this, 10), 0, Ui.dp(this, 10));
        body.addView(daily, new LinearLayout.LayoutParams(-1, -2));
        daily.setOnCheckedChangeListener((button, checked) -> {
            if (refreshing) return;
            if (checked && !settings.prefs.getBoolean("validated", false)) {
                toast("پیش از روشن کردن پایش، یک بررسی موفق لازم است."); refresh(); return;
            }
            Schedule.daily(this, checked);
            if (checked) { askNotificationPermission(); toast("پایش روزانه روشن شد؛ گوشی باید روشن و به اینترنت متصل باشد."); }
        });
        Ui.text(this, body, "زمان اجرا تقریبی است؛ محدودیت باتری ممکن است آن را عقب بیندازد. هنگام تغییر نتیجه یا مشکل ورود، اعلان می‌گیری.", 12, false);
        notifications = Ui.text(this, body, "", 12, false);
        Ui.button(this, body, "اجازه اعلان و تنظیم باتری", () -> {
            askNotificationPermission();
            new AlertDialog.Builder(this).setMessage("در تنظیمات برنامه، اعلان‌ها را روشن و باتری را روی «بدون محدودیت / Unrestricted» بگذار. در سامسونگ برنامه را از فهرست برنامه‌های خواب خارج کن. پس از Force stop باید دوباره برنامه را باز کنی.")
                    .setPositiveButton("بازکردن تنظیمات", (d, w) -> startActivity(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName()))))
                    .setNegativeButton("بستن", null).show();
        }, false);
        Ui.button(this, body, "ویرایش شهر، رشته و نشانی گزارش", this::reportSettings, false);
        Ui.button(this, body, "راهنما و محدودیت‌ها", this::help, false);
        Ui.button(this, body, "خروج از سامانه و پاک‌کردن اطلاعات", this::clearData, false);
        refresh();
    }

    private void refresh() {
        if (settings == null) return;
        refreshing = true;
        target.setText(getString(R.string.target_summary, settings.major(), settings.city()));
        status.setText(settings.prefs.getString("status", "هنوز بررسی نشده. ابتدا وارد سامانه شو و صفحه گزارش را انتخاب کن."));
        times.setText(getString(R.string.check_times, date(settings.prefs.getLong("last_attempt", 0)), date(settings.prefs.getLong("last_success", 0))));
        telegram.setText(settings.prefs.getBoolean("telegram_enabled", false)
                ? settings.prefs.getString("telegram_status", "اعلان تلگرام فعال است.") : "اعلان تلگرام هنوز فعال نیست.");
        daily.setChecked(settings.prefs.getBoolean("daily", false));
        notifications.setText(getSystemService(NotificationManager.class).areNotificationsEnabled()
                ? "اعلان گوشی مجاز است." : "اعلان گوشی بسته است؛ از تنظیمات برنامه اجازه بده.");
        refreshing = false;
    }
    private String date(long time) { return time == 0 ? "—" : DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(new Date(time)); }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
    private void askNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
    }

    private EditText input(LinearLayout parent, String label, String value, boolean secret) {
        Ui.text(this, parent, label, 13, true);
        EditText edit = new EditText(this); edit.setText(value); edit.setTextSize(14); edit.setSingleLine(true); edit.setSaveEnabled(false);
        edit.setInputType(InputType.TYPE_CLASS_TEXT | (secret ? InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS));
        edit.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = Ui.dp(this, 12); parent.addView(edit, p); return edit;
    }
    private LinearLayout dialogBody() {
        LinearLayout view = Ui.column(this); view.setPadding(Ui.dp(this, 22), Ui.dp(this, 10), Ui.dp(this, 22), Ui.dp(this, 6)); return view;
    }

    private void telegramSettings() {
        LinearLayout content = dialogBody();
        Ui.text(this, content, "توکن ربات خودت و Chat ID مقصد را وارد کن. ابتدا در تلگرام ربات را Start کن. فقط خلاصه نتیجه فرستاده می‌شود؛ نه رمز، کوکی یا متن جدول. برای تلگرام ممکن است VPN فعال لازم باشد.", 13, false);
        EditText token = input(content, "Bot token", settings.secret("bot"), true);
        EditText chat = input(content, "Chat ID", settings.secret("chat"), false);
        token.setTextDirection(View.TEXT_DIRECTION_LTR); chat.setTextDirection(View.TEXT_DIRECTION_LTR);
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("اعلان تلگرام").setView(scroll)
                .setPositiveButton("ذخیره و ارسال تست", null).setNegativeButton("بستن", null)
                .setNeutralButton("قطع اعلان تلگرام", (d, w) -> settings.prefs.edit().putBoolean("telegram_enabled", false).apply()).create();
        dialog.show(); dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        java.util.concurrent.atomic.AtomicBoolean keepResult = new java.util.concurrent.atomic.AtomicBoolean(true);
        dialog.setOnDismissListener(d -> keepResult.set(false));
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String t = token.getText().toString().trim(), c = chat.getText().toString().trim();
            if (!Telegram.validToken(t) || !Telegram.validChat(c)) { toast("توکن ربات یا Chat ID قالب درستی ندارد."); return; }
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("در حال ارسال…");
            long generation = settings.prefs.getLong("generation", 0);
            new Thread(() -> {
                boolean ok = false;
                try {
                    Telegram.send(t, c, "پیام آزمایشی پایش طرح‌پلاس\nاتصال ربات برقرار است. پس از روشن کردن بررسی روزانه، تغییر نتیجه یا خطای بررسی اطلاع داده می‌شود.");
                    if (!keepResult.get() || settings.prefs.getLong("generation", 0) != generation) return;
                    settings.prefs.edit().putBoolean("telegram_enabled", false).apply();
                    settings.putSecret("bot", t); settings.putSecret("chat", c);
                    settings.prefs.edit().putBoolean("telegram_enabled", true).remove("telegram_fingerprint").remove("telegram_warning")
                            .putString("telegram_status", "پیام آزمایشی با موفقیت تحویل تلگرام شد.").apply();
                    ok = true;
                } catch (Exception ignored) { /* Do not log credentials or URLs. */ }
                boolean success = ok;
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    if (success) { dialog.dismiss(); toast("پیام تست ارسال شد و تنظیمات ذخیره شد."); }
                    else { dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true); dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText("ذخیره و ارسال تست");
                        toast("ارسال یا ذخیره انجام نشد. VPN، Start کردن ربات، توکن و Chat ID را بررسی کن."); }
                });
            }, "telegram-test").start();
        });
    }

    private void reportSettings() {
        LinearLayout content = dialogBody();
        EditText city = input(content, "شهرستان", settings.city(), false);
        EditText major = input(content, "رشته", settings.major(), false);
        EditText url = input(content, "نشانی کامل صفحه گزارش (اختیاری)", settings.reportUrl(), false); url.setTextDirection(View.TEXT_DIRECTION_LTR);
        Ui.text(this, content, "نشانی باید صفحه WaitingQueueReport در HTTPS طرح‌پلاس باشد. تغییر این موارد، پایش روزانه را خاموش می‌کند تا دوباره بررسی موفق انجام شود.", 12, false);
        ScrollView scroll = new ScrollView(this); scroll.addView(content);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("هدف پایش").setView(scroll).setPositiveButton("ذخیره", null).setNegativeButton("انصراف", null).create();
        dialog.show(); dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String cityValue = city.getText().toString().trim(), majorValue = major.getText().toString().trim(), urlValue = url.getText().toString().trim();
            if (cityValue.isEmpty() || majorValue.isEmpty() || !UrlPolicy.report(urlValue)) { toast("شهر، رشته و نشانی معتبر لازم است."); return; }
            try {
                Schedule.stopAll(this); settings.invalidateReport(); settings.putSecret("report_url", urlValue);
                settings.prefs.edit().putString("city", cityValue).putString("major", majorValue).putBoolean("chosen", true)
                        .putString("status", "هدف عوض شد؛ بررسی همین حالا را بزن. پس از بررسی موفق، پایش روزانه را دوباره روشن کن.").apply();
                dialog.dismiss();
            } catch (Exception e) { toast("ذخیره امن انجام نشد."); }
        });
    }

    private void help() {
        new AlertDialog.Builder(this).setTitle("راه‌اندازی")
                .setMessage("۱. وارد سامانه شو و گزارش اعلام نیاز را باز کن؛ «همین گزارش را انتخاب و بررسی کن» را بزن.\n\n۲. نتیجه اولین بررسی را ببین. اگر ساختار گزارش شناخته نشد، نتیجه قطعی نیست و پایش روزانه فعال نمی‌شود.\n\n۳. تلگرام را تنظیم و پیام آزمایشی را دریافت کن. سپس بررسی روزانه را روشن کن.\n\nبررسی هر ۲۴ ساعت تقریبی است. اینترنت و برای تلگرام گاهی VPN لازم است. خاموشی، Force stop و محدودیت باتری می‌تواند اجرا را متوقف کند.\n\nکپچا و رمز فقط در سایت وارد می‌شوند. برنامه ورود را دور نمی‌زند؛ اگر نشست منقضی شود ورود دوباره لازم است.\n\nبرنامه جدول قابل دریافت با نشانی گزارش و صفحه‌بندی استاندارد ASP.NET را بررسی می‌کند. فیلترهایی که فقط با JavaScript یا ارسال فرم روی صفحه اعمال می‌شوند ممکن است در دریافت پس‌زمینه حفظ نشوند.\n\nردیف مرتبط، به معنی ظرفیت خالی یا ثبت‌نام قطعی نیست. نتیجه و ظرفیت نهایی را در سامانه تأیید کن. این نسخه هنوز با حساب واقعی تو آزمایش نشده است.")
                .setPositiveButton("متوجه شدم", null).show();
    }

    private void clearData() {
        new AlertDialog.Builder(this).setTitle("پاک‌کردن اطلاعات این برنامه؟")
                .setMessage("ورود ذخیره‌شده، توکن تلگرام و تنظیمات پاک می‌شوند و پایش متوقف می‌شود. حساب‌های اصلی تو حذف نمی‌شوند.")
                .setNegativeButton("انصراف", null).setPositiveButton("پاک شود", (d, w) -> {
                    Schedule.stopAll(this);
                    long generation = settings.prefs.getLong("generation", 0) + 1;
                    settings.prefs.edit().clear().putLong("generation", generation).apply();
                    CookieManager.getInstance().removeAllCookies(value -> { CookieManager.getInstance().flush(); toast("ورود و تنظیمات پاک شد."); });
                    WebStorage.getInstance().deleteAllData();
                    getSystemService(NotificationManager.class).cancelAll();
                    refresh();
                }).show();
    }
    @Override protected void onResume() { super.onResume(); settings.prefs.registerOnSharedPreferenceChangeListener(this); refresh(); }
    @Override protected void onPause() { settings.prefs.unregisterOnSharedPreferenceChangeListener(this); super.onPause(); }
    @Override public void onSharedPreferenceChanged(SharedPreferences prefs, String key) { runOnUiThread(this::refresh); }
}
