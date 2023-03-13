package no.ssb.dlp.pseudo.core.func;


import lombok.NonNull;
import lombok.Value;
import no.ssb.dapla.dlp.pseudo.func.util.FromString;

@Value
public class PseudoFuncParam<T> {

    @NonNull
    private final String paramName;
    @NonNull
    private final Class<T> type;
    private final boolean required;

    private final T defaultValue;

    public boolean isOptional() {
        return ! required;
    }

    public T parseValue(String stringValue) {
        return FromString.convert(stringValue, type);
    }

    public static <T> PseudoFuncParam required(Class<T> type, String name) {
        return new PseudoFuncParam(name, type, true, null);
    }

    public static <T> PseudoFuncParam optional(Class<T> type, String name, T defaultValue) {
        return new PseudoFuncParam(name, type, false, defaultValue);
    }

    public static <T> PseudoFuncParam optional(Class<T> type, String name) {
        return new PseudoFuncParam(name, type, false, null);
    }

}
