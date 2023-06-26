package no.ssb.dlp.pseudo.core;

import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.io.InputStream;
import java.util.Map;

public interface StreamProcessor {
    <T> Flowable<T> init(InputStream is, RecordMapSerializer<T> serializer);
    <T> Flowable<T> process(InputStream is, RecordMapSerializer<T> serializer);
    @FunctionalInterface
    public interface ItemProcessor {
        Map<String, Object> process(Map<String, Object> r);
    }
}
