package no.ssb.dlp.pseudo.core.map;

import com.google.common.base.Joiner;
import no.ssb.dapla.dlp.pseudo.func.util.FromString;
import no.ssb.dlp.pseudo.core.field.FieldDescriptor;
import no.ssb.dlp.pseudo.core.field.ValueInterceptor;
import no.ssb.dlp.pseudo.core.util.MoreCollectors;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MapTraverser {
    private static final Joiner PATH_JOINER = Joiner.on("/").skipNulls();
    private static final String ROOT_PATH = "";

    private MapTraverser() {}

    public static <T extends Map<String,Object>> T traverse(T map, ValueInterceptor interceptor) {
        return (T) traverse(ROOT_PATH, map, interceptor);
    }

    private static Object traverse(String path, Object node, ValueInterceptor interceptor) {
        if (node instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) node;
            return map.entrySet().stream()
                .collect(MoreCollectors.toMapWithNullValues(
                  Map.Entry::getKey,
                  e -> {
                      String nextPath = PATH_JOINER.join(path, e.getKey());
                      return isTraversable(e.getValue())
                        ? traverse(nextPath, e.getValue(), interceptor)
                        : processValue(e.getValue(), nextPath, interceptor);
                  },
                  RecordMap::new
                ));
        }
        else if (node instanceof Collection) {
            Collection<Object> collection = (Collection<Object>) node;
            AtomicInteger i = new AtomicInteger();
            return collection.stream()
              .map(value -> {
                  String nextPath = path + "[" + i.getAndIncrement() + "]";
                  return isTraversable(value)
                    ? traverse(nextPath, value, interceptor)
                    : processValue(value, nextPath, interceptor);
                })
              .toList();
        }
        else {
            return processValue(node, path, interceptor);
        }
    }

    static Object processValue(Object value, String path, ValueInterceptor interceptor) {
        String newValue = interceptor.apply(new FieldDescriptor(path), (value == null) ? null : String.valueOf(value));
        if (newValue != null) {
            return (value == null)
              ? newValue
              : FromString.convert(newValue, value.getClass());
        }

        return value;
    }

    private static boolean isTraversable(Object o) {
        return (o instanceof Map) || (o instanceof Collection);
    }

}
