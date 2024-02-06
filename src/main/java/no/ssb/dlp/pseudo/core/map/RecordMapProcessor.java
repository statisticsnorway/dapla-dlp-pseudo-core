package no.ssb.dlp.pseudo.core.map;

import io.reactivex.processors.FlowableProcessor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptorChain;

import java.util.Map;

@RequiredArgsConstructor
public class RecordMapProcessor<T> {
    private final ValueInterceptorChain valueInterceptorChain;
    @Getter
    private final MetadataProcessor<T> metadataProcessor;
    @FunctionalInterface
    public interface MetadataProcessor<T> {
        // The MetadataProcessor is used to publish/subscribe to events related to the processing of each RecordMap
        FlowableProcessor<T> toFlowableProcessor();
    }
    public Map<String, Object> init(Map<String, Object> r) {
        return MapTraverser.traverse(r, valueInterceptorChain::init);
    }

    public Map<String, Object> process(Map<String, Object> r) {
        return MapTraverser.traverse(r, valueInterceptorChain::apply);
    }

    public boolean hasPreprocessors() {
        return valueInterceptorChain.hasPreprocessors();
    }
}
