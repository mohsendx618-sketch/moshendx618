package ir.tarhplus.watch;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public final class MonitorWorker extends Worker {
    private static final Object RUN_LOCK = new Object();
    public MonitorWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull @Override public Result doWork() {
        synchronized (RUN_LOCK) { return check(); }
    }

    private Result check() {
        Settings s = new Settings(getApplicationContext());
        boolean manual = getInputData().getBoolean("manual", false);
        if (isStopped() || (!manual && !s.prefs.getBoolean("daily", false))) return Result.success();
        if (!s.prefs.getBoolean("chosen", false)) {
            s.prefs.edit().putString("status", "ابتدا وارد سامانه شو و صفحه گزارش را انتخاب کن.").apply();
            return Result.success();
        }
        long generation = s.prefs.getLong("generation", 0);
        s.prefs.edit().putLong("last_attempt", System.currentTimeMillis()).putString("status", "در حال بررسی گزارش و صفحه‌های بعدی…").apply();
        try {
            ReportClient.Scan scan = new ReportClient(s.prefs.getString("user_agent", "")).scan(s.reportUrl(), s.city(), s.major());
            if (stale(s, generation)) return Result.success();
            if (scan.state == ReportParser.State.LOGIN_REQUIRED)
                return warning(s, "login", "ورود به سامانه منقضی شده؛ برنامه را باز کن و دوباره وارد شو.", false);
            if (scan.state == ReportParser.State.UNKNOWN)
                return warning(s, "unknown", "ساختار گزارش یا همه صفحه‌ها قابل بررسی نبود؛ نتیجه قطعی نیست. گزارش را در سامانه باز کن.", false);
            String fingerprint = scan.matches.isEmpty() ? "none" : ReportParser.fingerprint(scan.matches);
            String previous = s.prefs.getString("fingerprint", "");
            String summary = scan.matches.isEmpty()
                    ? "در " + scan.pages + " صفحه بررسی‌شده، ردیفی برای «" + s.major() + " — " + s.city() + "» پیدا نشد."
                    : scan.matches.size() + " ردیف مرتبط با «" + s.major() + " — " + s.city() + "» پیدا شد. ظرفیت را در خود سامانه بررسی کن.";
            s.prefs.edit().putString("fingerprint", fingerprint).putString("status", summary)
                    .putBoolean("validated", true).putLong("last_success", System.currentTimeMillis())
                    .putInt("pages", scan.pages).putInt("matches", scan.matches.size())
                    .remove("last_warning").remove("telegram_warning").apply();
            if (!fingerprint.equals(previous) && (!previous.isEmpty() || !scan.matches.isEmpty())) notifyUser(summary);
            if (s.prefs.getBoolean("telegram_enabled", false)) {
                String delivered = s.prefs.getString("telegram_fingerprint", "");
                if (!fingerprint.equals(delivered) && (!delivered.isEmpty() || !scan.matches.isEmpty())) {
                    try {
                        if (stale(s, generation)) return Result.success();
                        Telegram.send(s.secret("bot"), s.secret("chat"), "پایش طرح‌پلاس\n" + summary
                                + "\nبرای دیدن جزئیات، برنامه را باز کن. این پیام تأیید ظرفیت یا ثبت‌نام نیست.");
                        if (!stale(s, generation)) s.prefs.edit().putString("telegram_fingerprint", fingerprint)
                                .putString("telegram_status", "آخرین اعلان تحویل تلگرام شد.").apply();
                    } catch (Exception e) {
                        if (!stale(s, generation)) s.prefs.edit().putString("telegram_status", "ارسال تلگرام ناموفق بود؛ اینترنت، VPN یا تنظیم ربات را بررسی کن.").apply();
                        return retryLimited();
                    }
                } else if (delivered.isEmpty()) {
                    // Seed a negative baseline without sending an unsolicited empty first report.
                    s.prefs.edit().putString("telegram_fingerprint", fingerprint).apply();
                }
            }
            return Result.success();
        } catch (ReportClient.LoginNeeded e) {
            return stale(s, generation) ? Result.success() : warning(s, "login", "سامانه به ورود دوباره نیاز دارد؛ برنامه را باز کن و وارد شو.", false);
        } catch (Exception e) {
            // Never display or log exception messages: a URL may contain a session token.
            return stale(s, generation) ? Result.success() : warning(s, "network", "بررسی انجام نشد؛ اینترنت، دسترسی به سامانه یا پاسخ سرور مشکل دارد. نتیجه قبلی، نتیجه فعلی نیست.", true);
        }
    }

    private boolean stale(Settings s, long generation) { return isStopped() || generation != s.prefs.getLong("generation", 0); }
    private Result retryLimited() { return getRunAttemptCount() < 2 ? Result.retry() : Result.success(); }

    private Result warning(Settings s, String code, String message, boolean retry) {
        if (!code.equals(s.prefs.getString("last_warning", ""))) notifyUser(message);
        s.prefs.edit().putString("last_warning", code).putString("status", message).putBoolean("validated", false).apply();
        if (s.prefs.getBoolean("telegram_enabled", false) && !code.equals(s.prefs.getString("telegram_warning", ""))) {
            try {
                Telegram.send(s.secret("bot"), s.secret("chat"), "هشدار پایش طرح‌پلاس\n" + message);
                s.prefs.edit().putString("telegram_warning", code).putString("telegram_status", "هشدار به تلگرام رسید.").apply();
            } catch (Exception e) {
                s.prefs.edit().putString("telegram_status", "هشدار تلگرام ارسال نشد؛ دسترسی شبکه و تنظیم ربات را بررسی کن.").apply();
                retry = true;
            }
        }
        return retry ? retryLimited() : Result.success();
    }

    private void notifyUser(String message) {
        Context context = getApplicationContext();
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return;
        PendingIntent open = PendingIntent.getActivity(context, 1, new Intent(context, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification notification = new Notification.Builder(context, "watch").setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("پایش طرح‌پلاس").setContentText(message).setStyle(new Notification.BigTextStyle().bigText(message))
                .setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build();
        try { context.getSystemService(NotificationManager.class).notify(27, notification); }
        catch (SecurityException ignored) { /* Status remains visible inside the app. */ }
    }
}
