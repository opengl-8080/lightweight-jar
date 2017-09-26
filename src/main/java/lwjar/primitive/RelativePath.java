package lwjar.primitive;

import java.nio.file.Path;
import java.util.Objects;

public class RelativePath {
    private final Path path;

    public RelativePath(Path path) {
        this.path = Objects.requireNonNull(path);
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("path is absolute path.");
        }
    }
    
    public Path getPath() {
        return this.path;
    }
    
    protected String getName() {
        return this.path.getFileName().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RelativePath that = (RelativePath) o;

        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}
