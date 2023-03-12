package no.ssb.dlp.pseudo.core.func;

import com.google.common.collect.ImmutableMap;
import no.ssb.dapla.dlp.pseudo.func.PseudoFunc;
import no.ssb.dapla.dlp.pseudo.func.PseudoFuncConfig;
import no.ssb.dlp.pseudo.core.PseudoException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PseudoFuncConfigPreset holds a set of common parameters + defines a set of parameters that can be
 * supplied externally in order to create a valid PseudoFuncConfig.
 */
public class PseudoFuncConfigPreset {

    private final String funcName;

    private final Map<String, Object> staticParams;

    private final Set<PseudoFuncParam> dynamicParams;

    private PseudoFuncConfigPreset(String funcName, Map<String, Object> staticParams, Set<PseudoFuncParam> dynamicParams) {
        this.funcName = funcName;
        this.staticParams = (staticParams != null) ? staticParams : Collections.emptyMap();
        this.dynamicParams = (dynamicParams != null) ? dynamicParams : new HashSet<>();
    }

    public String getFuncName() {
        return funcName;
    }

    /**
     * Params common for all functions
     */
    public Map<String, Object> getStaticParams() {
        return staticParams;
    }

    /**
     * Names of params that must be supplied externally.
     */
    public Set<PseudoFuncParam> getDynamicParams() {
        return dynamicParams;
    }

    /**
     * Construct a PseudoFuncConfig using the supplied user defined arguments
     */
    public PseudoFuncConfig toPseudoFuncConfig(PseudoFuncDeclaration funcDecl) {
        Map<String, String> args = Optional.ofNullable(funcDecl.getArgs()).orElse(Map.of());

        ImmutableMap.Builder params = ImmutableMap.builder();
        params.put(PseudoFuncConfig.Param.FUNC_DECL, funcDecl.toString());
        params.putAll(staticParams);

        for (PseudoFuncParam param : dynamicParams) {
            if (param.isRequired() && !args.containsKey(param.getParamName())) {
                throw new MissingPseudoFuncParamException("Missing dynamic required pseudo func param '" + param.getParamName() + "' in function declaration " + funcDecl.toString());
            }
            else {
                String strValue = args.get(param.getParamName());
                if (strValue == null) {
                    if (param.getDefaultValue() != null) {
                        params.put(param.getParamName(), param.getDefaultValue());
                    }
                }
                else {
                    Object value = param.parseValue(strValue);
                    if (value != null) {
                        params.put(param.getParamName(), param.parseValue(strValue));
                    }
                }
            }
        }

        Map<String, Object> paramsMap = params.build();
        Set<String> undefined = args.keySet().stream()
                .filter(s -> ! paramsMap.keySet().contains(s))
                .collect(Collectors.toSet());

        if (! undefined.isEmpty()) {
            throw new UndefinedPseudoFuncParamException("Encountered param(s) not defined in the PseudoFuncConfigPreset: " + undefined);
        }

        return new PseudoFuncConfig(params.build());
    }

    public static <T extends PseudoFunc> Builder builder(String funcName, Class<T> impl) {
        return new Builder(funcName, impl);
    }

    public static class Builder {
        private final String funcName;

        private final Map<String, Object> staticParams = new HashMap<>();

        private final Set<PseudoFuncParam> dynamicParams = new HashSet<>();

        public <T extends PseudoFunc> Builder(String funcName, Class<T> impl) {
            this.funcName = funcName;
            staticParams.put(PseudoFuncConfig.Param.FUNC_IMPL, impl.getName());
        }

        public Builder staticParam(String name, Object value) {
            staticParams.put(name, value);
            return this;
        }

        public <T> Builder requiredParam(Class<T> type, String name) {
            dynamicParams.add(PseudoFuncParam.required(type, name));
            return this;
        }

        public <T> Builder optionalParam(Class<T> type, String name) {
            dynamicParams.add(PseudoFuncParam.optional(type, name));
            return this;
        }

        public <T> Builder optionalParam(Class<T> type, String name, T defaultValue) {
            dynamicParams.add(PseudoFuncParam.optional(type, name, defaultValue));
            return this;
        }

        public PseudoFuncConfigPreset build() {
            return new PseudoFuncConfigPreset(funcName, staticParams, dynamicParams);
        }
    }

    static class MissingPseudoFuncParamException extends PseudoException {
        public MissingPseudoFuncParamException(String message) {
            super(message);
        }
    }

    static class UndefinedPseudoFuncParamException extends PseudoException {
        public UndefinedPseudoFuncParamException(String message) {
            super(message);
        }
    }
}
