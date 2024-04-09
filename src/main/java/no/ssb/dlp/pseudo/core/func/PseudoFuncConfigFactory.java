package no.ssb.dlp.pseudo.core.func;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import no.ssb.crypto.tink.fpe.UnknownCharacterStrategy;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.composite.MapAndEncryptFunc;
import no.ssb.dapla.dlp.pseudo.func.composite.MapAndEncryptFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFunc;
import no.ssb.dapla.dlp.pseudo.func.fpe.FpeFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.map.MapFailureStrategy;
import no.ssb.dapla.dlp.pseudo.func.map.MapFunc;
import no.ssb.dapla.dlp.pseudo.func.map.MapFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.redact.RedactFunc;
import no.ssb.dapla.dlp.pseudo.func.redact.RedactFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFunc;
import no.ssb.dapla.dlp.pseudo.func.tink.daead.TinkDaeadFuncConfig;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFunc;
import no.ssb.dapla.dlp.pseudo.func.tink.fpe.TinkFpeFuncConfig;
import no.ssb.dlp.pseudo.core.PseudoException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets.alphabetNameOf;
import static no.ssb.dapla.dlp.pseudo.func.text.CharacterGroup.*;
import static no.ssb.dlp.pseudo.core.func.PseudoFuncNames.*;

@Slf4j
class PseudoFuncConfigFactory {

    private static final Map<String, PseudoFuncConfigPreset> PSEUDO_CONFIG_PRESETS_MAP = new HashMap<>();

    static {
        PSEUDO_CONFIG_PRESETS_MAP.putAll(Maps.uniqueIndex(List.of(
          tinkDaeadPseudoFuncConfigPreset(DAEAD),
          tinkFpePseudoFuncConfigPreset(FF31),
          sidMappingPseudoFuncConfigPreset(MAP_SID),
          sidMappingAndTinkFpePseudoFuncConfigPreset(MAP_SID_FF31),
          sidMappingAndTinkDaeadPseudoFuncConfigPreset(MAP_SID_DAEAD),
          redactPseudoFuncConfigPreset(REDACT),
          fpePseudoFuncConfigPreset(FPE + "-text", alphabetNameOf(ALPHANUMERIC, WHITESPACE, SYMBOLS)),
          fpePseudoFuncConfigPreset(FPE + "-text_no", alphabetNameOf(ALPHANUMERIC_NO, WHITESPACE, SYMBOLS)),
          fpePseudoFuncConfigPreset(FPE + "-fnr", alphabetNameOf(DIGITS))

        ), PseudoFuncConfigPreset::getFuncName));
    }

    private PseudoFuncConfigFactory() {}

