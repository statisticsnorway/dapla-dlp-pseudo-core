package no.ssb.dlp.pseudo.core.func;

import no.ssb.dapla.dlp.pseudo.func.AbstractPseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncInput;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PseudoFuncConfigPresetTest {

    private enum SomeEnum {
        DEFAULT_VAL, VAL1, VAL2;
    }

    private static class DummyPseudoFunc extends AbstractPseudoFunc {
        public DummyPseudoFunc(PseudoFuncConfig genericConfig) {
            super(genericConfig.getFuncDecl());
        }

        @Override
        public String getAlgorithm() {
            return null;
        }

        @Override
        public PseudoFuncOutput apply(PseudoFuncInput input) {
            return new PseudoFuncOutput();
        }

        @Override
        public PseudoFuncOutput restore(PseudoFuncInput input) {
            return new PseudoFuncOutput();
        }
    }

    PseudoFuncConfigPreset preset() {
        return PseudoFuncConfigPreset.builder("foo", DummyPseudoFunc.class)
                .staticParam("staticStr", "yo")
                .staticParam("staticLong", 321L)
                .staticParam("staticBoolean", Boolean.TRUE)
                .requiredParam(String.class, "strParam")
                .optionalParam(Integer.class, "optionalIntParam")
                .optionalParam(SomeEnum.class, "optionalWithDefaultEnumParam", SomeEnum.DEFAULT_VAL)
                .build();
    }

    @Test
    void toPseudoFuncConfigFromPseudoFuncDeclaration_withAllOptionalParamsSet() {
        String funcDeclString = "foo(strParam=hey ho, optionalWithDefaultEnumParam=val1, optionalIntParam=123)";
        PseudoFuncDeclaration funcDecl = PseudoFuncDeclaration.fromString(funcDeclString);
        PseudoFuncConfig config = preset().toPseudoFuncConfig(funcDecl);
        Map<String, Object> map = config.asMap();

        assertThat(map).hasSize(8);
        assertThat(map.get("decl")).isEqualTo(funcDecl.toString());
        assertThat(map.get("impl")).isEqualTo(DummyPseudoFunc.class.getName());
        assertThat(map.get("staticStr")).isEqualTo("yo");
        assertThat(map.get("staticLong")).isEqualTo(321L);
        assertThat(map.get("staticBoolean")).isEqualTo(true);
        assertThat(map.get("strParam")).isEqualTo("hey ho");
        assertThat(map.get("optionalIntParam")).isEqualTo(123);
        assertThat(map.get("optionalWithDefaultEnumParam")).isEqualTo(SomeEnum.VAL1);
    }

    @Test
    void toPseudoFuncConfigFromPseudoFuncDeclaration_withNoOptionalParamsSet() {
        String funcDeclString = "foo(strParam=hey ho)";
        PseudoFuncDeclaration funcDecl = PseudoFuncDeclaration.fromString(funcDeclString);
        PseudoFuncConfig config = preset().toPseudoFuncConfig(funcDecl);
        Map<String, Object> map = config.asMap();

        assertThat(map).hasSize(7);
        assertThat(map.get("decl")).isEqualTo(funcDecl.toString());
        assertThat(map.get("impl")).isEqualTo(DummyPseudoFunc.class.getName());
        assertThat(map.get("staticStr")).isEqualTo("yo");
        assertThat(map.get("staticLong")).isEqualTo(321L);
        assertThat(map.get("staticBoolean")).isEqualTo(true);
        assertThat(map.get("strParam")).isEqualTo("hey ho");
        assertThat(map.get("optionalWithDefaultEnumParam")).isEqualTo(SomeEnum.DEFAULT_VAL);
    }

    @Test
    void toPseudoFuncConfigFromPseudoFuncDeclaration_withoutRequiredParamsSet_shouldThrowException() {
        String funcDeclString = "foo()";
        PseudoFuncDeclaration funcDecl = PseudoFuncDeclaration.fromString(funcDeclString);

        assertThatThrownBy(() -> {
            preset().toPseudoFuncConfig(funcDecl);
        })
        .isInstanceOf(PseudoFuncConfigPreset.MissingPseudoFuncParamException.class)
        .hasMessageContaining("Missing dynamic required pseudo func param 'strParam' in function declaration foo()");
    }

    @Test
    void toPseudoFuncConfigFromPseudoFuncDeclaration_withUndefinedParams_shouldThrowException() {
        String funcDeclString = "foo(strParam=hey ho, undefinedParam=blah)";
        PseudoFuncDeclaration funcDecl = PseudoFuncDeclaration.fromString(funcDeclString);

        assertThatThrownBy(() -> {
            preset().toPseudoFuncConfig(funcDecl);
        })
                .isInstanceOf(PseudoFuncConfigPreset.UndefinedPseudoFuncParamException.class)
                .hasMessageContaining("Encountered param(s) not defined in the PseudoFuncConfigPreset: [undefinedParam]");
    }

}