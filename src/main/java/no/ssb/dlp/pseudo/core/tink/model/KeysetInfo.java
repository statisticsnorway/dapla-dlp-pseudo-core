package no.ssb.dlp.pseudo.core.tink.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class KeysetInfo {
    private Integer primaryKeyId;
    private List<KeyInfo> keyInfo = new ArrayList<>();
}
