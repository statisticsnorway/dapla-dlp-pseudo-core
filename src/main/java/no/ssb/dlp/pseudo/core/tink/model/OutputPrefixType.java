package no.ssb.dlp.pseudo.core.tink.model;

public enum OutputPrefixType {
    UNKNOWN_PREFIX, TINK, LEGACY, RAW, CRUNCHY, UNRECOGNIZED;

    public static OutputPrefixType from(com.google.crypto.tink.proto.OutputPrefixType s) {
        return OutputPrefixType.valueOf(s.name());
    }

}
