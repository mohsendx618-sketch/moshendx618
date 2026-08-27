package ir.cafenet.hamyar;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Stores guide IDs and ticks only; never customer data, passwords or account identifiers. */
final class UserState {
    private final SharedPreferences prefs;
    UserState(Context c){prefs=c.getSharedPreferences("learning_state_v1",Context.MODE_PRIVATE);}
    boolean favorite(String id){return prefs.getStringSet("favorites",new HashSet<>()).contains(id);}
    void toggleFavorite(String id){Set<String> s=new HashSet<>(prefs.getStringSet("favorites",new HashSet<>()));if(!s.add(id))s.remove(id);prefs.edit().putStringSet("favorites",s).apply();}
    List<String> recent(){List<String> out=new ArrayList<>();for(String id:prefs.getString("recent","").split(","))if(!id.isEmpty())out.add(id);return out;}
    void opened(String id){List<String> list=recent();list.remove(id);list.add(0,id);if(list.size()>12)list=list.subList(0,12);prefs.edit().putString("recent",String.join(",",list)).apply();}
    boolean done(String id,int step){return prefs.getBoolean("step:"+id+":"+step,false);}
    void done(String id,int step,boolean yes){prefs.edit().putBoolean("step:"+id+":"+step,yes).apply();}
    int count(Guide g){int n=0;for(int i=0;i<g.steps.size();i++)if(done(g.id,i))n++;return n;}
    int next(Guide g){for(int i=0;i<g.steps.size();i++)if(!done(g.id,i))return i;return -1;}
    void reset(Guide g){SharedPreferences.Editor e=prefs.edit();for(int i=0;i<g.steps.size();i++)e.remove("step:"+g.id+":"+i);e.apply();}
    int font(){return prefs.getInt("font",0);}
    void cycleFont(){prefs.edit().putInt("font",(font()+1)%3).apply();}
}
