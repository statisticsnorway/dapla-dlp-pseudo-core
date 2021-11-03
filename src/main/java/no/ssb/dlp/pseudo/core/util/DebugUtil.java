package no.ssb.dlp.pseudo.core.util;

public class DebugUtil {

    private DebugUtil() {}

    public static boolean isDebugMode() {
        return System.getProperty("dapla.pseudo.debug").equals("true");
    }

}
