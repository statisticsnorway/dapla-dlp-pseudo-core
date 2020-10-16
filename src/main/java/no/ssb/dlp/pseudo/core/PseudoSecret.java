package no.ssb.dlp.pseudo.core;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Base64;


@Data
@NoArgsConstructor
public class PseudoSecret implements Serializable {
    private String id;
    private byte[] content;
    private String type;

    public PseudoSecret(String id, String base64EncodedContent, String type) {
        this.id = id;
        try {
            this.content = Base64.getDecoder().decode(base64EncodedContent);
        }
        catch (IllegalArgumentException e) {
            throw new PseudoException("Invalid secret. Must be a base64 encoded string");
        }
        this.type = type;
    }

    public String getBase64EncodedContent() {
        return Base64.getEncoder().encodeToString(content);
    }

}
