package ir.tarhplus.watch;

import org.junit.Test;
import static org.junit.Assert.*;

public class UrlPolicyTest {
    @Test public void defaultReportIsAllowed() { assertTrue(UrlPolicy.report(UrlPolicy.DEFAULT_REPORT)); }
    @Test public void officialHttpsSsoCanOpenButNotReceiveReportPost() {
        assertTrue(UrlPolicy.ministry("https://sso.behdasht.gov.ir/login"));
        assertFalse(UrlPolicy.report("https://sso.behdasht.gov.ir/WaitingQueueReport.aspx"));
    }
    @Test public void cleartextAndUnexpectedPortAreRejected() {
        assertFalse(UrlPolicy.report(UrlPolicy.DEFAULT_REPORT.replace("https:", "http:")));
        assertFalse(UrlPolicy.ministry("https://tarhplus.behdasht.gov.ir:8443/"));
    }
    @Test public void hostSpoofingAndUrlCredentialsAreRejected() {
        assertFalse(UrlPolicy.ministry("https://behdasht.gov.ir.attacker.example/"));
        assertFalse(UrlPolicy.ministry("https://notbehdasht.gov.ir/"));
        assertFalse(UrlPolicy.ministry("https://name@tarhplus.behdasht.gov.ir/"));
    }
    @Test public void formsMayOnlyPostBackToSameReportPath() {
        assertTrue(UrlPolicy.sameReport(UrlPolicy.DEFAULT_REPORT, UrlPolicy.DEFAULT_REPORT + "&page=2"));
        assertFalse(UrlPolicy.sameReport(UrlPolicy.DEFAULT_REPORT, UrlPolicy.HOME + "Other/WaitingQueueReport.aspx"));
        assertFalse(UrlPolicy.sameReport(UrlPolicy.DEFAULT_REPORT, UrlPolicy.HOME + "Delete.aspx"));
    }
    @Test public void invalidUriAndNonWebSchemesAreRejected() {
        assertFalse(UrlPolicy.ministry("javascript:alert(1)")); assertFalse(UrlPolicy.ministry("file:///etc/passwd"));
        assertFalse(UrlPolicy.ministry("https://bad host")); assertFalse(UrlPolicy.report(null));
    }
}
