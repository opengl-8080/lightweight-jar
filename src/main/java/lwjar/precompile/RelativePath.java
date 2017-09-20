package lwjar.precompile;

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
    
    Path path() {
        return this.path;
    }
}
