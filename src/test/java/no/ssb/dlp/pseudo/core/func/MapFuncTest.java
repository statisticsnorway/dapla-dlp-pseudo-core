package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.map.Mapper;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import org.junit.jupiter.api.Test;
import org.mockito.internal.matchers.Any;

import java.util.Collections;
import java.util.ServiceLoader;

import static org.mockito.Mockito.*;

public class MapFuncTest {
    private PseudoFunc f(String funcDecl) throws Exception {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        return PseudoFuncFactory.create(config);
    }

    @Test
    void mapFuncWithTimestamp() throws Exception {
        String funcDeclStr = "map-sid(keyId=1284924461, versionTimestamp=test)";
        PseudoFunc func = f(funcDeclStr);
        func.init(PseudoFuncInput.of("50607080901"));
    }
}