    private static PseudoFuncConfigPreset sidMappingPseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, MapFunc.class)
                .staticParam(MapFuncConfig.Param.CONTEXT, "sid")
                .requiredParam(String.class, TinkFpeFuncConfig.Param.KEY_ID)
                .optionalParam(String.class, MapFuncConfig.Param.SNAPSHOT_DATE)
                .optionalParam(MapFailureStrategy.class, MapFuncConfig.Param.MAP_FAILURE_STRATEGY, MapFailureStrategy.RETURN_ORIGINAL)
                .build();
    }

    private static PseudoFuncConfigPreset sidMappingAndTinkFpePseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, MapAndEncryptFunc.class)
                .staticParam(MapAndEncryptFuncConfig.Param.MAP_FUNC_IMPL, MapFunc.class.getName())
                .staticParam(MapAndEncryptFuncConfig.Param.ENCRYPTION_FUNC_IMPL, TinkFpeFunc.class.getName())
                .requiredParam(String.class, TinkFpeFuncConfig.Param.KEY_ID)
                .optionalParam(String.class, MapFuncConfig.Param.SNAPSHOT_DATE)
                .optionalParam(UnknownCharacterStrategy.class, TinkFpeFuncConfig.Param.UNKNOWN_CHARACTER_STRATEGY, UnknownCharacterStrategy.FAIL)
                .optionalParam(String.class, TinkFpeFuncConfig.Param.TWEAK)
                .optionalParam(Character.class, TinkFpeFuncConfig.Param.REDACT_CHAR)
                .optionalParam(MapFailureStrategy.class, MapFuncConfig.Param.MAP_FAILURE_STRATEGY, MapFailureStrategy.RETURN_ORIGINAL)
                .build();
    }

    private static PseudoFuncConfigPreset sidMappingAndTinkDaeadPseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, MapAndEncryptFunc.class)
                .staticParam(MapAndEncryptFuncConfig.Param.MAP_FUNC_IMPL, MapFunc.class.getName())
                .staticParam(MapAndEncryptFuncConfig.Param.ENCRYPTION_FUNC_IMPL, TinkDaeadFunc.class.getName())
                .optionalParam(String.class, MapFuncConfig.Param.SNAPSHOT_DATE)
                .requiredParam(String.class, TinkDaeadFuncConfig.Param.KEY_ID)
                .optionalParam(MapFailureStrategy.class, MapFuncConfig.Param.MAP_FAILURE_STRATEGY, MapFailureStrategy.RETURN_ORIGINAL)
                .build();
    }

    private static PseudoFuncConfigPreset redactPseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, RedactFunc.class)
                .optionalParam(String.class, RedactFuncConfig.Param.PLACEHOLDER, "*")
                .optionalParam(String.class, RedactFuncConfig.Param.REGEX)
                .build();
    }

    private static PseudoFuncConfigPreset tinkDaeadPseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, TinkDaeadFunc.class)
                .requiredParam(String.class, TinkDaeadFuncConfig.Param.KEY_ID)
                .build();
    }

    private static PseudoFuncConfigPreset tinkFpePseudoFuncConfigPreset(String funcName) {
        return PseudoFuncConfigPreset.builder(funcName, TinkFpeFunc.class)
                .requiredParam(String.class, TinkFpeFuncConfig.Param.KEY_ID)
                .optionalParam(UnknownCharacterStrategy.class, TinkFpeFuncConfig.Param.UNKNOWN_CHARACTER_STRATEGY, UnknownCharacterStrategy.FAIL)
                .optionalParam(String.class, TinkFpeFuncConfig.Param.TWEAK)
                .optionalParam(Character.class, TinkFpeFuncConfig.Param.REDACT_CHAR)
                .build();
    }

    private static PseudoFuncConfigPreset fpePseudoFuncConfigPreset(String funcName, String alphabet) {
        if (!funcName.startsWith(FPE)) {
            throw new IllegalArgumentException("Legacy FPE functions must be prefixed with '" + FPE + "'");
        }

        return PseudoFuncConfigPreset.builder(funcName, FpeFunc.class)
                .staticParam(FpeFuncConfig.Param.ALPHABET, alphabet)
                .requiredParam(String.class, FpeFuncConfig.Param.KEY_ID)
                .optionalParam(Boolean.class, FpeFuncConfig.Param.REPLACE_ILLEGAL_CHARS, true)
                .optionalParam(String.class, FpeFuncConfig.Param.REPLACE_ILLEGAL_CHARS_WITH)
                .build();
    }

    static PseudoFuncConfigPreset getConfigPreset(PseudoFuncDeclaration funcDecl) {
        String funcName = funcDecl.getFuncName();
        PseudoFuncConfigPreset preset = PSEUDO_CONFIG_PRESETS_MAP.get(funcDecl.getFuncName());
        if (preset != null) {
            return preset;
        }

        /*
        If no preset was defined AND this is an FPE function, then create the preset dynamically
        The alphabet to be used will be deduced from the function name (+ separated string with references to
        any already defined CharacterGroup, see no.ssb.dapla.dlp.pseudo.func.fpe.Alphabets)
         */
        if (funcName.startsWith(FPE + "-")) {
            String alphabetName = funcDecl.getFuncName().substring((FPE + "-").length());
            log.info("Add dynamic FPE function preset '{}' with alphabet '{}'. Allowed characters: {}",
              funcName, alphabetName, new String(Alphabets.fromAlphabetName(alphabetName).availableCharacters()));
            preset = fpePseudoFuncConfigPreset(funcName, alphabetName);
            PSEUDO_CONFIG_PRESETS_MAP.put(funcName, preset);
            return preset;
        }

        throw new InvalidPseudoFuncDeclarationException(funcName, "Check spelling. Only FPE functions can be created dynamically");
    }

    public static PseudoFuncConfig get(String funcDeclarationString) {
        PseudoFuncDeclaration funcDecl = PseudoFuncDeclaration.fromString(funcDeclarationString);
        return getConfigPreset(funcDecl).toPseudoFuncConfig(funcDecl);
    }

    static class InvalidPseudoFuncDeclarationException extends PseudoException {
        public InvalidPseudoFuncDeclarationException(String funcName, String message) {
            super("Invalid pseudo function declaration '" + funcName + "': " + message);
        }
    }

}
