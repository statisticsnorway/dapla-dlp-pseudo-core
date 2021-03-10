package no.ssb.dlp.pseudo.core.typeconverter;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.PseudoSecret;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

public class PseudoSecretTypeConverter implements TypeConverter<Map, PseudoSecret> {

    @Override
    public Optional<PseudoSecret> convert(Map propertyMap, Class<PseudoSecret> targetType, ConversionContext context) {
        PropertyAccessor props = new PropertyAccessor(propertyMap);
        PseudoSecret.PseudoSecretBuilder builder = PseudoSecret.builder()
          .name(props.optionalString("name"))
          .id(props.optionalString("id"))
          .version(props.optionalString("version"))
          .type(props.optionalString("type"));

        if (propertyMap.containsKey("content")) {
            builder.base64EncodedContent(props.optionalString("content"));
        }
        else if (propertyMap.containsKey("rawcontent")) {
            builder.content(props.optionalString("rawcontent").getBytes(StandardCharsets.UTF_8));
        }

        PseudoSecret pseudoSecret = builder.build();

        return (pseudoSecret.getId() == null && pseudoSecret.getContent() == null)
          ? Optional.empty()
          : Optional.of(pseudoSecret);
    }

    @RequiredArgsConstructor
    private static class PropertyAccessor {
        private final Map propertyMap;

        String optionalString(String key) {
            return ConversionService.SHARED.convert(propertyMap.get(key), String.class).orElse(null);
        }
    }

}
