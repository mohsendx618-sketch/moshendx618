package ir.tarhplus.watch;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

public class ReportClientTest {
    private static Document page(int number, int total, String city) {
        StringBuilder links = new StringBuilder();
        for (int n = 1; n <= total; n++) {
            if (n == number) links.append("<span>").append(n).append("</span>");
            else links.append("<a href=\"javascript:__doPostBack('grid','Page$").append(n).append("')\">").append(n).append("</a>");
        }
        return Jsoup.parse("<form action='WaitingQueueReport.aspx?FarakhanID=27' method='post'>"
                + "<input type='hidden' name='__VIEWSTATE' value='state-" + number + "'>"
                + "<table><tr><th>شهرستان</th><th>رشته</th><th>ظرفیت</th></tr>"
                + "<tr><td>" + city + "</td><td>اتاق عمل</td><td>" + number + "</td></tr>"
                + "<tr><td colspan='3'>" + links + "</td></tr></table></form>", UrlPolicy.DEFAULT_REPORT);
    }

    @Test public void traversesAllPagesWithoutReturningToFirstAndReplaysFreshViewstate() throws Exception {
        AtomicInteger fetched = new AtomicInteger();
        ReportClient client = new ReportClient("", (url, fields) -> {
            int number = fetched.incrementAndGet();
            if (number == 1) assertNull(fields);
            else {
                assertEquals("grid", fields.get("__EVENTTARGET"));
                assertEquals("Page$" + number, fields.get("__EVENTARGUMENT"));
                assertEquals("state-" + (number - 1), fields.get("__VIEWSTATE"));
            }
            assertTrue(number <= 3);
            return page(number, 3, number == 2 ? "آباده" : "شیراز");
        });
        ReportClient.Scan result = client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل");
        assertEquals(ReportParser.State.MATCH, result.state);
        assertEquals(3, result.pages);
        assertEquals(1, result.matches.size());
    }

    @Test public void negativeResultRequiresEveryPage() throws Exception {
        AtomicInteger fetched = new AtomicInteger();
        ReportClient client = new ReportClient("", (url, fields) -> page(fetched.incrementAndGet(), 3, "شیراز"));
        ReportClient.Scan result = client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل");
        assertEquals(ReportParser.State.NONE, result.state);
        assertEquals(3, fetched.get());
    }

    @Test public void loginOnSecondPageIsNotNegative() throws Exception {
        ReportClient client = new ReportClient("", (url, fields) -> fields == null ? page(1, 2, "شیراز") : Jsoup.parse("<input type=password>"));
        assertEquals(ReportParser.State.LOGIN_REQUIRED, client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل").state);
    }

    @Test public void repeatedPageIsIncompleteNotNegative() throws Exception {
        ReportClient client = new ReportClient("", (url, fields) -> page(1, 2, "شیراز"));
        assertEquals(ReportParser.State.UNKNOWN, client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل").state);
    }

    @Test public void unsupportedPagerIsIncompleteNotNegative() throws Exception {
        Document document = page(1, 1, "شیراز");
        document.selectFirst("td[colspan]").html("<a href='?page=2'>۲</a>");
        ReportClient client = new ReportClient("", (url, fields) -> document);
        assertEquals(ReportParser.State.UNKNOWN, client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل").state);
    }

    @Test(expected = IOException.class) public void failedLaterPagePropagatesInsteadOfClaimingNoMatch() throws Exception {
        ReportClient client = new ReportClient("", (url, fields) -> {
            if (fields == null) return page(1, 2, "شیراز");
            throw new IOException("fixture network failure");
        });
        client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل");
    }

    @Test public void hostileFormCannotReceiveSessionFields() throws Exception {
        Document document = page(1, 2, "شیراز");
        document.selectFirst("form").attr("action", "https://attacker.example/WaitingQueueReport.aspx");
        ReportClient client = new ReportClient("", (url, fields) -> { assertNull(fields); return document; });
        assertEquals(ReportParser.State.UNKNOWN, client.scan(UrlPolicy.DEFAULT_REPORT, "آباده", "اتاق عمل").state);
    }
}
