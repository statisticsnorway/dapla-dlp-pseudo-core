package no.ssb.dlp.pseudo.core.csv;

import com.google.common.base.Joiner;
import io.reactivex.Flowable;
import io.reactivex.Single;
import no.ssb.dlp.pseudo.core.map.MapTraverser;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class CsvRecordMapSerializer implements RecordMapSerializer<String> {

    private List<String> headers = new ArrayList<>();
    private static final char SEPARATOR = ';';
    private static final Joiner JOINER = Joiner.on(SEPARATOR).useForNull("null");

    // This implementation is a bit so-so - deducing header stuff from only one record.
    @Override
    public String serialize(Map<String, Object> r, int position) {
        boolean recordHeaders = headers.isEmpty();
        boolean printHeaders = position == 0;
        List<String> values = new ArrayList<>();
        MapTraverser.traverse(r, (field, value) -> {
            if (recordHeaders) {
                headers.add(field.getName());
            }
            values.add(value);
            return null;
        });

        if (values.size() != headers.size()) {
            throw new CsvSerializationException("CSV value to header mismatch for record at pos=" + position +
              ". Expected CSV row to have " + headers.size() + " columns, but encountered " + values.size() +
              ". This can happen if the source document does not contain values for all fields.");
        }

        return (printHeaders ?
          JOINER.join(headers) + System.lineSeparator() : "") +
          JOINER.join(values) + System.lineSeparator();
    }

    @Override
    public Flowable<String> serialize(Flowable<Map<String, Object>> recordStream) {
        AtomicInteger position = new AtomicInteger(0);

        return recordStream
          .map(r -> serialize(r, position.getAndIncrement()))
          .concatWith(Single.just("\\n"));
    }

    public static class CsvSerializationException extends RuntimeException {
        public CsvSerializationException(String message) {
            super(message);
        }
    }
}
