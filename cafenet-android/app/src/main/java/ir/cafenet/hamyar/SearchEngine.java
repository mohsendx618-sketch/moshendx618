package ir.cafenet.hamyar;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/** Pure local, deterministic Persian full-text search. No Android or network dependencies. */
final class SearchEngine {
    private static final Set<String> FILLERS=new HashSet<>(Arrays.asList("چطور","چگونه","چجوری","چجور","میخوام","بگیرم","برای","رو","را","آموزش","اموزش"));
    static String normalize(String input) {
        if(input==null) return "";
        String s=Normalizer.normalize(input,Normalizer.Form.NFKD).toLowerCase(Locale.ROOT);
        StringBuilder b=new StringBuilder();
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(Character.getType(c)==Character.NON_SPACING_MARK || c=='\u0640') continue;
            if(c=='ي'||c=='ى') c='ی';
            if(c=='ك') c='ک';
            if(c=='ة') c='ه';
            if(c>='۰'&&c<='۹') c=(char)('0'+c-'۰');
            if(c>='٠'&&c<='٩') c=(char)('0'+c-'٠');
            b.append(Character.isLetterOrDigit(c)?c:' ');
        }
        return b.toString().trim().replaceAll("\\s+"," ");
    }
    static String compact(String s) { return normalize(s).replace(" ",""); }
    private static List<String> terms(String q) {
        List<String> out=new ArrayList<>();
        for(String token:normalize(q).split(" ")) if(!token.isEmpty()&&!FILLERS.contains(token)) out.add(token);
        return out;
    }
    static final class Section {
        final int tab;
        final String text,normalized,joined;
        Section(int tab,String text) {this.tab=tab;this.text=text;normalized=normalize(text);joined=normalized.replace(" ","");}
    }
    static final class Entry {
        final String id,title,category,titleKey,aliasKey,allKey;
        final List<Section> sections;
        Entry(String id,String title,String category,String aliases,List<Section> sections) {
            this.id=id;this.title=title;this.category=category;this.sections=Collections.unmodifiableList(new ArrayList<>(sections));
            titleKey=compact(title);aliasKey=compact(aliases);
            StringBuilder full=new StringBuilder(titleKey+" "+aliasKey+" "+compact(category));
            for(Section s:sections) full.append(' ').append(s.joined);
            allKey=full.toString();
        }
    }
    static final class Hit {
        final Entry entry;
        final int score,tab;
        final String snippet;
        Hit(Entry entry,int score,int tab,String snippet) {this.entry=entry;this.score=score;this.tab=tab;this.snippet=snippet;}
    }
    static List<Hit> search(List<Entry> entries,String query) {
        List<String> terms=terms(query);
        String phrase=String.join("",terms);
        List<Hit> hits=new ArrayList<>();
        for(Entry e:entries) {
            if(terms.isEmpty()) {hits.add(new Hit(e,0,1,""));continue;}
            boolean matches=true;
            for(String t:terms) if(!e.allKey.contains(t)) {matches=false;break;}
            if(!matches) continue;
            int score=0;
            if(e.titleKey.equals(phrase)) score+=240;
            else if(e.titleKey.contains(phrase)) score+=140;
            if(e.aliasKey.contains(phrase)) score+=100;
            for(String t:terms) {
                if(e.titleKey.contains(t)) score+=24;
                if(e.aliasKey.contains(t)) score+=16;
            }
            int bestScore=0,tab=1;String snippet="";
            for(Section s:e.sections) {
                int n=0;
                for(String t:terms) if(s.joined.contains(t)) n+=2;
                if(s.joined.contains(phrase)) n+=5;
                if(n>bestScore) {bestScore=n;snippet=s.text;tab=s.tab;}
            }
            // A title/alias hit should open the instructions; a body-only hit opens its exact section.
            if(e.titleKey.contains(phrase)||e.aliasKey.contains(phrase)) tab=1;
            hits.add(new Hit(e,score+bestScore,tab,snippet));
        }
        hits.sort(Comparator.comparingInt((Hit h)->h.score).reversed());
        return hits;
    }
}
