package no.ssb.dlp.pseudo.core.func;

import com.google.common.collect.ImmutableList;
import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import no.ssb.crypto.tink.fpe.Fpe;
import no.ssb.crypto.tink.fpe.FpeConfig;
import no.ssb.crypto.tink.fpe.IncompatiblePlaintextException;
import no.ssb.dapla.dlp.pseudo.func.*;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class Ff31FuncTest {

    @BeforeAll
    static void init() throws Exception {
        FpeConfig.register();
    }

    private final static String KEYSET_JSON_FF31_256_ALPHANUMERIC = "{\"primaryKeyId\":1234567890,\"key\":[{\"keyData\":{\"typeUrl\":\"type.googleapis.com/ssb.crypto.tink.FpeFfxKey\",\"value\":\"EiBoBeUFkoew7YJObcgcz1uOmzdhJFkPP7driAxAuS0UiRpCEAIaPkFCQ0RFRkdISUpLTE1OT1BRUlNUVVZXWFlaYWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5\",\"keyMaterialType\":\"SYMMETRIC\"},\"status\":\"ENABLED\",\"keyId\":1234567890,\"outputPrefixType\":\"RAW\"}]}";

    private final static Map<String, String> KEYSETS = Map.of(
            "1234567890", KEYSET_JSON_FF31_256_ALPHANUMERIC
    );

    private Fpe fpePrimitive(String keyId) throws Exception {
        if (! KEYSETS.containsKey(keyId)) {
            throw new RuntimeException("Unknown keyId: " + keyId);
        }

        KeysetHandle keysetHandle = CleartextKeysetHandle.read(JsonKeysetReader.withString(KEYSETS.get(keyId)));
        return keysetHandle.getPrimitive(Fpe.class);
    }

    private PseudoFunc f(String funcDecl) throws Exception {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        String keyId = config.getRequired(TinkFpeFuncConfig.Param.KEY_ID, String.class);
        config.add(TinkFpeFuncConfig.Param.FPE, fpePrimitive(keyId));
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
    void givenText_ff31_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890)";
        transformAndRestore("Something", "oADYiZKI3", f(funcDeclStr));
    }
    @Test
    void givenText_ff31Fail_shouldFailForNonSupportedCharacters() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890)"; // defaults to "strategy=FAIL"
        assertThatThrownBy(() -> {
            f(funcDeclStr).apply(PseudoFuncInput.of("Ken sent me..."));
        })
                .isInstanceOf(IncompatiblePlaintextException.class)
                .hasMessageContaining("Plaintext can only contain characters from the alphabet");
    }

    @Test
    void givenText_ff31Skip_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890, strategy=SKiP)";
        transformAndRestore("Ken sent me...", "fCR kd95 VR...", f(funcDeclStr));
    }

    @Test
    void givenText_ff31Delete_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890, strategy=delete)";
        PseudoFunc func = f(funcDeclStr);

        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of("Ken sent me..."));
        assertThat(pseudonymized.getFirstValue()).isEqualTo("fCRkd95VR");

        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of("fCRkd95VR"));
        assertThat(depseudonymized.getFirstValue()).isEqualTo("Kensentme");
    }

    @Test
    void givenText_ff31Redact_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890, strategy=redact, redactChar=Z)";
        PseudoFunc func = f(funcDeclStr);

        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of("Ken sent me..."));
        assertThat(pseudonymized.getFirstValue()).isEqualTo("KGoDjzQOx4MasT");

        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of("KGoDjzQOx4MasT"));
        assertThat(depseudonymized.getFirstValue()).isEqualTo("KenZsentZmeZZZ");
    }


}
