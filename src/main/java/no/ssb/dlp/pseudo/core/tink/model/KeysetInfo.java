package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Data
public class KeysetInfo {
    private Integer primaryKeyId;
    private List<KeyInfo> keyInfo = new ArrayList<>();

    public static KeysetInfo from(com.google.crypto.tink.proto.KeysetInfo i) {
        KeysetInfo keysetInfo = new KeysetInfo();
        keysetInfo.setPrimaryKeyId(i.getPrimaryKeyId());
        keysetInfo.setKeyInfo(i.getKeyInfoList().stream()
                .map(k -> KeyInfo.from(k))
                .toList());
        return keysetInfo;
    }
}
