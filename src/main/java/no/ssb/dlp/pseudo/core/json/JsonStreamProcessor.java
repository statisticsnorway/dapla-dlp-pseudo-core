package no.ssb.dlp.pseudo.core.json;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.reactivex.Completable;
import io.reactivex.Emitter;
import io.reactivex.Flowable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import no.ssb.dlp.pseudo.core.StreamProcessor;
import no.ssb.dlp.pseudo.core.map.RecordMap;
import no.ssb.dlp.pseudo.core.map.RecordMapProcessor;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Slf4j
public class JsonStreamProcessor implements StreamProcessor {

    private static final ObjectMapper OBJECT_MAPPER;

    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    private final RecordMapProcessor recordMapProcessor;

    @Override
    public <T> Completable init(InputStream is) {
        if (recordMapProcessor.hasPreprocessors()) {
            return Completable.fromPublisher(processStream(is, RecordMapSerializerFactory.emptySerializer(), recordMapProcessor::init));
        } else {
            return Completable.complete();
        }
    }

    @Override
    public <T> Flowable<T> process(InputStream is, RecordMapSerializer<T> serializer) {
        return processStream(is, serializer, (map) -> recordMapProcessor.process(map));
    }

    <T> JsonProcessorContext<T> initJsonProcessorContext(InputStream is, RecordMapSerializer<T> serializer) throws IOException {
        final JsonParser jsonParser = OBJECT_MAPPER.getFactory().createParser(is);
        return new JsonProcessorContext<>(jsonParser, serializer);
    }

    private <T> Flowable<T> processStream(InputStream is, RecordMapSerializer<T> serializer,
                                          ItemProcessor processor) {
        return Flowable.generate(
          () -> initJsonProcessorContext(is, serializer),
          (ctx, emitter) -> {this.processItem(ctx, emitter, processor);},
          JsonProcessorContext::close
        );
    }

    private <T> void processItem(JsonProcessorContext<T> ctx, Emitter<T> emitter,
                                 ItemProcessor processor) throws IOException {
        JsonParser jsonParser = ctx.getJsonParser();
        JsonToken jsonToken = jsonParser.nextToken();
        while (jsonToken == JsonToken.START_ARRAY || jsonToken == JsonToken.END_ARRAY) {
            jsonToken = jsonParser.nextToken();
        }

        if (jsonToken != null) {
            int position = ctx.currentPosition.getAndIncrement();
            Map<String, Object> r = OBJECT_MAPPER.readValue(jsonParser, RecordMap.class);
            Map<String, Object> processedRecord = processor.process(r);
            emitter.onNext(ctx.getSerializer().serialize(processedRecord, position));
        }
        else {
            emitter.onComplete();
        }
    }

    @Value
    static class JsonProcessorContext<T> {
        private final JsonParser jsonParser;
        private final RecordMapSerializer<T> serializer;
        private final AtomicInteger currentPosition = new AtomicInteger();

        public void close() throws IOException {
            jsonParser.close();
        }
    }

}
