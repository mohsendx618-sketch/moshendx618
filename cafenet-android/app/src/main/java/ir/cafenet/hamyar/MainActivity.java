package ir.cafenet.hamyar;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class MainActivity extends ComponentActivity {
    private Catalog catalog;
    private UserState state;
    private String query="",category="همه موضوع‌ها",mode="همه",guideId="";
    private int tab=1,homeY=0,guideY=0;
    private LinearLayout root,results,body,filterRow,modeRow;
    private EditText search;
    private TextView resultCount,progress;
    private ScrollView scroll;
    private final List<View> stepViews=new ArrayList<>();

    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);
        state=new UserState(this);
        try{catalog=new Catalog(this);}catch(Exception e){
            LinearLayout error=Ui.column(this);error.setPadding(30,60,30,30);Ui.text(this,error,"آموزش‌ها باز نشدند",22,true);
            Ui.text(this,error,"فایل نصب ناقص است. برنامه را از همان فایل اصلی دوباره نصب کن؛ اطلاعات مشتری در این برنامه ذخیره نشده است.",17,false);setContentView(error);return;
        }
        if(saved!=null){query=saved.getString("query","");category=saved.getString("category","همه موضوع‌ها");mode=saved.getString("mode","همه");guideId=saved.getString("guide","");tab=saved.getInt("tab",1);homeY=saved.getInt("homeY",0);guideY=saved.getInt("guideY",0);}
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){
            if(!guideId.isEmpty()){guideId="";showHome();}else if(!query.isEmpty()){search.setText("");}else{setEnabled(false);getOnBackPressedDispatcher().onBackPressed();}
        }});
        if(catalog.byId.containsKey(guideId))showGuide(false);else{guideId="";showHome();}
    }
    @Override protected void onSaveInstanceState(Bundle out){
        super.onSaveInstanceState(out);if(scroll!=null){if(guideId.isEmpty())homeY=scroll.getScrollY();else guideY=scroll.getScrollY();}
        out.putString("query",query);out.putString("category",category);out.putString("mode",mode);out.putString("guide",guideId);out.putInt("tab",tab);out.putInt("homeY",homeY);out.putInt("guideY",guideY);
    }
    private int bodySize(){return 16+state.font()*2;}
    private void newRoot(){
        root=Ui.column(this);root.setId(R.id.content_root);root.setBackgroundColor(Ui.BG);root.setFocusableInTouchMode(true);
        ViewCompat.setOnApplyWindowInsetsListener(root,(v,insets)->{androidx.core.graphics.Insets x=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.ime());v.setPadding(x.left,x.top,x.right,x.bottom);return insets;});
        setContentView(root);ViewCompat.requestApplyInsets(root);
    }
    private LinearLayout padded(LinearLayout parent){LinearLayout l=Ui.column(this);l.setPadding(Ui.dp(this,18),Ui.dp(this,12),Ui.dp(this,18),0);parent.addView(l,new LinearLayout.LayoutParams(-1,-2));return l;}
    private LinearLayout scrolling(LinearLayout parent){scroll=new ScrollView(this);scroll.setId(R.id.body_scroll);scroll.setFillViewport(true);parent.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));LinearLayout c=Ui.column(this);c.setPadding(Ui.dp(this,18),Ui.dp(this,8),Ui.dp(this,18),Ui.dp(this,24));scroll.addView(c);return c;}
    private void showHome(){
        newRoot();LinearLayout header=padded(root);LinearLayout top=Ui.row(this);header.addView(top);
        TextView title=new TextView(this);title.setText("همیار کافی‌نت");title.setTextSize(26);title.setTextColor(Ui.INK);title.setTypeface(null,Typeface.BOLD);top.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        Button info=Ui.button(this,"درباره",false,this::about);top.addView(info,new LinearLayout.LayoutParams(-2,-2));
        TextView sub=Ui.text(this,header,Ui.fa(catalog.guides.size())+" راهنما • جست‌وجو و مطالعه کاملاً آفلاین",13,false);sub.setTextColor(Ui.TEAL);
        LinearLayout searchRow=Ui.row(this);header.addView(searchRow,new LinearLayout.LayoutParams(-1,-2));
        search=new EditText(this);search.setId(R.id.search_input);search.setHint("جست‌وجو؛ کدپستی، اسکن، بیمه…");search.setTextSize(17);search.setSingleLine(true);search.setTextColor(Ui.INK);search.setHintTextColor(Ui.MUTED);search.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_RTL);search.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);search.setImeOptions(android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH);search.setPadding(Ui.dp(this,14),Ui.dp(this,10),Ui.dp(this,14),Ui.dp(this,10));search.setBackground(Ui.shape(this,Ui.WHITE,14,true));search.setMinHeight(Ui.dp(this,54));searchRow.addView(search,new LinearLayout.LayoutParams(0,-2,1f));
        Button clear=Ui.button(this,"پاک",false,()->search.setText(""));clear.setContentDescription("پاک کردن جست‌وجو");LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(Ui.dp(this,56),-2);cp.setMarginStart(Ui.dp(this,6));searchRow.addView(clear,cp);
        search.setText(query);search.setSelection(query.length());
        search.setOnEditorActionListener((v,id,event)->{hideKeyboard();return true;});
        Button taxes=Ui.button(this,"مالیات و اظهارنامه • "+Ui.fa(catalog.taxCount())+" راهنما",category.equals("همه مالیات‌ها"),()->{query="";mode="همه";category="همه مالیات‌ها";homeY=0;showHome();});taxes.setId(R.id.tax_shortcut);LinearLayout.LayoutParams tp=new LinearLayout.LayoutParams(-1,-2);tp.topMargin=Ui.dp(this,8);header.addView(taxes,tp);
        modeRow=Ui.row(this);LinearLayout.LayoutParams mp=new LinearLayout.LayoutParams(-1,-2);mp.topMargin=Ui.dp(this,8);header.addView(modeRow,mp);renderModes();
        HorizontalScrollView cats=new HorizontalScrollView(this);cats.setHorizontalScrollBarEnabled(false);header.addView(cats);filterRow=Ui.row(this);cats.addView(filterRow);renderCategories();
        resultCount=Ui.text(this,header,"",13,false);resultCount.setId(R.id.results_count);resultCount.setTextColor(Ui.MUTED);
        results=scrolling(root);renderResults();
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int start,int count,int after){} public void onTextChanged(CharSequence s,int start,int before,int count){query=s.toString();homeY=0;renderResults();scroll.scrollTo(0,0);}public void afterTextChanged(Editable e){}});
        root.requestFocus();scroll.post(()->scroll.scrollTo(0,homeY));
    }
    private void renderModes(){modeRow.removeAllViews();for(String label:new String[]{"همه","نشان‌شده","اخیر","نیمه‌تمام"}){Button b=Ui.button(this,label,mode.equals(label),()->{mode=label;homeY=0;renderModes();renderResults();scroll.scrollTo(0,0);});b.setTextSize(12);b.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,5));Ui.weighted(modeRow,b,this);}}
    private void renderCategories(){filterRow.removeAllViews();List<String> names=new ArrayList<>();names.add("همه موضوع‌ها");names.addAll(catalog.categories);for(String label:names){Button b=Ui.button(this,label,category.equals(label),()->{category=label;homeY=0;renderCategories();renderResults();scroll.scrollTo(0,0);});b.setTextSize(12);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,-2);p.setMarginEnd(Ui.dp(this,6));p.topMargin=Ui.dp(this,8);p.bottomMargin=Ui.dp(this,5);filterRow.addView(b,p);}}
    private void renderResults(){
        if(results==null)return;results.removeAllViews();List<SearchEngine.Hit> hits=SearchEngine.search(catalog.entries,query);List<String> recent=state.recent();
        if(mode.equals("اخیر"))hits.sort(Comparator.comparingInt(h->{int i=recent.indexOf(h.entry.id);return i<0?999:i;}));
        int n=0;
        for(SearchEngine.Hit h:hits){Guide g=catalog.byId.get(h.entry.id);if(category.equals("همه مالیات‌ها")){if(!g.id.startsWith("tax_"))continue;}else if(!category.equals("همه موضوع‌ها")&&!category.equals(g.category))continue;
            if(mode.equals("نشان‌شده")&&!state.favorite(g.id))continue;if(mode.equals("اخیر")&&!recent.contains(g.id))continue;
            int count=state.count(g);if(mode.equals("نیمه‌تمام")&&(count==0||count==g.steps.size()))continue;n++;
            LinearLayout card=Ui.card(this,results,Ui.WHITE);Ui.clickable(this,card,Ui.WHITE,18);card.setFocusable(true);card.setContentDescription("باز کردن «"+g.title+"»");
            TextView meta=Ui.text(this,card,g.category+"   •   "+Ui.fa(g.steps.size())+" مرحله"+(state.favorite(g.id)?"   ★":""),12,false);meta.setTextColor(Ui.TEAL);
            Ui.text(this,card,g.title,19,true);
            TextView desc=Ui.text(this,card,query.trim().isEmpty()?g.summary:(h.snippet.isEmpty()?g.summary:h.snippet),14,false);desc.setTextColor(Ui.MUTED);desc.setMaxLines(3);desc.setEllipsize(android.text.TextUtils.TruncateAt.END);
            String status=count>0?"چک‌لیست: "+Ui.fa(count)+" از "+Ui.fa(g.steps.size()):g.evidence.equals("route")?"مسیر کلی؛ منوهای روز نیازمند تطبیق":"راهنما بر پایه منبع رسمی";
            TextView foot=Ui.text(this,card,status,12,false);foot.setTextColor(g.evidence.equals("route")?Ui.AMBER:Ui.TEAL);
            card.setOnClickListener(v->{homeY=scroll.getScrollY();hideKeyboard();guideId=g.id;tab=h.tab;guideY=0;state.opened(g.id);showGuide(false);});
        }
        resultCount.setText(Ui.fa(n)+" راهنما"+(query.trim().isEmpty()?"":" برای «"+query+"»"));
        if(n==0){LinearLayout empty=Ui.card(this,results,Ui.WHITE);Ui.text(this,empty,"اینجا چیزی پیدا نشد",20,true);Ui.text(this,empty,"عبارت کوتاه‌تری مثل «کدپستی»، «عکس»، «سوابق» یا «چاپ» بنویس. فیلتر موضوع و نشان‌شده‌ها هم روی نتیجه اثر دارند.",16,false);Ui.button(this,empty,"نمایش همه راهنماها",true,()->{query="";category="همه موضوع‌ها";mode="همه";homeY=0;showHome();});}
    }
    private void showGuide(boolean preserveScroll){
        Guide g=catalog.byId.get(guideId);if(g==null){guideId="";showHome();return;}
        int oldY=preserveScroll&&scroll!=null?scroll.getScrollY():guideY;
        newRoot();LinearLayout header=padded(root);LinearLayout bar=Ui.row(this);header.addView(bar);
        Ui.weighted(bar,Ui.button(this,"راهنماها ←",false,()->{guideId="";showHome();}),this);
        Button fav=Ui.button(this,state.favorite(g.id)?"★ نشان‌شده":"☆ نشان کن",state.favorite(g.id),()->{state.toggleFavorite(g.id);showGuide(true);});fav.setId(R.id.favorite_button);Ui.weighted(bar,fav,this);
        Button font=Ui.button(this,"آ+",false,()->{state.cycleFont();showGuide(true);});font.setContentDescription("تغییر اندازه نوشته");bar.addView(font,new LinearLayout.LayoutParams(Ui.dp(this,54),-2));
        TextView title=Ui.text(this,header,g.title,22,true);title.setId(R.id.guide_title);
        TextView desc=Ui.text(this,header,g.mode+" • آموزش داخل برنامه آفلاین است",12,false);desc.setTextColor(Ui.MUTED);
        LinearLayout tabs=Ui.row(this);header.addView(tabs);String[] names={"قبل شروع","مراحل","گیر کردم","منابع"};for(int i=0;i<names.length;i++){final int t=i;Button b=Ui.button(this,names[i],tab==i,()->{tab=t;guideY=0;showGuide(false);});b.setTextSize(13);b.setPadding(Ui.dp(this,4),Ui.dp(this,5),Ui.dp(this,4),Ui.dp(this,5));Ui.weighted(tabs,b,this);}
        body=scrolling(root);
        if(tab==0)renderReady(g);else if(tab==1)renderSteps(g);else if(tab==2)renderProblems(g);else renderSources(g);
        scroll.post(()->scroll.scrollTo(0,oldY));
    }
    private void renderReady(Guide g){
        LinearLayout c=Ui.card(this,body,Ui.SOFT);Ui.text(this,c,"خروجی این کار",18,true);Ui.text(this,c,g.outcome,bodySize(),false);
        if(!g.route.isEmpty())linkButton(body,"باز کردن سایت مربوط",g.route);
        Ui.note(this,body,g.caution);
        Ui.text(this,body,"قبل از اینکه شروع کنی",19,true);for(String s:g.ready)bullet(body,s);
        Ui.button(this,body,"برو به مراحل انجام کار",true,()->{tab=1;guideY=0;showGuide(false);});
    }
    private void renderSteps(Guide g){
        stepViews.clear();
        Ui.note(this,body,g.caution);
        if(!g.route.isEmpty())linkButton(body,"نشانی شروع: "+android.net.Uri.parse(g.route).getHost(),g.route);
        LinearLayout counter=Ui.card(this,body,Ui.SOFT);progress=Ui.text(this,counter,"",15,true);progress.setId(R.id.guide_progress);updateProgress(g);
        TextView hint=Ui.text(this,counter,"تیک‌ها فقط یادآور تو هستند؛ برای مشتری بعدی پاکشان کن.",12,false);hint.setTextColor(Ui.MUTED);
        LinearLayout actions=Ui.row(this);counter.addView(actions);
        Ui.weighted(actions,Ui.button(this,"ادامه از تیک‌نخورده",true,()->{int i=state.next(g);if(i>=0&&i<stepViews.size())scroll.smoothScrollTo(0,stepViews.get(i).getTop());else toast("همه مراحل تیک خورده‌اند؛ کنترل نهایی را مرور کن.");}),this);
        Ui.weighted(actions,Ui.button(this,"مشتری جدید",false,()->new AlertDialog.Builder(this).setTitle("شروع دوباره این راهنما؟").setMessage("فقط تیک‌های «"+g.title+"» پاک می‌شوند. نشان‌ها و سایر راهنماها باقی می‌مانند.").setNegativeButton("نه",null).setPositiveButton("پاک کردن تیک‌ها",(d,w)->{state.reset(g);guideY=0;showGuide(false);}).show()),this);
        for(int i=0;i<g.steps.size();i++){
            final int index=i;Guide.Step step=g.steps.get(i);boolean done=state.done(g.id,i);LinearLayout card=Ui.card(this,body,done?Ui.SOFT:Ui.WHITE);stepViews.add(card);
            CheckBox tick=new CheckBox(this);tick.setText(Ui.fa(i+1)+". "+step.title);tick.setTextSize(bodySize()+1);tick.setTextColor(Ui.INK);tick.setTypeface(null,Typeface.BOLD);tick.setMinHeight(Ui.dp(this,48));tick.setTextDirection(View.TEXT_DIRECTION_RTL);tick.setChecked(done);tick.setContentDescription("مرحله "+Ui.fa(i+1)+": "+step.title);card.addView(tick,new LinearLayout.LayoutParams(-1,-2));
            tick.setOnCheckedChangeListener((v,yes)->{state.done(g.id,index,yes);card.setBackground(Ui.shape(this,yes?Ui.SOFT:Ui.WHITE,18,true));updateProgress(g);});
            TextView text=Ui.text(this,card,step.text,bodySize(),false);text.setTextIsSelectable(true);
        }
        LinearLayout finish=Ui.card(this,body,Ui.SOFT);Ui.text(this,finish,"قبل از تحویل به مشتری",19,true);for(String s:g.finish)bullet(finish,s);
        Ui.button(this,body,"به مشکلی خوردم",false,()->{tab=2;guideY=0;showGuide(false);});
    }
    private void updateProgress(Guide g){if(progress!=null)progress.setText(Ui.fa(state.count(g))+" از "+Ui.fa(g.steps.size())+" مرحله تیک خورده");}
    private void renderProblems(Guide g){Ui.text(this,body,"مشکل را پیدا کن؛ راه بعدی را بخوان",17,true);for(Guide.Problem p:g.problems){LinearLayout c=Ui.card(this,body,Ui.WHITE);TextView t=Ui.text(this,c,p.problem,18,true);t.setTextColor(Ui.AMBER);Ui.text(this,c,p.fix,bodySize(),false);}Ui.note(this,body,"اگر پیام سایت با آموزش فرق دارد، ثبت یا پرداخت را متوقف کن و راهنمای همان سامانه را بخوان؛ اطلاعات حدسی وارد نکن.");}
    private void renderSources(Guide g){
        Ui.text(this,body,"حدود اعتبار این آموزش",19,true);Ui.text(this,body,g.caution,bodySize(),false);
        Ui.text(this,body,"بررسی منابع: "+catalog.reviewed,14,true);Ui.text(this,body,catalog.note,14,false);
        for(String key:g.sources){Catalog.Source ref=catalog.sources.get(key);LinearLayout c=Ui.card(this,body,Ui.WHITE);Ui.text(this,c,ref.title,17,true);Ui.text(this,c,ref.scope,14,false);TextView url=Ui.text(this,c,ref.url,12,false);url.setTextDirection(View.TEXT_DIRECTION_LTR);url.setTextIsSelectable(true);linkButton(c,"مشاهده منبع در مرورگر",ref.url);}
        Ui.note(this,body,"این برنامه وابسته به اداره پست، تأمین اجتماعی یا سامانه‌های نام‌برده نیست. هزینه و مهلت را از صفحه روزِ همان خدمت بخوان.");
    }
    private void bullet(LinearLayout parent,String s){Ui.text(this,parent,"• "+s,bodySize(),false);}
    private void linkButton(LinearLayout parent,String label,String url){Ui.button(this,parent,label,false,()->openLink(url));}
    private void openLink(String url){
        if(!UrlPolicy.isAllowed(url)){toast("این نشانی در فهرست منابع برنامه نیست.");return;}
        new AlertDialog.Builder(this).setTitle("باز شدن مرورگر؛ نیازمند اینترنت").setMessage(url+"\n\nفقط سایت باز می‌شود؛ هیچ اطلاعاتی از طرف این برنامه ارسال یا ثبت نمی‌شود. دامنه و شرایط روز سایت را بررسی کن.")
        .setNegativeButton("برگشت",null).setNeutralButton("کپی نشانی",(d,w)->{ClipboardManager c=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(c!=null)c.setPrimaryClip(ClipData.newPlainText("نشانی منبع",url));toast("نشانی کپی شد");})
        .setPositiveButton("باز کن",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,android.net.Uri.parse(url)));}catch(ActivityNotFoundException e){toast("مرورگر نصب نیست؛ نشانی را کپی کن.");}}).show();
    }
    private void about(){new AlertDialog.Builder(this).setTitle("همیار کافی‌نت ۱.۱").setMessage(Ui.fa(catalog.guides.size())+" آموزش متنی • "+Ui.fa(catalog.taxCount())+" راهنمای مالیاتی • بدون تبلیغ\n\nجست‌وجو، آموزش‌ها، نشان‌ها و تیک مراحل بدون اینترنت کار می‌کنند. انجام خدمات در سایت‌های اصلی اینترنت می‌خواهد.\n\nاین نسخه آموزشی است و همه استثناهای مالیاتی را پوشش نمی‌دهد. آموزش مالیات جای حسابدار و مشاور مالیاتی نیست؛ نرخ، نصاب، مهلت و معافیت باید طبق مقررات همان سال و دوره تأیید شوند.\n\nمراحل پشت ورود، ارسال اظهارنامه و پرداخت با حساب واقعی آزمایش نشده‌اند. منابع آرشیوی و بازنشرشده و حدود اعتبار هر راهنما مشخص‌اند.\n\nهیچ کد ملی، رمز، شماره کارت، پیامک یا اطلاعات مشتری دریافت نمی‌شود. فقط شناسه راهنماهای اخیر، نشان‌ها، اندازه متن و تیک‌ها روی همین گوشی ذخیره می‌شوند.\n\nدر جست‌وجو فقط موضوع کار را بنویس، نه اطلاعات مشتری. برای یک مشتری تازه تیک‌های راهنمای قبلی را پاک کن.").setPositiveButton("متوجه شدم",null).show();}
    private void hideKeyboard(){InputMethodManager m=(InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(m!=null&&search!=null)m.hideSoftInputFromWindow(search.getWindowToken(),0);}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
}
