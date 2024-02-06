package no.ssb.dlp.pseudo.core.func;

import no.ssb.dlp.pseudo.core.PseudoException;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

// TODO: Support values wrapped by quotes (for values that need to use special characters like commas or parentheses)

public class PseudoFuncDeclaration {
    public static final String KEY_REFERENCE  = "keyId";
    private final String funcName;
    private final Map<String, String> args;

    public PseudoFuncDeclaration(String funcName, Map<String, String> args) {
        this.funcName = funcName;
        this.args = args;
    }

    public static PseudoFuncDeclaration fromString(final String funcDeclStr) {
        String s = funcDeclStr.replaceAll("[()]", " ").trim();
        if (s.indexOf(' ') == -1) {
            return new PseudoFuncDeclaration(s, Map.of());
        } else {
            String funcName = s.substring(0, s.indexOf(' '));
            String argsString = s.substring(funcName.length(), s.length());

            Map<String, String> args = Arrays.stream(argsString.split(","))
                    .map(kv -> {
                        String[] items = kv.split("=", 2);
                        if (items.length == 2) {
                            return items;
                        }
                        else {
                            // For backwards compatibility with fpe:
                            // If param name is not specified (which used to be okay earlier, then we maintain backwards
                            // compatibility by assuming the param name to be 'keyId'.
                            // This should be removed if/when we no longer support the fpe- function family.
                            if (funcName.startsWith(PseudoFuncNames.FPE)) {
                                return new String[] {KEY_REFERENCE, kv.trim()}; // For backwards compatibility. Should be removed if we phase out fpe- functions
                            }
                            throw new InvalidPseudoFuncParam("Pseudo func param should be on the format 'key=value', but was '" + kv.trim() + "'. Func declaration:" + funcDeclStr);
                        }
                    })
                    .collect(Collectors.toMap(
                            kv -> kv[0].trim(),
                            kv -> kv[1].trim(),
                            (a, b) -> b, LinkedHashMap::new));

            return new PseudoFuncDeclaration(funcName, args);
        }
    }

    @Override
    public String toString() {
        return funcName + "(" + args.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", ")) + ")";
    }

    public String getFuncName() {
        return funcName;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    static class InvalidPseudoFuncParam extends PseudoException {
        public InvalidPseudoFuncParam(String message) {
            super(message);
        }
    }

}

