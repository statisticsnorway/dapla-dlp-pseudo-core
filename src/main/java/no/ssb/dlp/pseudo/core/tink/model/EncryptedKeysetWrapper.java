package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;
import no.ssb.dlp.pseudo.core.PseudoKeyset;
import no.ssb.dlp.pseudo.core.util.Json;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Data
public class EncryptedKeysetWrapper implements PseudoKeyset {
    private URI kekUri;
    private String encryptedKeyset;
    private KeysetInfo keysetInfo;

    @Override
    public String primaryKeyId() {
        return keysetInfo.getPrimaryKeyId().toString();
    }

    @Override
    public Set<String> keyIds() {
        return keysetInfo.getKeyInfo().stream()
                .map(keyInfo -> keyInfo.getKeyId().toString())
                .collect(Collectors.toSet());
    }

    @Override
    public String toJson() {
        return Json.from(Map.of(
                "encryptedKeyset", encryptedKeyset,
                "keysetInfo", keysetInfo
        ));
    }

}
