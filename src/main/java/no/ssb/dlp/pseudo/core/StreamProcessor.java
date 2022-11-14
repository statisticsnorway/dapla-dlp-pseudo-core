package no.ssb.dlp.pseudo.core;

import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.io.InputStream;

public interface StreamProcessor {
    <T> Flowable<T> process(InputStream is, RecordMapSerializer<T> serializer);
}
