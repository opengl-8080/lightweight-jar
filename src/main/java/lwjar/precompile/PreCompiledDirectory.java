package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

class PreCompiledDirectory {
    private final Directory directory;

    PreCompiledDirectory(OutputDirectory outputDirectory) {
        this.directory = outputDirectory.resolve("src");
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

    JavaSourceFiles collectSourceFiles() throws IOException {
        return new JavaSourceFiles(this.directory.collectFiles(ProcessingFile::isJavaSource));
    }
    
    ClassPath asClassPath() {
        return new ClassPath(this.directory);
    }
}
