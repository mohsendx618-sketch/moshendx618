package ir.cafenet.hamyar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Guide {
    final String id, title, category, summary, mode, evidence, caution, outcome, route;
    final List<String> aliases, ready, finish, sources;
    final List<Step> steps;
    final List<Problem> problems;
    final SearchEngine.Entry searchEntry;

    Guide(String id, String title, String category, String summary, String mode,
          String evidence, String caution, String outcome, String route,
          List<String> aliases, List<String> ready, List<Step> steps,
          List<Problem> problems, List<String> finish, List<String> sources) {
        this.id=id; this.title=title; this.category=category; this.summary=summary;
        this.mode=mode; this.evidence=evidence; this.caution=caution; this.outcome=outcome; this.route=route;
        this.aliases=Collections.unmodifiableList(new ArrayList<>(aliases)); this.ready=Collections.unmodifiableList(new ArrayList<>(ready)); this.steps=Collections.unmodifiableList(new ArrayList<>(steps));
        this.problems=Collections.unmodifiableList(new ArrayList<>(problems)); this.finish=Collections.unmodifiableList(new ArrayList<>(finish)); this.sources=Collections.unmodifiableList(new ArrayList<>(sources));
        List<SearchEngine.Section> sections = new ArrayList<>();
        sections.add(new SearchEngine.Section(0, summary+" "+outcome+" "+String.join(" ",ready)));
        for (Step s : steps) sections.add(new SearchEngine.Section(1,s.title+" — "+s.text));
        for (Problem p : problems) sections.add(new SearchEngine.Section(2,p.problem+" — "+p.fix));
        sections.add(new SearchEngine.Section(1,String.join(" ",finish)));
        searchEntry=new SearchEngine.Entry(id,title,category,String.join(" ",aliases),sections);
    }
    static final class Step {
        final String title,text;
        Step(String title,String text) { this.title=title;this.text=text; }
    }
    static final class Problem {
        final String problem,fix;
        Problem(String problem,String fix) { this.problem=problem;this.fix=fix; }
    }
}
