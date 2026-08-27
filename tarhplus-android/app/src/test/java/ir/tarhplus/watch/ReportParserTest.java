package ir.tarhplus.watch;

import org.junit.Test;
import java.util.Arrays;
import static org.junit.Assert.*;

public class ReportParserTest {
    private static String table(String rows) { return "<table><tr><th>شهرستان</th><th>رشته</th><th>ظرفیت</th></tr>" + rows + "</table>"; }
    private static String row(String city, String specialty) { return "<tr><td>" + city + "</td><td>" + specialty + "</td><td>۲</td></tr>"; }
    private static ReportParser.Page parse(String html) { return ReportParser.parse(html, "آباده", "اتاق عمل"); }
    @Test public void exactRowMatches() {
        ReportParser.Page result = parse(table(row("آباده", "اتاق عمل")));
        assertEquals(ReportParser.State.MATCH, result.state); assertEquals(1, result.matches.size());
    }
    @Test public void termsInDifferentRowsDoNotMatch() {
        assertEquals(ReportParser.State.NONE, parse(table(row("آباده", "پرستاری") + row("شیراز", "اتاق عمل"))).state);
    }
    @Test public void differentTownWithSamePrefixIsExcluded() {
        assertEquals(ReportParser.State.NONE, parse(table(row("آباده طشک", "اتاق عمل"))).state);
    }
    @Test public void normalizesArabicLettersMarksAndDigits() {
        assertEquals("ی ک اتاق عمل 123", ReportParser.normalize("ي ك اتاق‌عمل ۱۲٣"));
        assertEquals(ReportParser.State.MATCH, parse(table(row("اباده", "اتاق‌عمل"))).state);
    }
    @Test public void loginNeverBecomesNegative() {
        assertEquals(ReportParser.State.LOGIN_REQUIRED, parse("<input type=password>" + table(row("آباده", "اتاق عمل"))).state);
    }
    @Test public void textOnlyAndNavigationAreUnknown() {
        assertEquals(ReportParser.State.UNKNOWN, parse("<nav>آباده اتاق عمل</nav><p>خوش آمدید</p>").state);
    }
    @Test public void headerOnlyGridIsNotAssumedLoaded() { assertEquals(ReportParser.State.UNKNOWN, parse(table("")).state); }
    @Test public void explicitEmptyReportIsNegative() { assertEquals(ReportParser.State.NONE, parse("اعلام نیاز — رکوردی یافت نشد").state); }
    @Test public void unknownEmptyMessageIsNotAccepted() { assertEquals(ReportParser.State.UNKNOWN, parse("رکوردی یافت نشد").state); }
    @Test public void outerLayoutDoesNotJoinRowsOrDuplicateHits() {
        String layout = "<table><tr><td>" + table(row("آباده", "اتاق عمل")) + "</td></tr></table>";
        assertEquals(1, parse(layout).matches.size());
    }
    @Test public void knownServiceErrorOverridesTable() {
        assertEquals(ReportParser.State.UNKNOWN, parse("خطای سرور" + table(row("آباده", "اتاق عمل"))).state);
    }
    @Test public void fingerprintStableAcrossOrderingAndPersianVariants() {
        assertEquals(ReportParser.fingerprint(Arrays.asList("ي ۱۲", "آباده")), ReportParser.fingerprint(Arrays.asList("اباده", "ی 12")));
        assertNotEquals(ReportParser.fingerprint(Arrays.asList("آباده ۲")), ReportParser.fingerprint(Arrays.asList("آباده ۳")));
    }
    @Test public void specialtyRequiresWordBoundaries() {
        assertEquals(ReportParser.State.NONE, parse(table(row("آباده", "اتاق عملیات"))).state);
    }
    @Test public void cityPrefixLabelIsAccepted() {
        assertEquals(ReportParser.State.MATCH, parse(table(row("شهرستان آباده", "کارشناس اتاق عمل"))).state);
    }
}
