package ir.tarhplus.watch;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.webkit.CookieManager;

public final class WatchApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        CookieManager.getInstance().setAcceptCookie(true);
        NotificationChannel channel = new NotificationChannel("watch", "نتیجه پایش طرح‌پلاس", NotificationManager.IMPORTANCE_DEFAULT);
        channel.setDescription("تغییر ردیف‌های مرتبط، انقضای ورود و خطای بررسی");
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
}
