package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RedactFuncTest {

    private PseudoFunc f(String funcDecl) {
        PseudoFuncConfig config = PseudoFuncConfigFactory.get(funcDecl);
        return PseudoFuncFactory.create(config);
    }

    static void assertEqual(PseudoFuncOutput out, Object expected) {
        assertThat(out.getFirstValue()).isEqualTo(expected);
    }

    @Test
    void givenText_redactWithDefaults_shouldReplaceWithPlaceholder() {
        PseudoFuncOutput out = f("redact()").apply(PseudoFuncInput.of("Something"));
        assertEqual(out, "*");
    }

    @Test
    void givenText_redactWithCustomPlaceholder_shouldReplaceWithPlaceholder() {
        PseudoFuncOutput out = f("redact(placeholder=###)").apply(PseudoFuncInput.of("Something"));
        assertEqual(out, "###");
    }

    @Test
    void givenEmpty_redact_shouldReturnEmpty() {
        PseudoFuncOutput out = f("redact()").apply(PseudoFuncInput.of(""));
        assertEqual(out, "");
    }

    // TODO: Support regexes with comma, like these: redact(regex='^.{0,4})'
    @Test
    void givenText_redactWithRegex_shouldReplaceTextPartially() {
        PseudoFuncOutput out = f("redact(regex=^Some").apply(PseudoFuncInput.of("Something"));
        assertEqual(out, "*thing");
    }

    @Test
    void givenText_redactRestore_shouldEchoInput() {
        PseudoFuncOutput out = f("redact()").restore(PseudoFuncInput.of("Something"));
        assertEqual(out, "Something");
    }

}
