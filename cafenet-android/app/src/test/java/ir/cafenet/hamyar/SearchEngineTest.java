package ir.cafenet.hamyar;

import org.junit.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import static org.junit.Assert.*;

public class SearchEngineTest {
    private SearchEngine.Entry cert(){
        return new SearchEngine.Entry("postal","دریافت گواهی کد پستی","پست و نشانی","postal تاییدیه آدرس",
            Arrays.asList(new SearchEngine.Section(0,"نشانی کامل و کدپستی ۱۰ رقمی"),
                new SearchEngine.Section(1,"شماره سفارش را نگه دار و رمز پویا را خود مشتری وارد کند"),
                new SearchEngine.Section(2,"پول کم شده ولی گواهی ندارم؛ وضعیت پرداخت قبلی را بررسی کن")));
    }
    private SearchEngine.Entry scan(){
        return new SearchEngine.Entry("scan","اسکن مدارک","اسکن و PDF","scanner scan",
            Arrays.asList(new SearchEngine.Section(1,"برای گواهی کد پستی مدارک اضافه اسکن نکن"),
                new SearchEngine.Section(2,"اسکنر وصل نیست؛ اتصال دستگاه را بررسی کن")));
    }
    private List<SearchEngine.Entry> all(){return Arrays.asList(scan(),cert());}
    @Test public void arabicLetters(){assertEquals("کد پستی",SearchEngine.normalize("كد پستي"));}
    @Test public void arabicMaksura(){assertEquals("ی",SearchEngine.normalize("ى"));}
    @Test public void persianDigits(){assertEquals("0123456789",SearchEngine.normalize("۰۱۲۳۴۵۶۷۸۹"));}
    @Test public void arabicDigits(){assertEquals("0123456789",SearchEngine.normalize("٠١٢٣٤٥٦٧٨٩"));}
    @Test public void zeroWidthAndWhitespace(){assertEquals("کد پستی",SearchEngine.normalize("  کد‌پستی\n"));}
    @Test public void diacritics(){assertEquals("گواهی",SearchEngine.normalize("گُواهی"));}
    @Test public void alefVariants(){assertEquals(SearchEngine.normalize("آموزش"),SearchEngine.normalize("اموزش"));}
    @Test public void englishCase(){assertEquals("pdf",SearchEngine.normalize("PDF"));}
    @Test public void punctuation(){assertEquals("کد پستی 10",SearchEngine.normalize("کد-پستی (۱۰)"));}
    @Test public void nullSafe(){assertEquals("",SearchEngine.normalize(null));}
    @Test public void emptyReturnsStableOrder(){List<SearchEngine.Hit> h=SearchEngine.search(all(),"");assertEquals(2,h.size());assertEquals("scan",h.get(0).entry.id);}
    @Test public void compactTitleSearch(){assertEquals("postal",SearchEngine.search(all(),"کدپستی").get(0).entry.id);}
    @Test public void spacedTitleSearch(){assertEquals("postal",SearchEngine.search(all(),"کد پستی").get(0).entry.id);}
    @Test public void arabicQuery(){assertEquals("postal",SearchEngine.search(all(),"گواهي كدپستي").get(0).entry.id);}
    @Test public void naturalQuestion(){assertEquals("postal",SearchEngine.search(all(),"چطور گواهی کدپستی بگیرم").get(0).entry.id);}
    @Test public void aliasSearch(){assertEquals("postal",SearchEngine.search(all(),"POSTAL").get(0).entry.id);}
    @Test public void synonyms(){assertEquals("postal",SearchEngine.search(all(),"تاییدیه آدرس").get(0).entry.id);}
    @Test public void allTermsRequired(){assertTrue(SearchEngine.search(all(),"اسکن ماهواره").isEmpty());}
    @Test public void bodyMatches(){assertEquals("postal",SearchEngine.search(all(),"رمزپویا").get(0).entry.id);}
    @Test public void troubleRoutesToTrouble(){SearchEngine.Hit h=SearchEngine.search(all(),"پول کم").get(0);assertEquals(2,h.tab);assertTrue(h.snippet.contains("پرداخت"));}
    @Test public void titleRoutesToSteps(){assertEquals(1,SearchEngine.search(all(),"کد پستی").get(0).tab);}
    @Test public void titleRanksAboveBody(){assertEquals("postal",SearchEngine.search(all(),"گواهی کد پستی").get(0).entry.id);}
    @Test public void categorySearch(){assertEquals("scan",SearchEngine.search(all(),"PDF").get(0).entry.id);}
    @Test public void numberSearch(){assertEquals("postal",SearchEngine.search(all(),"۱۰ رقمی").get(0).entry.id);}
    @Test public void noDocuments(){assertTrue(SearchEngine.search(Collections.emptyList(),"کد").isEmpty());}
    @Test public void punctuationOnlyIsEmptyQuery(){assertEquals(2,SearchEngine.search(all(),"؟؟؟").size());}
}
