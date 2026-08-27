package ir.cafenet.hamyar;

import org.junit.Test;
import static org.junit.Assert.*;

public class UrlPolicyTest {
    @Test public void permitsOfficialPost(){assertTrue(UrlPolicy.isAllowed("https://gnaf.post.ir"));}
    @Test public void permitsOfficialReference(){assertTrue(UrlPolicy.isAllowed("https://support.google.com/chrome/answer/1069693?hl=en"));}
    @Test public void permitsPersianPdfPath(){assertTrue(UrlPolicy.isAllowed("https://tehran.irantvto.ir/uploads/203/آزمون/فرایند%20پرداخت.pdf"));}
    @Test public void permitsHttpsCase(){assertTrue(UrlPolicy.isAllowed("HTTPS://POST.IR"));}
    @Test public void rejectsHttp(){assertFalse(UrlPolicy.isAllowed("http://post.ir"));}
    @Test public void rejectsFakeSuffix(){assertFalse(UrlPolicy.isAllowed("https://post.ir.evil.test"));}
    @Test public void rejectsUserInfo(){assertFalse(UrlPolicy.isAllowed("https://customer@post.ir"));}
    @Test public void rejectsPhishingUserInfo(){assertFalse(UrlPolicy.isAllowed("https://post.ir@evil.test"));}
    @Test public void rejectsUnexpectedPort(){assertFalse(UrlPolicy.isAllowed("https://post.ir:8443"));}
    @Test public void rejectsJavascript(){assertFalse(UrlPolicy.isAllowed("javascript:alert(1)"));}
    @Test public void rejectsFileAndIntent(){assertFalse(UrlPolicy.isAllowed("file:///sdcard/customer.pdf"));assertFalse(UrlPolicy.isAllowed("intent://post.ir"));}
    @Test public void rejectsUnknownDomains(){assertFalse(UrlPolicy.isAllowed("https://example.com"));assertFalse(UrlPolicy.isAllowed("https://localhost"));}
    @Test public void rejectsNullAndMalformed(){assertFalse(UrlPolicy.isAllowed(null));assertFalse(UrlPolicy.isAllowed("https://post.ir bad"));}
    @Test public void permitsOfficialTaxPortal(){assertTrue(UrlPolicy.isAllowed("https://my.tax.gov.ir"));assertTrue(UrlPolicy.isAllowed("https://tp.tax.gov.ir"));}
    @Test public void permitsDisclosedArchive(){assertTrue(UrlPolicy.isAllowed("https://chargoon.com/wp-content/uploads/2025/12/سند.pdf"));}
    @Test public void rejectsTaxLookalikes(){assertFalse(UrlPolicy.isAllowed("https://my-tax.gov.ir"));assertFalse(UrlPolicy.isAllowed("https://my.tax.gov.ir.evil.test"));assertFalse(UrlPolicy.isAllowed("https://my.tax.gov.ir@evil.test"));}
    @Test public void rejectsUnknownTaxSubdomain(){assertFalse(UrlPolicy.isAllowed("https://random.tax.gov.ir"));}
}
