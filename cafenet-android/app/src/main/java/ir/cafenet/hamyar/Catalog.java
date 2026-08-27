package ir.cafenet.hamyar;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

final class Catalog {
    final List<Guide> guides=new ArrayList<>();
    final List<SearchEngine.Entry> entries=new ArrayList<>();
    final Map<String,Guide> byId=new LinkedHashMap<>();
    final Map<String,Source> sources=new LinkedHashMap<>();
    final LinkedHashSet<String> categories=new LinkedHashSet<>();
    String reviewed,note;
    static final class Source {
        final String title,url,scope;
        Source(JSONObject o) throws Exception {title=o.getString("title");url=o.getString("url");scope=o.getString("scope");
            if(!UrlPolicy.isAllowed(url)) throw new IllegalArgumentException("Unapproved source URL");}
    }
    static List<String> strings(JSONArray a) throws Exception {
        List<String> out=new ArrayList<>();for(int i=0;i<a.length();i++)out.add(a.getString(i));return out;
    }
    Catalog(Context context) throws Exception {
        String json;
        try(InputStream in=context.getAssets().open("guides.json")) {
            java.io.ByteArrayOutputStream buffer=new java.io.ByteArrayOutputStream();
            byte[] bytes=new byte[8192];int n;
            while((n=in.read(bytes))!=-1)buffer.write(bytes,0,n);
            json=buffer.toString(StandardCharsets.UTF_8.name());
        }
        JSONObject root=new JSONObject(json);
        reviewed=root.getString("reviewed");note=root.getString("note");
        JSONObject refs=root.getJSONObject("sources");
        java.util.Iterator<String> keys=refs.keys();while(keys.hasNext()){String k=keys.next();sources.put(k,new Source(refs.getJSONObject(k)));}
        JSONArray list=root.getJSONArray("guides");
        for(int i=0;i<list.length();i++) {
            JSONObject o=list.getJSONObject(i);List<Guide.Step> steps=new ArrayList<>();List<Guide.Problem> problems=new ArrayList<>();
            JSONArray a=o.getJSONArray("steps");for(int j=0;j<a.length();j++){JSONObject s=a.getJSONObject(j);steps.add(new Guide.Step(s.getString("title"),s.getString("text")));}
            a=o.getJSONArray("problems");for(int j=0;j<a.length();j++){JSONObject p=a.getJSONObject(j);problems.add(new Guide.Problem(p.getString("problem"),p.getString("fix")));}
            Guide g=new Guide(o.getString("id"),o.getString("title"),o.getString("category"),o.getString("summary"),o.getString("mode"),
                o.getString("evidence"),o.getString("caution"),o.getString("outcome"),o.optString("route",""),
                strings(o.getJSONArray("aliases")),strings(o.getJSONArray("ready")),steps,problems,strings(o.getJSONArray("finish")),strings(o.getJSONArray("sources")));
            if(byId.containsKey(g.id)||g.steps.isEmpty())throw new IllegalArgumentException("Invalid guide");
            for(String ref:g.sources)if(!sources.containsKey(ref))throw new IllegalArgumentException("Missing source");
            if(!g.route.isEmpty()&&!UrlPolicy.isAllowed(g.route))throw new IllegalArgumentException("Unapproved route");
            guides.add(g);byId.put(g.id,g);entries.add(g.searchEntry);categories.add(g.category);
        }
        if(guides.isEmpty())throw new IllegalArgumentException("Empty catalogue");
    }
}
