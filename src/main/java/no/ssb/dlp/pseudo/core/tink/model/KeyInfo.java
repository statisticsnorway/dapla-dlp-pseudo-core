package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;

@Data
public class KeyInfo {
    private String typeUrl;
    private KeyStatus status;
    private Integer keyId;
    private OutputPrefixType outputPrefixType;

    public static KeyInfo from(com.google.crypto.tink.proto.KeysetInfo.KeyInfo i) {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setKeyId(i.getKeyId());
        keyInfo.setTypeUrl(i.getTypeUrl());
        keyInfo.setStatus(KeyStatus.from(i.getStatus()));
        keyInfo.setOutputPrefixType(OutputPrefixType.from(i.getOutputPrefixType()));
        return keyInfo;
    }
}
