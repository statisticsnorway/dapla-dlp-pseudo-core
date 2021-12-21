package no.ssb.dlp.pseudo.core.csv;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.PseudoOperation;
import no.ssb.dlp.pseudo.core.StreamPseudonymizer;
import no.ssb.dlp.pseudo.core.map.RecordMapPseudonymizer;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class CsvStreamPseudonymizer implements StreamPseudonymizer {

    private final RecordMapPseudonymizer recordPseudonymizer;

    @Override
    public <T> Flowable<T> pseudonymize(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(PseudoOperation.PSEUDONYMIZE, is, serializer);
    }

    @Override
    public <T> Flowable<T> depseudonymize(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(PseudoOperation.DEPSEUDONYMIZE, is, serializer);
    }

    <T> CsvProcessorContext<T> initCsvProcessorContext(PseudoOperation operation, InputStream is, RecordMapSerializer<T> serializer) throws IOException {
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setHeaderExtractionEnabled(true);
        final CsvParser csvParser = new CsvParser(settings);
        csvParser.beginParsing(is);
        return new CsvProcessorContext<>(operation, csvParser, serializer);
    }

    private <T> Flowable<T> processStream(PseudoOperation operation, InputStream is, RecordMapSerializer<T> serializer) {
        return Flowable.generate(
          () -> initCsvProcessorContext(operation, is, serializer),
          (ctx, emitter) -> {this.processItem(ctx, emitter);}
        );
    }

    private <T> void processItem(CsvProcessorContext<T> ctx, Emitter<T> emitter)  {
        Record record = ctx.csvParser.parseNextRecord();
        if (record != null) {
            int position = ctx.currentPosition.getAndIncrement();
            Map<String, Object> recordMap = record.fillFieldObjectMap(new LinkedHashMap<>());
            Map<String, Object> processedRecord = ctx.operation == PseudoOperation.PSEUDONYMIZE
              ? recordPseudonymizer.pseudonymize(recordMap)
              : recordPseudonymizer.depseudonymize(recordMap);
            emitter.onNext(ctx.getSerializer().serialize(processedRecord, position));
        }
        else {
            emitter.onComplete();
        }
    }

    @Value
    static class CsvProcessorContext<T> {
        private final PseudoOperation operation;
        private final CsvParser csvParser;
        private final RecordMapSerializer<T> serializer;
        private final AtomicInteger currentPosition = new AtomicInteger();
    }

}
