package no.ssb.dlp.pseudo.core.util;

import com.google.crypto.tink.*;
import lombok.experimental.UtilityClass;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@UtilityClass
public class TinkUtil {

    public static String newWrappedKeyJson(String kekUri) {
        try {
            KeysetHandle keysetHandle = KeysetHandle.generateNew(KeyTemplates.get("AES256_SIV"));
            return toWrappedKeyJson(keysetHandle, kekUri);
        }
        catch (Exception e) {
            throw new RuntimeException("Error generating new wrapped key", e);
        }
    }

    public static String toKeyJson(KeysetHandle keysetHandle) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(baos));
        return new String(baos.toByteArray());
    }

    public static String toWrappedKeyJson(KeysetHandle keysetHandle, String kekUri) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keysetHandle.write(JsonKeysetWriter.withOutputStream(baos), KmsClients.get(kekUri).getAead(kekUri));

        CleartextKeysetHandle.write(keysetHandle, JsonKeysetWriter.withOutputStream(baos));
        return new String(baos.toByteArray());
    }

}
