package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

class PreCompiledDirectory {
    private final Directory directory;

    PreCompiledDirectory(OutputDirectory outputDirectory) {
        this.directory = outputDirectory.resolveDirectory("src");
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

    JavaSourceFiles collectSourceFiles() {
        return new JavaSourceFiles(this.directory.collectFiles(ProcessingFile::isJavaSource));
    }
    
    String getStringPath() {
        return this.directory.getStringPath();
    }
}
