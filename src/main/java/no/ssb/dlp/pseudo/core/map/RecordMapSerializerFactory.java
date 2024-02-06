package no.ssb.dlp.pseudo.core.map;

import io.micronaut.http.MediaType;
import io.reactivex.Flowable;
import no.ssb.dlp.pseudo.core.csv.CsvRecordMapSerializer;
import no.ssb.dlp.pseudo.core.file.MoreMediaTypes;
import no.ssb.dlp.pseudo.core.json.JsonRecordMapSerializer;

import java.util.Map;

public class RecordMapSerializerFactory {

    private RecordMapSerializerFactory() {}

    public static RecordMapSerializer<String> newFromMediaType(MediaType mediaType) {
        if (MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return new JsonRecordMapSerializer();
        }
        if (MoreMediaTypes.TEXT_CSV_TYPE.equals(mediaType)) {
            return new CsvRecordMapSerializer();
        }
        else {
            throw new IllegalArgumentException("Unsupported media type - no RecordMapSerializer implementation exists for " + mediaType);
        }
    }

    public static RecordMapSerializer<String> emptySerializer() {
        return new RecordMapSerializer<>() {
            @Override
            public String serialize(Map<String, Object> r, int position) {
                return "";
            }

            @Override
            public Flowable<String> serialize(Flowable<Map<String, Object>> recordStream) {
                return Flowable.empty();
            }
        };
    }


}
