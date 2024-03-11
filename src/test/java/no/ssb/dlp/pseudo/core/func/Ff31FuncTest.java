package no.ssb.dlp.pseudo.core.func;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import no.ssb.crypto.tink.fpe.Fpe;
import no.ssb.crypto.tink.fpe.FpeConfig;
import no.ssb.crypto.tink.fpe.IncompatiblePlaintextException;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class Ff31FuncTest {

    @BeforeAll
    static void init() throws Exception {
        FpeConfig.register();
    }

    private final static String KEYSET_JSON_FF31_256_ALPHANUMERIC = "{\"primaryKeyId\":832997605,\"key\":[{\"keyData\":{\"typeUrl\":\"type.googleapis.com/ssb.crypto.tink.FpeFfxKey\",\"value\":\"EiCCNkK81HHmUY4IjEzXDrGLOT5t+7PGQ1eIyrGqGa4S3BpCEAIaPjAxMjM0NTY3ODlBQkNERUZHSElKS0xNTk9QUVJTVFVWV1hZWmFiY2RlZmdoaWprbG1ub3BxcnN0dXZ3eHl6\",\"keyMaterialType\":\"SYMMETRIC\"},\"status\":\"ENABLED\",\"keyId\":832997605,\"outputPrefixType\":\"RAW\"}]}";

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

    private void transformAndRestore(String originalVal, String expectedVal, PseudoFunc func) {
        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of(originalVal));
        assertThat(pseudonymized.getValue()).isEqualTo(expectedVal);
        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of(pseudonymized.getValue()));
        assertThat(depseudonymized.getValue()).isEqualTo(originalVal);
    }

    @Test
    void givenText_ff31_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890)";
        transformAndRestore("Something", "gHFaQBh7g", f(funcDeclStr));
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
        transformAndRestore("Ken sent me...", "6Dy NHKv ig...", f(funcDeclStr));
    }

    @Test
    void givenText_ff31Delete_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890, strategy=delete)";
        PseudoFunc func = f(funcDeclStr);

        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of("Ken sent me..."));
        assertThat(pseudonymized.getValue()).isEqualTo("6DyNHKvig");

        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of("6DyNHKvig"));
        assertThat(depseudonymized.getValue()).isEqualTo("Kensentme");
    }

    @Test
    void givenText_ff31Redact_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "ff31(keyId=1234567890, strategy=redact, redactChar=Z)";
        PseudoFunc func = f(funcDeclStr);

        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of("Ken sent me..."));
        assertThat(pseudonymized.getValue()).isEqualTo("3WD8UlZRDER1z5");

        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of("3WD8UlZRDER1z5"));
        assertThat(depseudonymized.getValue()).isEqualTo("KenZsentZmeZZZ");
    }


}
