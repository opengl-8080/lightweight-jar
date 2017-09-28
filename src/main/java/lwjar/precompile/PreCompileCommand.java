package lwjar.precompile;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.TooManyCompileErrorException;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

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

        if (outDir == null) {
            outDir = Paths.get("./out");
        }
        
        this.preCompiledDirectory = new PreCompiledDirectory(new Directory(outDir.resolve("src")));
        
        ErrorLogFile compileErrorLog = new ErrorLogFile(new Directory(outDir));
        Directory outputDir = new Directory(outDir);
        this.preCompiler = new PreCompiler(compileErrorLog, outputDir);
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();

        int cnt = 0;
        ClassPath classPath = this.preCompiledDirectory.asClassPath();
        CompileResult result = this.preCompiler.compile(this.preCompiledDirectory.collectSourceFiles(), classPath);

        while (result.isError()) {
            this.replaceErrorFiles(result);

            cnt++;
            if (100 < cnt) {
                throw new TooManyCompileErrorException("too many compile errors are occurred.");
            }

            result = this.preCompiler.compile(this.preCompiledDirectory.collectSourceFiles(), classPath);
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
        this.removeErrorJavaFileFromOut(errorSourceFiles);
    }

    private void removeErrorJavaFileFromOut(UncompilableJavaSources errorSourceFiles) {
        errorSourceFiles.forEach(uncompilableJavaSource -> {
            ProcessingFile outJavaFile = this.preCompiledDirectory.resolve(uncompilableJavaSource);
            outJavaFile.delete();
        });
    }
}
