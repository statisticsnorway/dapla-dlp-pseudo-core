package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncFactory;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

public class LegacyFpeFuncTest {

    private final static Map<String, String> KEYSETS = Map.of(
            "secret1", "C5sn7B4YtwcilAwuVx6NuAsMWLusOSA/ldia40ZugDI="
    );

    private PseudoFunc f(String funcDecl) {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        String keyId = config.getRequired(FpeFuncConfig.Param.KEY_ID, String.class);
        config.add(FpeFuncConfig.Param.KEY_DATA, KEYSETS.get(keyId));
        return PseudoFuncFactory.create(config);
    }

    private void transformAndRestore(String originalVal, String expectedVal, PseudoFunc func) {
        PseudoFuncOutput pseudonymized = func.apply(PseudoFuncInput.of(originalVal));
        assertThat(pseudonymized.getValue()).isEqualTo(expectedVal);
        PseudoFuncOutput depseudonymized = func.restore(PseudoFuncInput.of(pseudonymized.getValue()));
        assertThat(depseudonymized.getValue()).isEqualTo(originalVal);
    }

    @Test
    void givenText_fpeAnychar_shouldEncryptAndDecrypt() {
        String funcDeclStr = "fpe-anychar(keyId=secret1)";
        transformAndRestore("Something", "-Æ'GÕT@«L", f(funcDeclStr));
    }

    @Test
    void givenText_fpeCustomAlphabet_shouldEncryptAndDecrypt() {
        String funcDeclStr = "fpe-abcdefghij(keyId=secret1)";
        transformAndRestore("abcdef", "djcjbf", f(funcDeclStr));
    }

    @Test
    void givenNonAlphabetText_fpeCustomAlphabet_shouldFail() {
        String funcDeclStr = "fpe-abcdefghij(keyId=secret1, replaceIllegalChars=false, replaceIllegalCharsWith=X)";
        assertThatThrownBy(() -> {
            f(funcDeclStr).apply(PseudoFuncInput.of("abcHELLO"));
        })
                .isInstanceOf(FpeFunc.FpePseudoFuncException.class)
                .hasMessageContaining("FPE pseudo apply error");
    }

    @Test
    void givenDigits_fpeDigits_shouldEncryptAndDecrypt() {
        String funcDeclStr = "fpe-digits(keyId=secret1)";
        transformAndRestore("1234567890", "7830880047", f(funcDeclStr));
    }

}
