package lwjar.precompile;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.TooManyCompileErrorException;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.*;

public class PreCompileCommand implements Command {
    
    private final Charset encoding;

    private final LibrarySourceDirectory librarySourceDirectory;
    private final LibraryClassDirectory libraryClassDirectory;
    private final PreCompiledDirectory preCompiledDirectory;

    private final JavaSourceCompressor javaSourceCompressor;

    private final Path compileErrorLog;
    
    private final CompileWorkDirectory workDir;

    public PreCompileCommand(String encoding, Path orgSrcDir, Path orgClassesDir, Path outDir, Integer compressLevel) {
        this.encoding = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        
        this.librarySourceDirectory = new LibrarySourceDirectory(new Directory(orgSrcDir));
        this.libraryClassDirectory = new LibraryClassDirectory(new Directory(orgClassesDir));
        this.javaSourceCompressor = new JavaSourceCompressor(JavaSourceCompressor.CompressLevel.valueOf(compressLevel));

        if (outDir == null) {
            outDir = Paths.get("./out");
        }
        
        this.preCompiledDirectory = new PreCompiledDirectory(new Directory(outDir.resolve("src")));
        this.compileErrorLog = outDir.resolve("compile-errors.log");
        
        this.workDir = new CompileWorkDirectory(new Directory(outDir.resolve("work")));
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();
        this.workDir.create();

        int cnt = 0;
        CompileResult result = this.compile();

        while (result.error) {
            this.replaceErrorFiles(result);

            cnt++;
            if (100 < cnt) {
                throw new TooManyCompileErrorException("too many compile errors are occurred.");
            }

            result = this.compile();
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

    private CompileResult compile() throws IOException {
        this.workDir.recreate();

        List<ProcessingFile> sourceFiles = this.preCompiledDirectory.collectSourceFiles();
        String[] args = this.buildJavacArgs(sourceFiles);

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler()
                .run(null, null, error, args);

        String errorMessage = error.toString(Charset.defaultCharset().toString());

        if (!errorMessage.isEmpty()) {
            Files.write(this.compileErrorLog, error.toByteArray(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        return new CompileResult(resultCode != 0, errorMessage);
    }
    
    private String[] buildJavacArgs(List<ProcessingFile> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.getStringPath());
        javacOptions.add("-cp");
        javacOptions.add(this.preCompiledDirectory.getStringPath());
        javacOptions.add("-encoding");
        javacOptions.add(this.encoding.toString());
        javacOptions.addAll(sourceFiles.stream().map(ProcessingFile::getAbsolutePathString).collect(toList()));

        return javacOptions.toArray(new String[javacOptions.size()]);
    }
    

    private static class CompileResult {
        private final boolean error;
        private final String errorMessage;

        private CompileResult(boolean error, String errorMessage) {
            this.error = error;
            this.errorMessage = errorMessage;
        }

        private UncompilableJavaSources getErrorSourceFiles(PreCompiledDirectory preCompiledDirectory) throws IOException {
            System.out.println("extracting error source files...");
            
            Pattern pattern = Pattern.compile("^([^ \\r\\n]+\\.java):\\d+:", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(this.errorMessage);
            Set<UncompilableJavaSource> paths = new HashSet<>();

            while (matcher.find()) {
                Path absoluteSourcePath = Paths.get(matcher.group(1));
                ProcessingFile uncompilableSource = new ProcessingFile(absoluteSourcePath);
                RelativePath relativePath = preCompiledDirectory.relative(uncompilableSource);
                UncompilableJavaSource uncompilableJavaSource = new UncompilableJavaSource(relativePath.getPath());
                paths.add(uncompilableJavaSource);
            }
            
            return new UncompilableJavaSources(paths);
        }
    }
}
