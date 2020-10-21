package no.ssb.dlp.pseudo.core.typeconverter;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;
import no.ssb.dlp.pseudo.core.PseudoSecret;

import java.util.Map;
import java.util.Optional;

public class PseudoSecretTypeConverter implements TypeConverter<Map, PseudoSecret> {

    @Override
    public Optional<PseudoSecret> convert(Map propertyMap, Class<PseudoSecret> targetType, ConversionContext context) {
        Optional<String> id = ConversionService.SHARED.convert(propertyMap.get("id"), String.class);
        Optional<String> content = ConversionService.SHARED.convert(propertyMap.get("content"), String.class);
        Optional<String> type = ConversionService.SHARED.convert(propertyMap.get("type"), String.class);

        return content.isPresent()
          ? Optional.of(new PseudoSecret(id.orElse(null), content.get(), type.orElse(null)))
          : Optional.empty();
    }

}
