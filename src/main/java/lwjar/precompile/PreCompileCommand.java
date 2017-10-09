package lwjar.precompile;

import lwjar.cli.Command;
import lwjar.OutputDirectory;
import lwjar.TooManyCompileErrorException;
import lwjar.primitive.Directory;

import java.io.IOException;
import java.util.Objects;

public class PreCompileCommand implements Command {
    private static final int DEFAULT_RETRY_COUNT = 100;
    
    private final int retryCount;

    private final ApplicationSourceDirectory applicationSourceDirectory;
    private final LibrarySourceDirectory librarySourceDirectory;
    private final LibraryClassDirectory libraryClassDirectory;
    private final PreCompiledDirectory preCompiledDirectory;

    private final PreCompiler preCompiler;
    private final JavaSourceCompressor.CompressLevel compressLevel;

    public PreCompileCommand(
            LibrarySourceDirectory librarySourceDirectory,
            LibraryClassDirectory libraryClassDirectory,
            ApplicationSourceDirectory applicationSourceDir,
            OutputDirectory outputDirectory,
            JavaSourceCompressor.CompressLevel compressLevel,
            Integer retryCount) {
        
        this.retryCount = retryCount == null ? DEFAULT_RETRY_COUNT : retryCount;
        
        this.applicationSourceDirectory = Objects.requireNonNull(applicationSourceDir);
        this.librarySourceDirectory = Objects.requireNonNull(librarySourceDirectory);
        this.libraryClassDirectory = Objects.requireNonNull(libraryClassDirectory);
        this.preCompiledDirectory = new PreCompiledDirectory(outputDirectory);
        
        this.preCompiler = new PreCompiler(outputDirectory, this.preCompiledDirectory);
        this.compressLevel = compressLevel;
    }
    
    public Directory getPreCompiledDirectory() {
        return this.preCompiledDirectory.getDirectory();
    }

    @Override
    public void execute() throws IOException {
        JavaSourceCompressor javaSourceCompressor = new JavaSourceCompressor(this.compressLevel);
        this.preCompiledDirectory.copyJavaSourceFiles(this.librarySourceDirectory, javaSourceCompressor);

        int cnt = 0;
        CompileResult result = this.preCompiler.compile();

        while (result.isError()) {
            this.replaceErrorFiles(result);

            cnt++;
            if (this.retryCount < cnt) {
                throw new TooManyCompileErrorException("too many (" + this.retryCount + " times) compile errors are occurred.");
            }

            result = this.preCompiler.compile();
        }

        this.preCompiledDirectory.copyClassFileThatOnlyExistsInBinaryJar(this.libraryClassDirectory);
        this.preCompiledDirectory.copyResourceFilesFrom(this.libraryClassDirectory);
        this.preCompiledDirectory.copyApplicationSourceFiles(this.applicationSourceDirectory, javaSourceCompressor);
    }

    private void replaceErrorFiles(CompileResult result) throws IOException {
        UncompilableJavaSources uncompilableJavaSources = result.getErrorSourceFiles(this.preCompiledDirectory);

        this.preCompiledDirectory.copyUncompilableClassFiles(this.libraryClassDirectory, uncompilableJavaSources);
        uncompilableJavaSources.removeFiles();
    }
}
