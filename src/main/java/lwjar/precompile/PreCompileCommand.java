package lwjar.precompile;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.OutputDirectory;
import lwjar.TooManyCompileErrorException;
import lwjar.primitive.Directory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class PreCompileCommand implements Command {
    private static final int DEFAULT_RETRY_COUNT = 100;
    
    private final int retryCount;

    private final ApplicationSourceDirectory applicationSourceDirectory;
    private final LibrarySourceDirectory librarySourceDirectory;
    private final LibraryClassDirectory libraryClassDirectory;
    private final PreCompiledDirectory preCompiledDirectory;

    private final PreCompiler preCompiler;
    private final JavaSourceCompressor.CompressLevel compressLevel;

    public PreCompileCommand(String encoding, Path applicationSourceDir, Path orgSrcDir, Path orgClassesDir, Path outDir, Integer compressLevel, Integer retryCount) {
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        
        this.retryCount = retryCount == null ? DEFAULT_RETRY_COUNT : retryCount;
        
        this.applicationSourceDirectory = new ApplicationSourceDirectory(new Directory(applicationSourceDir));
        this.librarySourceDirectory = new LibrarySourceDirectory(new Directory(orgSrcDir));
        this.libraryClassDirectory = new LibraryClassDirectory(new Directory(orgClassesDir));

        OutputDirectory outputDirectory = new OutputDirectory(outDir);

        this.preCompiledDirectory = new PreCompiledDirectory(outputDirectory);

        this.preCompiler = new PreCompiler(outputDirectory, this.preCompiledDirectory);
        
        this.compressLevel = JavaSourceCompressor.CompressLevel.valueOf(compressLevel);
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
