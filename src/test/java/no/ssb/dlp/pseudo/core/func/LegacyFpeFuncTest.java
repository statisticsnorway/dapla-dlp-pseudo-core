package no.ssb.dlp.pseudo.core.func;

import com.google.common.collect.ImmutableList;
import no.ssb.dapla.dlp.pseudo.func.*;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LegacyFpeFuncTest {

    private final static Map<String, String> KEYSETS = Map.of(
            "secret1", "C5sn7B4YtwcilAwuVx6NuAsMWLusOSA/ldia40ZugDI="
    );

    private PseudoFunc f(String funcDecl) throws Exception {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        String keyId = config.getRequired(FpeFuncConfig.Param.KEY_ID, String.class);
        config.add(FpeFuncConfig.Param.KEY_DATA, KEYSETS.get(keyId));
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
    void givenText_fpeAnychar_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "fpe-anychar(keyId=secret1)";
        transformAndRestore("Something", "-Æ'GÕT@«L", f(funcDeclStr));
    }

    @Test
    void givenText_fpeCustomAlphabet_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "fpe-abcdefghij(keyId=secret1)";
        transformAndRestore("abcdef", "djcjbf", f(funcDeclStr));
    }

    @Test
    void givenNonAlphabetText_fpeCustomAlphabet_shouldFail() throws Exception {
        String funcDeclStr = "fpe-abcdefghij(keyId=secret1, replaceIllegalChars=false, replaceIllegalCharsWith=X)";
        assertThatThrownBy(() -> {
            f(funcDeclStr).apply(PseudoFuncInput.of("abcHELLO"));
        })
                .isInstanceOf(FpeFunc.FpePseudoFuncException.class)
                .hasMessageContaining("FPE pseudo apply error");
    }

    @Test
    void givenDigits_fpeDigits_shouldEncryptAndDecrypt() throws Exception {
        String funcDeclStr = "fpe-digits(keyId=secret1)";
        transformAndRestore("1234567890", "7830880047", f(funcDeclStr));
    }

}
