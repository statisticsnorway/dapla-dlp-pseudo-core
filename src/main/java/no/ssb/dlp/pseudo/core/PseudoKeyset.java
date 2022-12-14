package no.ssb.dlp.pseudo.core;

import java.util.Set;

public interface PseudoKeyset {
    String getPrimaryKeyId();
    Set<String> getKeyIds();

    String getKekUri();

    String toJson();

}
