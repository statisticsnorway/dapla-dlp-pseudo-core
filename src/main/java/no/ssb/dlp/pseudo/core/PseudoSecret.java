package no.ssb.dlp.pseudo.core;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Base64;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PseudoSecret implements Serializable {
    private String name;

    private String id;
    private String version;
    private byte[] content;
    private String type;

    public static class PseudoSecretBuilder {
        public PseudoSecretBuilder base64EncodedContent(String base64EncodedContent) {
            this.content = base64DecodedContentOf(base64EncodedContent.getBytes(StandardCharsets.UTF_8));
            return this;
        }
    }

    public String getBase64EncodedContent() {
        return Base64.getEncoder().encodeToString(content);
    }

    public void setBase64EncodedContent(String base64EncodedContent) {
        setBase64EncodedContent(base64EncodedContent.getBytes(StandardCharsets.UTF_8));
    }

    public void setBase64EncodedContent(byte[] base64EncodedContent) {
        this.content = base64DecodedContentOf(base64EncodedContent);
    }

    private static byte[] base64DecodedContentOf(byte[] base64EncodedContent) {
        try {
            return Base64.getDecoder().decode(base64EncodedContent);
        }
        catch (IllegalArgumentException e) {
            throw new PseudoException("Invalid secret. Content must be base64 encoded.");
        }
    }
}
