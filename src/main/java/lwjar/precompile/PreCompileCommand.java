package lwjar.precompile;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.TooManyCompileErrorException;
import lwjar.primitive.Directory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class PreCompileCommand implements Command {

    private final LibrarySourceDirectory librarySourceDirectory;
    private final LibraryClassDirectory libraryClassDirectory;
    private final PreCompiledDirectory preCompiledDirectory;

    private final JavaSourceCompressor javaSourceCompressor;

    private final PreCompiler preCompiler;

    public PreCompileCommand(String encoding, Path orgSrcDir, Path orgClassesDir, Path outDir, Integer compressLevel) {
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        
        this.librarySourceDirectory = new LibrarySourceDirectory(new Directory(orgSrcDir));
        this.libraryClassDirectory = new LibraryClassDirectory(new Directory(orgClassesDir));
        this.javaSourceCompressor = new JavaSourceCompressor(JavaSourceCompressor.CompressLevel.valueOf(compressLevel));

        OutputDirectory outputDirectory = new OutputDirectory(outDir);

        this.preCompiledDirectory = new PreCompiledDirectory(outputDirectory);

        this.preCompiler = new PreCompiler(outputDirectory, this.preCompiledDirectory);
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();

        int cnt = 0;
        CompileResult result = this.preCompiler.compile();

        while (result.isError()) {
            this.replaceErrorFiles(result);

            cnt++;
            if (100 < cnt) {
                throw new TooManyCompileErrorException("too many compile errors are occurred.");
            }

            result = this.preCompiler.compile();
        }

        this.libraryClassDirectory.copyClassFileThatOnlyExistsInBinaryJar(this.preCompiledDirectory);
        this.libraryClassDirectory.copyResourceFilesTo(this.preCompiledDirectory);
    }

    private void copyOrgToOut() throws IOException {
        if (this.preCompiledDirectory.exists()) {
            return;
        }

        System.out.println("copy and compressing source files...");
        this.librarySourceDirectory.copyTo(this.preCompiledDirectory, this.javaSourceCompressor);
    }

    private void replaceErrorFiles(CompileResult result) throws IOException {
        UncompilableJavaSources errorSourceFiles = result.getErrorSourceFiles(this.preCompiledDirectory);

        System.out.println("remove error source files...");
//        errorSourceFiles.forEach(System.out::println);

        this.libraryClassDirectory.copyErrorClassFiles(this.preCompiledDirectory, errorSourceFiles);
        errorSourceFiles.removeFiles();
    }
}
