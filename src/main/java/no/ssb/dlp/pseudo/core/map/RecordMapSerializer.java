package no.ssb.dlp.pseudo.core.map;

import io.reactivex.Flowable;

import java.util.Map;

/**
 * @see no.ssb.dlp.pseudo.core.json.JsonRecordMapSerializer
 * @see no.ssb.dlp.pseudo.core.csv.CsvRecordMapSerializer
 */
public interface RecordMapSerializer<T> {

    /**
     * Serialize a RecordMap to some implementation specific format.
     *
     * @param r the RecordMap to serialize
     * @param position the record's sequence number
     * @return a serialized RecordMap
     */
    T serialize(Map<String, Object> r, int position);

    /**
     * Serialize a {@link Flowable} of records.
     *
     * @param recordStream the stream of records to serialize
     * @return a {@link Flowable} of the serialized records
     */
    Flowable<T> serialize(Flowable<Map<String, Object>> recordStream);
}
