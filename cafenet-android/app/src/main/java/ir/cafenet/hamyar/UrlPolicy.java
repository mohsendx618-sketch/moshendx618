package ir.cafenet.hamyar;

import java.net.URI;
import java.util.Locale;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

final class UrlPolicy {
    private static final Set<String> HOSTS=new HashSet<>(Arrays.asList(
        "post.ir","www.post.ir","gnaf.post.ir","tracking.post.ir","khadamat.mardom.ir",
        "tamin.ir","www.tamin.ir","es.tamin.ir","my.medu.ir",
        "irantvto.ir","yazd.irantvto.ir","sanjesh.irantvto.ir","tehran.irantvto.ir",
        "portaltvto.com","azmoon.portaltvto.com","certificate.portaltvto.com","pay.portaltvto.com",
        "support.microsoft.com","support.google.com","mail.google.com","docs.gimp.org","www.gimp.org",
        "pdfsam.org","blog.pdfsam.org",
        "my.tax.gov.ir","tax.gov.ir","intamedia.ir","tp.tax.gov.ir","salary.tax.gov.ir",
        "www.nezamqom.ir","acco.ir","chargoon.com","www.sppcco.com","shenasname.ir",
        "irnotary.ir","way2pay.ir","ec.iau.ir","www.scribd.com","www.ekhtebar.ir"
    ));
    static boolean isAllowed(String value) {
        try {
            URI u=new URI(value);
            return "https".equalsIgnoreCase(u.getScheme())&&u.getUserInfo()==null
                &&(u.getPort()==-1||u.getPort()==443)&&u.getHost()!=null
                &&HOSTS.contains(u.getHost().toLowerCase(Locale.ROOT));
        } catch(Exception e) {return false;}
    }
}
