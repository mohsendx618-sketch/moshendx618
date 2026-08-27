package ir.cafenet.hamyar;

import android.app.Activity;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int INK=Color.rgb(23,43,44), MUTED=Color.rgb(83,104,104), TEAL=Color.rgb(18,109,97);
    static final int BG=Color.rgb(245,246,242), WHITE=Color.WHITE, LINE=Color.rgb(216,226,219);
    static final int SOFT=Color.rgb(230,241,234), AMBER=Color.rgb(128,85,20), NOTE=Color.rgb(255,244,218);
    static int dp(Activity a,int n){return Math.round(n*a.getResources().getDisplayMetrics().density);}
    static String fa(int value){String s=Integer.toString(value);StringBuilder b=new StringBuilder();for(char c:s.toCharArray())b.append(c>='0'&&c<='9'?(char)('۰'+c-'0'):c);return b.toString();}
    static LinearLayout column(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    static LinearLayout row(Activity a){LinearLayout l=new LinearLayout(a);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);l.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);return l;}
    static GradientDrawable shape(Activity a,int color,int radius,boolean stroke){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(a,radius));if(stroke)d.setStroke(dp(a,1),LINE);return d;}
    static void clickable(Activity a,View v,int color,int radius){v.setBackground(new RippleDrawable(ColorStateList.valueOf(0x25126D61),shape(a,color,radius,true),null));}
    static LinearLayout card(Activity a,LinearLayout parent,int color){LinearLayout c=column(a);c.setPadding(dp(a,16),dp(a,14),dp(a,16),dp(a,14));c.setBackground(shape(a,color,18,true));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(a,12);parent.addView(c,p);return c;}
    static TextView text(Activity a,LinearLayout parent,String value,int size,boolean bold){TextView t=new TextView(a);t.setText(value);t.setTextSize(size);t.setTextColor(INK);t.setGravity(Gravity.START);t.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);t.setLineSpacing(dp(a,3),1.12f);if(bold)t.setTypeface(null,Typeface.BOLD);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(a,8);parent.addView(t,p);return t;}
    static TextView note(Activity a,LinearLayout parent,String value){TextView t=text(a,parent,value,13,false);t.setTextColor(AMBER);t.setPadding(dp(a,12),dp(a,9),dp(a,12),dp(a,9));t.setBackground(shape(a,NOTE,10,false));return t;}
    static Button button(Activity a,String label,boolean selected,Runnable action){Button b=new Button(a);b.setText(label);b.setTextSize(14);b.setAllCaps(false);b.setTextColor(selected?WHITE:TEAL);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(a,48));b.setMinimumHeight(dp(a,48));b.setPadding(dp(a,12),dp(a,6),dp(a,12),dp(a,6));clickable(a,b,selected?TEAL:WHITE,12);b.setOnClickListener(v->action.run());return b;}
    static Button button(Activity a,LinearLayout parent,String label,boolean selected,Runnable action){Button b=button(a,label,selected,action);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(a,8);parent.addView(b,p);return b;}
    static void weighted(LinearLayout row,View v,Activity a){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,-2,1f);p.setMarginEnd(dp(a,4));row.addView(v,p);}
}
