package no.ssb.dlp.pseudo.core.csv;

import com.univocity.parsers.common.record.Record;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.StreamProcessor;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class CsvStreamProcessor implements StreamProcessor {

    private final RecordMapProcessor recordMapProcessor;

    @Override
    public <T> Flowable<T> init(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(is, serializer, (map) -> recordMapProcessor.init(map));
    }

    @Override
    public <T> Flowable<T> process(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(is, serializer, (map) -> recordMapProcessor.process(map));
    }

    <T> CsvProcessorContext<T> initCsvProcessorContext(InputStream is, RecordMapSerializer<T> serializer) {
        CsvParserSettings settings = new CsvParserSettings();
        settings.detectFormatAutomatically();
        settings.setHeaderExtractionEnabled(true);
        final CsvParser csvParser = new CsvParser(settings);
        csvParser.beginParsing(is);
        return new CsvProcessorContext<>(csvParser, serializer);
    }

    private <T> Flowable<T> processStream(InputStream is, RecordMapSerializer<T> serializer, ItemProcessor processor) {
        return Flowable.generate(
                () -> initCsvProcessorContext(is, serializer),
                (ctx, emitter) -> {this.processItem(ctx, emitter, processor);}
        );
    }

    private <T> void processItem(CsvProcessorContext<T> ctx, Emitter<T> emitter, ItemProcessor processor)  {
        Record r = ctx.csvParser.parseNextRecord();
        if (r != null) {
            int position = ctx.currentPosition.getAndIncrement();
            Map<String, Object> recordMap = r.fillFieldObjectMap(new LinkedHashMap<>());
            Map<String, Object> processedRecord = processor.process(recordMap);
            emitter.onNext(ctx.getSerializer().serialize(processedRecord, position));
        }
        else {
            emitter.onComplete();
        }
    }

    @Value
    static class CsvProcessorContext<T> {
        private final CsvParser csvParser;
        private final RecordMapSerializer<T> serializer;
        private final AtomicInteger currentPosition = new AtomicInteger();
    }

}
