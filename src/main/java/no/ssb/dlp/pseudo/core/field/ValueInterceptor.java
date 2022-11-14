package no.ssb.dlp.pseudo.core.field;

/**
 * An interceptor that allows for overriding a value based on the value itself, and the field name to which it was applied.
 */
@FunctionalInterface
public interface ValueInterceptor {

    /**
     * Applied when setting a value to a corresponding field. E.g cake='chocolate' where fieldName=cake and value=chocolate.
     * @param field The field descriptor (path, name, ...) of the field to assign a value.
     * @param value The intercepted value.
     * @return The actual value that will be applied to the field.
     */
    String apply(FieldDescriptor field, String value); //TODO: value should be Object?

}
