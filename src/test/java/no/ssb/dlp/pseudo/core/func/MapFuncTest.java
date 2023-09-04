package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.map.MapFunc;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;

import static org.mockito.Mockito.*;

public class MapFuncTest {
    private PseudoFunc f(String funcDecl) {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        return PseudoFuncFactory.create(config);
    }

    @Test
    void mapFuncWithTimestamp() {
        final Mapper mockMapper = mock(Mapper.class);
        try (var mapFunc = mockStatic(MapFunc.class)) {
            mapFunc.when(() -> MapFunc.loadMapper()).thenReturn(mockMapper);
            String funcDeclStr = "map-sid(keyId=1284924461, versionTimestamp=test)";
            PseudoFunc func = f(funcDeclStr);
            func.init(PseudoFuncInput.of("50607080901"));
        }
        // Check that the mockMapper has received the versionTimestamp
        ArgumentCaptor<Map> argumentsCaptured = ArgumentCaptor.forClass(Map.class);
        verify(mockMapper).setConfig(argumentsCaptured.capture());
        assert argumentsCaptured.getValue().containsKey("versionTimestamp");
        // Check that the init method was called
        verify(mockMapper).init(eq("50607080901"));
    }
}