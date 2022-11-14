package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.util.Json;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class EncryptedKeysetWrapper implements PseudoKeyset {
    private String encryptedKeyset;
    private KeysetInfo keysetInfo;

    private String kekUri;

//    @Override
    public String getPrimaryKeyId() {
        return keysetInfo.getPrimaryKeyId().toString();
    }

//    @Override
    public Set<String> getKeyIds() {
        return keysetInfo.getKeyInfo().stream()
                .map(keyInfo -> keyInfo.getKeyId().toString())
                .collect(Collectors.toSet());
    }

//    @Override
    public String toJson() {
        return Json.from(Map.of(
                "encryptedKeyset", encryptedKeyset,
                "keysetInfo", keysetInfo
        ));
    }

}
