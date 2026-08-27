package ir.tarhplus.watch;

import java.net.URI;
import java.util.Locale;

public final class UrlPolicy {
    public static final String HOME = "https://tarhplus.behdasht.gov.ir/";
    public static final String DEFAULT_REPORT = HOME + "ApplicationPool7/Jazb/PortalPlus/WaitingQueueReport.aspx?FarakhanID=27";
    private UrlPolicy() {}

    public static boolean ministry(String value) {
        try {
            URI u = new URI(value);
            String host = u.getHost() == null ? "" : u.getHost().toLowerCase(Locale.ROOT);
            return "https".equalsIgnoreCase(u.getScheme()) && u.getUserInfo() == null
                    && (u.getPort() == -1 || u.getPort() == 443)
                    && (host.equals("behdasht.gov.ir") || host.endsWith(".behdasht.gov.ir"));
        } catch (Exception e) { return false; }
    }

    public static boolean report(String value) {
        try {
            URI u = new URI(value);
            return ministry(value) && "tarhplus.behdasht.gov.ir".equalsIgnoreCase(u.getHost())
                    && u.getPath() != null && u.getPath().toLowerCase(Locale.ROOT).endsWith("waitingqueuereport.aspx");
        } catch (Exception e) { return false; }
    }

    public static boolean sameReport(String from, String to) {
        try {
            return report(from) && report(to) && new URI(from).getPath().equals(new URI(to).getPath());
        } catch (Exception e) { return false; }
    }
}
