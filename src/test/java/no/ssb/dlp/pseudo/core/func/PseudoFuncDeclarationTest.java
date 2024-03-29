package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PseudoFuncDeclarationTest {

    @Test
    void parseFuncDeclStringWithArgs() {
        String funcDecl = "ffx31(keyId=123, strategy=skip)";
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString(funcDecl);
        assertThat(decl.getFuncName()).isEqualTo("ffx31");
        assertThat(decl.getArgs().size()).isEqualTo(2);
        assertThat(decl.getArgs().get("keyId")).isEqualTo("123");
        assertThat(decl.getArgs().get("strategy")).isEqualTo("skip");
    }

    @Test
    void parseFuncDeclStringWithArgsThatContainSpaces() {
        String funcDecl = "foo(an arg=1 2=3, yet another arg=hi ho)";
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString(funcDecl);
        assertThat(decl.getFuncName()).isEqualTo("foo");
        assertThat(decl.getArgs().size()).isEqualTo(2);
        assertThat(decl.getArgs().get("an arg")).isEqualTo("1 2=3");
        assertThat(decl.getArgs().get("yet another arg")).isEqualTo("hi ho");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\n", "()", "(   )"})
    void parseEmptyFuncDeclString() {
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString("");
        assertThat(decl.getFuncName()).isEqualTo("");
    }

    @ParameterizedTest
    @ValueSource(strings = {"ffx31()", "ffx31", "ffx31(    )"})
    void parseFuncDeclStringWithoutArgs(String funcDecl) {
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString(funcDecl);
        assertThat(decl.getFuncName()).isEqualTo("ffx31");
        assertThat(decl.getArgs()).isNotNull();
        assertThat(decl.getArgs().size()).isEqualTo(0);
    }

    @Test
    void parseFuncDeclStringWithImplicitKeyIdForFpeFunctions() {
        String funcDecl = "fpe-anychar(123)";
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString(funcDecl);
        assertThat(decl.getFuncName()).isEqualTo("fpe-anychar");
        assertThat(decl.getArgs()).isNotNull();
        assertThat(decl.getArgs().get(FpeFuncConfig.Param.KEY_ID)).isEqualTo("123");

        assertThatThrownBy(() -> {
            PseudoFuncDeclaration.fromString("foo-anychar(123)");
        })
                .isInstanceOf(PseudoFuncDeclaration.InvalidPseudoFuncParam.class)
                .hasMessageContaining("Pseudo func param should be on the format 'key=value', but was '123'. Func declaration:foo-anychar(123)");
    }

    @ParameterizedTest
    @CsvSource(delimiter = ';', value = {
            "foo;foo()",
            "foo();foo()",
            "foo(keyId=123);foo(keyId=123)",
            "foo(    keyId   =  123  );foo(keyId=123)",
            "foo(    some thing =   my value );foo(some thing=my value)",
            "foo(bar = baz=123);foo(bar=baz=123)",
    })
    void toStringOfparsedFuncDecl(String funcDecl, String expectedToString) {
        PseudoFuncDeclaration decl = PseudoFuncDeclaration.fromString(funcDecl);
        assertThat(decl.toString()).isEqualTo(expectedToString);
    }

}
