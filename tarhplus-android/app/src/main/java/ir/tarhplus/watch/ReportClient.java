package ir.tarhplus.watch;

import android.webkit.CookieManager;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

final class ReportClient {
    private static final Pattern POSTBACK = Pattern.compile("__doPostBack\\(\\s*['\"]([A-Za-z0-9_$:.-]+)['\"]\\s*,\\s*['\"]Page\\$(\\d+|Next|Last|First|Prev)['\"]\\s*\\)");
    static final class Scan {
        final ReportParser.State state;
        final List<String> matches;
        final int pages;
        Scan(ReportParser.State state, List<String> matches, int pages) {
            this.state = state; this.matches = matches; this.pages = pages;
        }
    }
    static final class LoginNeeded extends IOException {}
    private final String userAgent;
    ReportClient(String userAgent) { this.userAgent = userAgent; }

    Scan scan(String url, String city, String major) throws IOException {
        Set<String> matches = new LinkedHashSet<>(), visited = new HashSet<>(), signatures = new HashSet<>();
        Document doc = fetch(url, null);
        long deadline = System.currentTimeMillis() + 120_000;
        for (int count = 1; count <= 30; count++) {
            ReportParser.Page page = ReportParser.parse(doc, city, major);
            if (page.state == ReportParser.State.UNKNOWN || page.state == ReportParser.State.LOGIN_REQUIRED)
                return new Scan(page.state, new ArrayList<>(matches), count);
            matches.addAll(page.matches);
            if (!signatures.add(page.signature)) return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
            List<Next> next = new ArrayList<>();
            boolean unsupported = false, lastAvailable = false;
            for (Element table : page.tables) {
                for (Element link : table.select("a[href], a[onclick]")) {
                    String script = link.attr("href") + " " + link.attr("onclick");
                    Matcher m = POSTBACK.matcher(script);
                    if (m.find()) {
                        String target = m.group(1), command = m.group(2);
                        if (command.equals("Last")) { lastAvailable = true; continue; }
                        if (command.equals("First") || command.equals("Prev")) continue;
                        String key = target + ":" + command;
                        if (count == 1 && command.equals("1")) { visited.add(key); continue; }
                        if (!command.equals("Next") && visited.contains(key)) continue;
                        next.add(new Next(link, target, command, key, command.equals("Next") ? Integer.MAX_VALUE : Integer.parseInt(command)));
                    } else if (isPager(link)) {
                        // Non-WebForms pagers need site-specific support. Never claim a complete scan.
                        unsupported = true;
                    }
                }
            }
            if (unsupported) return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
            if (next.isEmpty()) {
                if (lastAvailable) return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
                return new Scan(matches.isEmpty() ? ReportParser.State.NONE : ReportParser.State.MATCH, new ArrayList<>(matches), count);
            }
            if (count == 30 || System.currentTimeMillis() >= deadline)
                return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
            next.sort(Comparator.comparingInt(n -> n.order));
            Next selected = next.get(0);
            visited.add(selected.key);
            Element form = selected.link.closest("form");
            if (form == null) return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
            String action = form.absUrl("action");
            if (action.isEmpty()) action = doc.location();
            if (!UrlPolicy.sameReport(url, action)) return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), count);
            Map<String, String> fields = new LinkedHashMap<>();
            for (Element input : form.select("input[name]:not([disabled])")) {
                String type = input.attr("type").toLowerCase(java.util.Locale.ROOT);
                if (type.equals("hidden") || type.equals("text") || type.equals("search")) fields.put(input.attr("name"), input.val());
                if ((type.equals("radio") || type.equals("checkbox")) && input.hasAttr("checked")) fields.put(input.attr("name"), input.val());
            }
            for (Element select : form.select("select[name]:not([disabled])")) fields.put(select.attr("name"), select.val());
            fields.put("__EVENTTARGET", selected.target);
            fields.put("__EVENTARGUMENT", "Page$" + selected.command);
            fields.remove("__ASYNCPOST");
            doc = fetch(action, fields);
        }
        return new Scan(ReportParser.State.UNKNOWN, new ArrayList<>(matches), 30);
    }

    private static boolean isPager(Element link) {
        String text = ReportParser.normalize(link.text());
        boolean navigation = text.matches("\\d+|[<>»«‹›…]+|next|previous|بعدی|قبلی") || text.contains("صفحه بعد");
        return navigation && (link.closest("td[colspan]") != null || link.closest(".pager, .pagination, .rgPager") != null);
    }

    private static final class Next {
        final Element link;
        final String target, command, key;
        final int order;
        Next(Element link, String target, String command, String key, int order) {
            this.link = link; this.target = target; this.command = command; this.key = key; this.order = order;
        }
    }

    private Document fetch(String url, Map<String, String> fields) throws IOException {
        for (int redirect = 0; redirect <= 4; redirect++) {
            if (!UrlPolicy.report(url)) throw new LoginNeeded();
            HttpsURLConnection connection = (HttpsURLConnection) new URL(url).openConnection();
            try {
                connection.setConnectTimeout(20_000);
                connection.setReadTimeout(25_000);
                connection.setInstanceFollowRedirects(false);
                connection.setRequestProperty("Accept", "text/html,application/xhtml+xml");
                if (!userAgent.isEmpty()) connection.setRequestProperty("User-Agent", userAgent);
                String cookie = CookieManager.getInstance().getCookie(url);
                if (cookie != null) connection.setRequestProperty("Cookie", cookie);
                if (fields != null) {
                    StringBuilder body = new StringBuilder();
                    for (Map.Entry<String, String> e : fields.entrySet()) {
                        if (body.length() > 0) body.append('&');
                        body.append(URLEncoder.encode(e.getKey(), "UTF-8")).append('=').append(URLEncoder.encode(e.getValue(), "UTF-8"));
                    }
                    byte[] bytes = body.toString().getBytes(StandardCharsets.UTF_8);
                    connection.setRequestMethod("POST");
                    connection.setDoOutput(true);
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    connection.setFixedLengthStreamingMode(bytes.length);
                    try (java.io.OutputStream output = connection.getOutputStream()) { output.write(bytes); }
                }
                int code = connection.getResponseCode();
                for (Map.Entry<String, List<String>> h : connection.getHeaderFields().entrySet()) {
                    if ("Set-Cookie".equalsIgnoreCase(h.getKey()))
                        for (String value : h.getValue()) CookieManager.getInstance().setCookie(url, value);
                }
                CookieManager.getInstance().flush();
                if (code == 401) throw new LoginNeeded();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Invalid redirect");
                    String destination = new URL(new URL(url), location).toString();
                    if (!UrlPolicy.sameReport(url, destination)) throw new LoginNeeded();
                    url = destination;
                    if (code != 307 && code != 308) fields = null;
                    continue;
                }
                if (code != 200) throw new IOException("Report request unsuccessful");
                byte[] bytes;
                try (InputStream input = connection.getInputStream(); ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                    byte[] block = new byte[8192];
                    int read;
                    while ((read = input.read(block)) != -1) {
                        if (buffer.size() + read > 4 * 1024 * 1024) throw new IOException("Report too large");
                        buffer.write(block, 0, read);
                    }
                    bytes = buffer.toByteArray();
                }
                String contentType = connection.getContentType();
                String charset = null;
                if (contentType != null) {
                    Matcher m = Pattern.compile("charset=\\s*[\"']?([A-Za-z0-9_-]+)", Pattern.CASE_INSENSITIVE).matcher(contentType);
                    if (m.find() && java.nio.charset.Charset.isSupported(m.group(1))) charset = m.group(1);
                }
                return Jsoup.parse(new ByteArrayInputStream(bytes), charset, url);
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many redirects");
    }
}
