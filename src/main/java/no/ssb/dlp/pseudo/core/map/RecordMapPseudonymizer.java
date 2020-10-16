package no.ssb.dlp.pseudo.core.map;

import lombok.RequiredArgsConstructor;
import no.ssb.dlp.pseudo.core.FieldPseudonymizer;

import java.util.Map;

@RequiredArgsConstructor
public class RecordMapPseudonymizer {
    private final FieldPseudonymizer fieldPseudonymizer;

    public Map<String, Object> pseudonymize(Map<String, Object> record) {
        return MapTraverser.traverse(record, fieldPseudonymizer::pseudonymize);
    }

    public Map<String, Object> depseudonymize(Map<String, Object> record) {
        return MapTraverser.traverse(record, fieldPseudonymizer::depseudonymize);
    }
}
