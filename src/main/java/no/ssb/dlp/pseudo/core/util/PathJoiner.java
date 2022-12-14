package no.ssb.dlp.pseudo.core.util;

import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.stream.Collectors;

@UtilityClass
public class PathJoiner {
    public static String joinAndKeepLeadingSlash(String... pathFragments) {
        return "/" + joinWithoutLeadingOrTrailingSlash(pathFragments);
    }

    public static String joinAndKeepTrailingSlash(String... pathFragments) {
        return joinWithoutLeadingOrTrailingSlash(pathFragments) + "/";
    }

    public static String joinAndKeepLeadingAndTrailingSlash(String... pathFragments) {
        return "/" + joinWithoutLeadingOrTrailingSlash(pathFragments) + "/";
    }

    public static String joinWithoutLeadingOrTrailingSlash(String... pathFragments) {
        return Arrays.asList(pathFragments).stream()
          .filter(path -> path != null && path.trim().length() > 0)
          .map(path -> stripTrailingSlashes(stripLeadingSlashes(path)))
          .collect(Collectors.joining("/"));
    }

    public static String stripTrailingSlashes(String input) {
        return input.endsWith("/") ? stripTrailingSlashes(input.substring(0, input.length() - 1)) : input;
    }

    public static String stripLeadingSlashes(String input) {
        return input.startsWith("/") ? stripLeadingSlashes(input.substring(1)) : input;
    }

}
