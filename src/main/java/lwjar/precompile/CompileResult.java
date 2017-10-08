package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;

class CompileResult {
    private final boolean success;
    private final Set<Path> errorFiles;

    CompileResult(boolean success, Set<Path> errorFiles) {
        this.success = success;
        this.errorFiles = errorFiles;
    }

    UncompilableJavaSources getErrorSourceFiles(PreCompiledDirectory preCompiledDirectory) throws IOException {
        Set<UncompilableJavaSource> uncompilableJavaSourceSet = this.errorFiles.stream()
                .map(path -> {
                    ProcessingFile uncompilableSource = new ProcessingFile(path);
                    return new UncompilableJavaSource(preCompiledDirectory, uncompilableSource);
                })
                .collect(Collectors.toSet());
        
        return new UncompilableJavaSources(uncompilableJavaSourceSet);
    }

    boolean isError() {
        return !this.success;
    }
}
