package no.ssb.dlp.pseudo.core;

import java.net.URI;
import java.util.Set;

public interface PseudoKeyset {
    String getPrimaryKeyId();
    Set<String> getKeyIds();

    URI getKekUri();

    String toJson();

}
