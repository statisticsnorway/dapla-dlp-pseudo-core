package no.ssb.dlp.pseudo.core.func;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import no.ssb.crypto.tink.fpe.Fpe;
import no.ssb.crypto.tink.fpe.FpeConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.map.MapFunc;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MapAndFf31FuncTest {

    @BeforeAll
    static void init() {
        try {
            FpeConfig.register();
        } catch (GeneralSecurityException e) {
            //Ignore since it may happen in concurrent junit tests
        }
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
    void givenText_map_and_ff31_shouldEncryptAndDecrypt() throws Exception {
        final Mapper mockMapper = mock(Mapper.class);
        try (var mapFunc = mockStatic(MapFunc.class)) {
            mapFunc.when(() -> MapFunc.loadMapper()).thenReturn(mockMapper);
            when(mockMapper.map(eq(PseudoFuncInput.of("Something")))).thenReturn(PseudoFuncOutput.of("Secret"));
            when(mockMapper.restore(eq(PseudoFuncInput.of("Secret")))).thenReturn(PseudoFuncOutput.of("Something"));
            String funcDeclStr = "map-sid-ff31(keyId=1234567890)";
            transformAndRestore("Something", "CQqlS3", f(funcDeclStr));
        }
    }

}
