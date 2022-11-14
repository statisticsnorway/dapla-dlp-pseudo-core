package no.ssb.dlp.pseudo.core.map;

import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptorChain;

import java.util.Map;

@RequiredArgsConstructor
public class RecordMapProcessor {
    private final ValueInterceptorChain valueInterceptorChain;

    public Map<String, Object> process(Map<String, Object> r) {
        return MapTraverser.traverse(r, valueInterceptorChain::apply);
    }

}
