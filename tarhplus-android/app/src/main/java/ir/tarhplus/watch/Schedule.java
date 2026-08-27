package ir.tarhplus.watch;

import android.content.Context;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;

final class Schedule {
    private static Constraints online() { return new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(); }
    static void now(Context context) {
        new Settings(context).prefs.edit().putString("status", "در انتظار اینترنت و شروع بررسی…").apply();
        WorkManager.getInstance(context).enqueueUniqueWork("tarhplus-now", ExistingWorkPolicy.KEEP,
                new OneTimeWorkRequest.Builder(MonitorWorker.class).setConstraints(online())
                        .setInputData(new Data.Builder().putBoolean("manual", true).build())
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
    }
    static void daily(Context context, boolean enabled) {
        WorkManager manager = WorkManager.getInstance(context);
        new Settings(context).prefs.edit().putBoolean("daily", enabled).apply();
        if (!enabled) { manager.cancelUniqueWork("tarhplus-daily"); return; }
        manager.enqueueUniquePeriodicWork("tarhplus-daily", ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(MonitorWorker.class, 24, TimeUnit.HOURS, 1, TimeUnit.HOURS)
                        .setConstraints(online()).setInitialDelay(24, TimeUnit.HOURS)
                        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS).build());
    }
    static void stopAll(Context context) {
        daily(context, false);
        WorkManager.getInstance(context).cancelUniqueWork("tarhplus-now");
    }
}
