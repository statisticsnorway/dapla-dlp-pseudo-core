package no.ssb.dlp.pseudo.core;

import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.core.map.RecordMapSerializer;

import java.io.InputStream;

public interface StreamPseudonymizer {
    <T> Flowable<T> pseudonymize(InputStream is, RecordMapSerializer<T> serializer);
    <T> Flowable<T> depseudonymize(InputStream is, RecordMapSerializer<T> serializer);
}
