package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.nio.file.Path;
import java.nio.file.Paths;

class OutputDirectory {
    private final Directory directory;

    OutputDirectory(Path dir) {
        this.directory = dir == null
                        ? new Directory(Paths.get("./out"))
                        : new Directory(dir);
    }

    Directory resolveDirectory(String path) {
        return this.directory.resolveDirectory(new RelativePath(Paths.get(path)));
    }
    
    ProcessingFile resolveFile(RelativePath relativePath) {
        return this.directory.resolve(relativePath);
    }
}
