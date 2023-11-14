package no.ssb.dlp.pseudo.core.field;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.Optional;

public class FieldDescriptor {
    private final String path;
    private final String name;

    public FieldDescriptor(String s) {
        s = Optional.ofNullable(s).orElse("");
        this.path = s.startsWith("/") ? s : "/" + s;
        this.name = this.path.substring(this.path.lastIndexOf('/') + 1);
    }

    public static FieldDescriptor from(String path) {
        return new FieldDescriptor(path);
    }

    /**
     * The path of the field (including the field name itself). Path elements are separated by a
     * forward slash. The root path is expressed as a single slash (/). Path can never be
     * null or empty.
     *
     * Given an object structured like this:
     * <pre>
     *    {
     *         "foo": {
     *             "bar": 123
     *         }
     *     }
     * </pre>
     *
     * Then, field paths would be...
     * <pre>
     *     root -> /
     *     foo  -> /foo
     *     bar  -> /foo/bar
     * </pre>
     */
    public String getPath() {
        return path;
    }

    /** The name of the field. Never null or empty */
    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FieldDescriptor field = (FieldDescriptor) o;

        if (!path.equals(field.path)) return false;
        return name.equals(field.name);
    }

    /**
     * Return true iff the value matches a specified glob pattern
     *
     * Glob syntax follows several simple rules, see:
     * https://docs.oracle.com/javase/tutorial/essential/io/fileOps.html#glob
     */
    public boolean globMatches(String globPattern) {
        return FileSystems.getDefault().getPathMatcher("glob:" + globPattern).matches(Paths.get(path));
    }

    @Override
    public int hashCode() {
        int result = path.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }
}
