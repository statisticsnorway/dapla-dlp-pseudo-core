package no.ssb.dlp.pseudo.core.field;

import java.io.Serializable;
import java.util.*;

/**
 * A chain of {@link ValueInterceptor}s that should be invoked serially after conversion.
 *
 * This can be used together with avro-buddy converters in order to perform multiple additional tasks such as
 * pseudonymization, validation, reporting, logging, etc
 *
 * Example: 1) Perform pseudonymization and 2) log schema metrics
 */
public class ValueInterceptorChain implements ValueInterceptor {
    private final List<ValueInterceptor> initChain = new ArrayList<>();
    private final List<ValueInterceptor> chain = new ArrayList<>();
    private final Map<String, Serializable> context = new HashMap<>();

    public ValueInterceptorChain preprocessor(ValueInterceptor valueInterceptor) {
        initChain.add(valueInterceptor);
        return this;
    }

    public ValueInterceptorChain register(ValueInterceptor valueInterceptor) {
        chain.add(valueInterceptor);
        return this;
    }

    public ValueInterceptorChain register(ValueInterceptor valueInterceptor, ValueInterceptor... valueInterceptors) {
        chain.add(valueInterceptor);
        chain.addAll(Arrays.asList(valueInterceptors));
        return this;
    }

    public String init(FieldDescriptor field, String value) {
        for (ValueInterceptor vi : initChain) {
            value = vi.apply(field, value);
        }
        return value;
    }

    public boolean hasPreprocessors() {
        return !initChain.isEmpty();
    }

    @Override
    public String apply(FieldDescriptor field, String value) {
        for (ValueInterceptor vi : chain) {
            value = vi.apply(field, value);
        }
        return value;
    }

    public void putContextData(String key, Serializable value) {
        context.put(key, value);
    }

    public <T extends Serializable> Optional<T> getContextData(String key, Class<T> type) {
        return Optional.ofNullable((T) context.get(key));
    }

}
