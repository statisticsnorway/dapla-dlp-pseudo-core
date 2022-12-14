package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;

@Data
public class KeyInfo {
    private String typeUrl;
    private KeyStatus status;
    private Integer keyId;
    private OutputPrefixType outputPrefixType;
}
