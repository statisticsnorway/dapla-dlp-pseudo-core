package no.ssb.dlp.pseudo.core;

import java.net.URI;
import java.util.Set;

public interface PseudoKeyset {
    String primaryKeyId();
    Set<String> keyIds();

    URI getKekUri();

    String toJson();

}
