package ir.tarhplus.watch;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int INK = Color.rgb(20, 44, 57), TEAL = Color.rgb(8, 126, 139);
    static int dp(Activity a, int value) { return Math.round(value * a.getResources().getDisplayMetrics().density); }
    static LinearLayout column(Activity a) {
        LinearLayout l = new LinearLayout(a); l.setOrientation(LinearLayout.VERTICAL); l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); return l;
    }
    static void insets(Activity a, View root) {
        root.setOnApplyWindowInsetsListener((v, inset) -> {
            v.setPadding(inset.getSystemWindowInsetLeft(), inset.getSystemWindowInsetTop(), inset.getSystemWindowInsetRight(), inset.getSystemWindowInsetBottom());
            return inset;
        });
        root.requestApplyInsets();
    }
    static TextView text(Activity a, LinearLayout parent, String text, int size, boolean bold) {
        TextView view = new TextView(a); view.setText(text); view.setTextSize(size); view.setTextColor(INK);
        view.setGravity(Gravity.START); view.setTextDirection(View.TEXT_DIRECTION_RTL);
        view.setLineSpacing(dp(a, 3), 1.08f);
        if (bold) view.setTypeface(null, Typeface.BOLD);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(a, 10);
        parent.addView(view, p); return view;
    }
    static Button button(Activity a, LinearLayout parent, String title, Runnable action, boolean primary) {
        Button b = new Button(a); b.setText(title); b.setTextSize(15); b.setAllCaps(false);
        b.setTextColor(primary ? Color.WHITE : TEAL); b.setMinHeight(dp(a, 52));
        b.setPadding(dp(a, 10), dp(a, 8), dp(a, 10), dp(a, 8));
        GradientDrawable background = new GradientDrawable(); background.setColor(primary ? TEAL : Color.WHITE);
        background.setCornerRadius(dp(a, 14)); background.setStroke(dp(a, 1), primary ? TEAL : Color.rgb(202, 221, 226));
        b.setBackground(background);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(a, 10);
        parent.addView(b, p); b.setOnClickListener(v -> action.run()); return b;
    }
}
