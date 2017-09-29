package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.RelativePath;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputDirectory {
    private final Directory directory;

    public OutputDirectory(Path dir) {
        this.directory = dir == null
                        ? new Directory(Paths.get("./out"))
                        : new Directory(dir);
    }

    public Directory resolve(String path) {
        return this.directory.resolveDirectory(new RelativePath(Paths.get(path)));
    }
}
