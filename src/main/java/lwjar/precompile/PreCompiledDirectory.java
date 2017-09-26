package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

class PreCompiledDirectory {
    private final Directory directory;

    PreCompiledDirectory(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
    }
    
    boolean has(RelativePath relativePath) {
        ProcessingFile file = this.directory.resolve(relativePath);
        return file.exists();
    }

    ProcessingFile resolve(RelativePath relativePath) {
        return this.directory.resolve(relativePath);
    }
    
    boolean exists() {
        return this.directory.exists();
    }
    
    RelativePath relative(ProcessingFile file) {
        return this.directory.relativePath(file);
    }

    List<ProcessingFile> collectSourceFiles() throws IOException {
        return this.directory.collectFiles(ProcessingFile::isJavaSource);
    }

    String getStringPath() {
        return this.directory.getStringPath();
    }
}
