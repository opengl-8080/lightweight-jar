package lwjar.precompile;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class PreCompiledDirectory {
    private final Directory directory;

    PreCompiledDirectory(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
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
