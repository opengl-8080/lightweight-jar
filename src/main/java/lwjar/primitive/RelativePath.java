package lwjar.primitive;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class RelativePath {
    private final Path path;

    public RelativePath(String path) {
        this(Paths.get(path));
    }

    public RelativePath(Path path) {
        this.path = Objects.requireNonNull(path);
        if (path.isAbsolute()) {
            throw new IllegalArgumentException("path is absolute path.");
        }
    }
    
    public RelativePath getParentPath() {
        return new RelativePath(this.path.getParent());
    }
    
    public String toSlashPathString() {
        return this.path.toString().replace("\\", "/");
    }
    
    public Path getPath() {
        return this.path;
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
