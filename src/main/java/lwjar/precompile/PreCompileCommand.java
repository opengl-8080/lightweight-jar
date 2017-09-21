package lwjar.precompile;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.TooManyCompileErrorException;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
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
    
    private final Path workDir;

    public PreCompileCommand(String encoding, Path orgSrcDir, Path orgClassesDir, Path outDir, Integer compressLevel) {
        this.encoding = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        
        this.librarySourceDirectory = new LibrarySourceDirectory(new Directory(orgSrcDir));
        this.libraryClassDirectory = new LibraryClassDirectory(new Directory(orgClassesDir));
        this.javaSourceCompressor = new JavaSourceCompressor(JavaSourceCompressor.CompressLevel.valueOf(compressLevel));

        if (outDir == null) {
            outDir = Paths.get("./out");
        }
        
        this.preCompiledDirectory = new PreCompiledDirectory(outDir.resolve("src"));
        this.compileErrorLog = outDir.resolve("compile-errors.log");
        
        this.workDir = outDir.resolve("work");
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();
        this.createWorkDir();

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

        this.libraryClassDirectory.copyClassFileOnly(this.preCompiledDirectory);
        this.libraryClassDirectory.copyNoJavaAndClassFiles(this.preCompiledDirectory);
    }

    private void copyOrgToOut() throws IOException {
        if (this.preCompiledDirectory.exists()) {
            return;
        }

        System.out.println("copy and compressing source files...");
        this.librarySourceDirectory.copyTo(this.preCompiledDirectory, this.javaSourceCompressor);
    }

    private void createWorkDir() {
        try {
            Files.createDirectories(this.workDir);
        } catch (AccessDeniedException e) {
            // skip this. because this error is sometimes occurred without any problems. (I don't understand well...)
            System.err.println("Warning: " + e.getClass() + " : " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create work directory.", e);
        }
    }

    private void replaceErrorFiles(CompileResult result) throws IOException {
        UncompilableJavaSources errorSourceFiles = result.getErrorSourceFiles(this.preCompiledDirectory);

        System.out.println("remove error source files...");
//        errorSourceFiles.forEach(System.out::println);

        this.libraryClassDirectory.copyErrorClassFilesFromOrgToOut(this.preCompiledDirectory, errorSourceFiles);
        this.removeErrorJavaFileFromOut(errorSourceFiles);
    }

    private void removeErrorJavaFileFromOut(UncompilableJavaSources errorSourceFiles) {
        errorSourceFiles.forEach(relativeJavaSourcePath -> {
            ProcessingFile outJavaFile = this.preCompiledDirectory.resolve(relativeJavaSourcePath);
            outJavaFile.delete();
        });
    }

    private CompileResult compile() throws IOException {
        this.recreateWorkDir();

        List<String> sourceFiles = this.preCompiledDirectory.collectSourceFiles();
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
    
    private void recreateWorkDir() throws IOException {
        System.out.println("recreate work directory...");
        this.removeWorkDir();
        this.createWorkDir();
    }
    
    private void removeWorkDir() {
        try {
            Files.walkFileTree(this.workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("failed remove work directory.", e);
        }
    }
    
    private String[] buildJavacArgs(List<String> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.preCompiledDirectory.stringPath());
        javacOptions.add("-encoding");
        javacOptions.add(this.encoding.toString());
        javacOptions.addAll(sourceFiles);

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
            Set<String> errorSrcPathSet = new HashSet<>();

            while (matcher.find()) {
                String sourcePath = matcher.group(1);
                errorSrcPathSet.add(sourcePath);
            }

            Set<RelativeJavaSourcePath> paths
                    = errorSrcPathSet.stream()
                        .map(Paths::get)
                        .map(preCompiledDirectory::relativize)
                        .sorted()
                        .map(RelativeJavaSourcePath::new)
                        .collect(toSet());
            
            return new UncompilableJavaSources(paths);
        }
    }
}
