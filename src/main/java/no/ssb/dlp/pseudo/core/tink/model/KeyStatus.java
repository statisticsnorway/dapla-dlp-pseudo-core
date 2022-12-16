package no.ssb.dlp.pseudo.core.tink.model;

import com.google.crypto.tink.proto.KeyStatusType;
import com.google.crypto.tink.proto.KeysetInfo;

public enum KeyStatus {
    UNKNOWN_STATUS, ENABLED, DISABLED, DESTROYED, UNRECOGNIZED;

    public static KeyStatus from(com.google.crypto.tink.proto.KeyStatusType s) {
        return KeyStatus.valueOf(s.name());
    }
}
