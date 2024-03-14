package no.ssb.dlp.pseudo.core.func;

import com.google.crypto.tink.CleartextKeysetHandle;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.JsonKeysetReader;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.daead.DeterministicAeadConfig;
import no.ssb.crypto.tink.fpe.Fpe;
import no.ssb.crypto.tink.fpe.FpeConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.map.MapFunc;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.security.GeneralSecurityException;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MapAndDaeadFuncTest {

    @BeforeAll
    static void init() {
        try {
            DeterministicAeadConfig.register();
        } catch (GeneralSecurityException e) {
            //Ignore since it may happen in concurrent junit tests
        }
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

    private void transformAndRestore(String originalVal, String expectedVal, PseudoFunc func) {
        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of(originalVal));
        assertThat(pseudonymized.getValue()).isEqualTo(expectedVal);
        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of(pseudonymized.getValue()));
        assertThat(depseudonymized.getValue()).isEqualTo(originalVal);
    }

    @Test
    void givenText_map_and_daead_shouldEncryptAndDecrypt() throws Exception {
        final Mapper mockMapper = mock(Mapper.class);
        try (var mapFunc = mockStatic(MapFunc.class)) {
            mapFunc.when(() -> MapFunc.loadMapper()).thenReturn(mockMapper);
            when(mockMapper.map(eq(PseudoFuncInput.of("Something")))).thenReturn(PseudoFuncOutput.of("Secret"));
            when(mockMapper.restore(eq(PseudoFuncInput.of("Secret")))).thenReturn(PseudoFuncOutput.of("Something"));
            String funcDeclStr = "map-sid-daead(keyId=1284924461)";
            transformAndRestore("Something", "AUyWZC2kWmY72/261fvqshAWQXfy+FY+F7PB", f(funcDeclStr));
        }
    }

}
