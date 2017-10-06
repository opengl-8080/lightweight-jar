package lwjar;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
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

    public Directory resolveDirectory(String path) {
        return this.directory.resolveDirectory(new RelativePath(path));
    }

    public ProcessingFile resolveFile(RelativePath relativePath) {
        return this.directory.resolve(relativePath);
    }
}
