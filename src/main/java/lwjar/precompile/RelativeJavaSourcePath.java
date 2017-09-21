package lwjar.precompile;

import java.nio.file.Path;

public class RelativeJavaSourcePath {
    private final Path path;

    public RelativeJavaSourcePath(Path path) {
        this.path = path;
    }

    public String getClassName() {
        return this.path.getFileName().toString().replaceAll("\\.java", "");
    }

    public RelativePath parentDir() {
        return new RelativePath(this.path.getParent());
    }

    public Path path() {
        return this.path;
    }
}
