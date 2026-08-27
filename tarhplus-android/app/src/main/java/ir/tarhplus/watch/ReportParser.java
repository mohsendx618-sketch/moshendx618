package ir.tarhplus.watch;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/** Conservative parser: an unknown, partial or login page is never a negative result. */
public final class ReportParser {
    public enum State { MATCH, NONE, LOGIN_REQUIRED, UNKNOWN }
    public static final class Page {
        public final State state;
        public final List<String> matches;
        public final List<Element> tables;
        public final String signature;
        Page(State state, List<String> matches, List<Element> tables, String signature) {
            this.state = state; this.matches = matches; this.tables = tables; this.signature = signature;
        }
    }
    private ReportParser() {}

    public static String normalize(String input) {
        if (input == null) return "";
        String s = input.replace('ي', 'ی').replace('ى', 'ی').replace('ك', 'ک')
                .replace('آ', 'ا').replace('أ', 'ا').replace('إ', 'ا')
                .replace('\u200c', ' ').replace('\u200d', ' ').replace('\u00a0', ' ')
                .replace("ـ", "").replaceAll("[\\u064B-\\u065F\\u0670]", "");
        for (int i = 0; i < 10; i++) {
            s = s.replace((char) ('۰' + i), (char) ('0' + i)).replace((char) ('٠' + i), (char) ('0' + i));
        }
        return s.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    static boolean containsTerm(String text, String term) {
        String n = normalize(term);
        return !n.isEmpty() && Pattern.compile("(?<![\\p{L}\\p{N}])" + Pattern.quote(n)
                + "(?![\\p{L}\\p{N}])").matcher(normalize(text)).find();
    }

    public static Page parse(String html, String city, String specialty) {
        return parse(Jsoup.parse(html), city, specialty);
    }

    public static Page parse(Document doc, String city, String specialty) {
        String body = normalize(doc.text());
        if (!doc.select("input[type=password]").isEmpty() || body.contains("نشست شما منقضی")
                || body.contains("لطفا مجددا وارد") || body.contains("دوباره وارد شوید")) {
            return new Page(State.LOGIN_REQUIRED, Collections.emptyList(), Collections.emptyList(), "login");
        }
        if (body.contains("access denied") || body.contains("service unavailable")
                || body.contains("checking your browser") || body.contains("خطای سرور")) {
            return new Page(State.UNKNOWN, Collections.emptyList(), Collections.emptyList(), "error");
        }
        Set<String> matches = new LinkedHashSet<>();
        List<Element> reports = new ArrayList<>();
        List<String> allRows = new ArrayList<>();
        for (Element table : doc.select("table")) {
            Elements rows = directRows(table);
            int headerAt = -1, cityIndex = -1, specialtyIndex = -1;
            for (int ri = 0; ri < Math.min(4, rows.size()); ri++) {
                Elements cells = cells(rows.get(ri));
                if (cells.isEmpty() || !rows.get(ri).select("table").isEmpty()) continue;
                String header = normalize(rows.get(ri).text());
                boolean place = header.contains("شهر") || header.contains("محل خدمت") || header.contains("محل مورد نیاز");
                boolean profession = header.contains("رشته") || header.contains("تخصص");
                if (place && profession) {
                    headerAt = ri;
                    for (int i = 0; i < cells.size(); i++) {
                        String h = normalize(cells.get(i).text());
                        if (h.equals("شهر") || h.contains("شهرستان")) cityIndex = i;
                        if (h.contains("رشته") || h.contains("تخصص")) specialtyIndex = i;
                    }
                    break;
                }
            }
            if (headerAt < 0) continue;
            reports.add(table);
            for (int ri = headerAt + 1; ri < rows.size(); ri++) {
                Element row = rows.get(ri);
                Elements cells = cells(row);
                if (cells.size() < 2 || !row.select("table").isEmpty()) continue;
                String text = row.text().trim();
                allRows.add(normalize(text));
                boolean cityMatch = containsTerm(text, city);
                if (cityIndex >= 0 && cityIndex < cells.size()) {
                    String place = normalize(cells.get(cityIndex).text()).replaceFirst("^(شهرستان|شهر)\\s+", "");
                    cityMatch = place.equals(normalize(city));
                } else if (normalize(city).equals("اباده") && normalize(text).contains("اباده طشک")) {
                    cityMatch = false;
                }
                boolean specialtyMatch = specialtyIndex >= 0 && specialtyIndex < cells.size()
                        && containsTerm(cells.get(specialtyIndex).text(), specialty);
                if (cityMatch && specialtyMatch) matches.add(text);
            }
        }
        State state = reports.isEmpty() || allRows.isEmpty() ? State.UNKNOWN : matches.isEmpty() ? State.NONE : State.MATCH;
        // A known, explicit empty report can be valid even when the grid is not rendered.
        if (allRows.isEmpty() && body.contains("اعلام نیاز") &&
                (body.contains("رکوردی یافت نشد") || body.contains("اطلاعاتی یافت نشد") || body.contains("موردی یافت نشد"))) {
            state = State.NONE;
        }
        return new Page(state, new ArrayList<>(matches), reports, fingerprint(allRows));
    }

    private static Elements directRows(Element table) {
        Elements result = new Elements();
        for (Element row : table.select("tr")) {
            Element ancestor = row.parent();
            while (ancestor != null && !ancestor.tagName().equals("table")) ancestor = ancestor.parent();
            if (ancestor == table) result.add(row);
        }
        return result;
    }

    private static Elements cells(Element row) {
        Elements result = new Elements();
        for (Element e : row.children()) if (e.tagName().equals("td") || e.tagName().equals("th")) result.add(e);
        return result;
    }

    public static String fingerprint(List<String> rows) {
        try {
            List<String> ordered = new ArrayList<>();
            for (String r : rows) ordered.add(normalize(r));
            Collections.sort(ordered);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(String.join("\n", ordered).getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder();
            for (byte b : digest) out.append(String.format(Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (Exception e) { throw new IllegalStateException("SHA-256 unavailable", e); }
    }
}
