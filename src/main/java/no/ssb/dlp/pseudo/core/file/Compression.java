package no.ssb.dlp.pseudo.core.file;

import io.micronaut.http.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor @AllArgsConstructor
public class Compression {
    private MediaType type;
    private CompressionEncryptionMethod encryption;
    private char[] password;

    public boolean zipCompressionEnabled() {
        return MoreMediaTypes.APPLICATION_ZIP_TYPE.equals(type);
    }
}
