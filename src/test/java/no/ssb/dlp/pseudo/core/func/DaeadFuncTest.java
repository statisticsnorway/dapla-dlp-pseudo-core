package no.ssb.dlp.pseudo.core.func;

import com.google.common.collect.ImmutableList;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import no.ssb.dapla.dlp.pseudo.func.*;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DaeadFuncTest {

    @BeforeAll
    static void init() throws Exception {
        DeterministicAeadConfig.register();
    }

    private final static String KEYSET_JSON_AES256_SIV = "{\"primaryKeyId\":1284924461,\"key\":[{\"keyData\":{\"typeUrl\":\"type.googleapis.com/google.crypto.tink.AesSivKey\",\"value\":\"EkCIjYUrKTTMAxEZST8xoyBXrfSLtTt+XmfBcE/PQxhr1Ob+YdD84bSMPQDaTGMqD241C4J7oQ+w3RFXaC8vKzbI\",\"keyMaterialType\":\"SYMMETRIC\"},\"status\":\"ENABLED\",\"keyId\":1284924461,\"outputPrefixType\":\"TINK\"}]}";

    private final static Map<String, String> KEYSETS = Map.of(
            "1284924461", KEYSET_JSON_AES256_SIV
    );

    private DeterministicAead daeadPrimitive(String keyId) throws Exception {
        if (! KEYSETS.containsKey(keyId)) {
            throw new RuntimeException("Unknown keyId: " + keyId);
        }

        KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(KEYSETS.get(keyId)));
        return keysetHandle.getPrimitive(DeterministicAead.class);
    }

    private PseudoFunc f(String funcDecl) throws Exception {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        String keyId = config.getRequired(TinkFpeFuncConfig.Param.KEY_ID, String.class);
        config.add(TinkDaeadFuncConfig.Param.DAEAD, daeadPrimitive(keyId));
        return PseudoFuncFactory.create(config);
    }

    private void transformAndRestore(Object originalVal, Object expectedVal, PseudoFunc func) {
        Iterable expectedElements = (expectedVal instanceof Iterable) ? (Iterable) expectedVal : ImmutableList.of(expectedVal);
        Iterable originalElements = (originalVal instanceof Iterable) ? (Iterable) originalVal : ImmutableList.of(originalVal);
        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of(originalVal));
        assertThat(pseudonymized.getValues()).containsExactlyElementsOf(expectedElements);
        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of(pseudonymized.getValues()));
        assertThat(depseudonymized.getValues()).containsExactlyElementsOf(originalElements);
    }

    @Test
    void givenText_daead_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "daead(keyId=1284924461)";
        transformAndRestore("Something", "AUyWZC3OtYjeblGN+jZeR4w6alLoxuSsaigbZ+am", f(funcDeclStr));
    }

}
