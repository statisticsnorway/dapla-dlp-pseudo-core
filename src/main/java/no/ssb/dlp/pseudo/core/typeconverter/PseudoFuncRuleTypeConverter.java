package no.ssb.dlp.pseudo.core.typeconverter;

import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.ConversionService;
import io.micronaut.core.convert.TypeConverter;
import no.ssb.dlp.pseudo.core.PseudoFuncRule;

import java.util.Map;
import java.util.Optional;

public class PseudoFuncRuleTypeConverter implements TypeConverter<Map, PseudoFuncRule> {

    @Override
    public Optional<PseudoFuncRule> convert(Map map, Class<PseudoFuncRule> targetType, ConversionContext context) {
        Optional<String> name = ConversionService.SHARED.convert(map.get("name"), String.class);
        Optional<String> pattern = ConversionService.SHARED.convert(map.get("pattern"), String.class);
        Optional<String> func = ConversionService.SHARED.convert(map.get("func"), String.class);

        return pattern.isPresent() && func.isPresent()
          ? Optional.of(new PseudoFuncRule(name.orElse(null), pattern.get(), func.get()))
          : Optional.empty();
    }
}
