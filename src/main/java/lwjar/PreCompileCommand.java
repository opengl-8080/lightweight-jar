package lwjar;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PreCompileCommand implements Command {
    
    private final Charset encoding;

    private final Path orgSrcDir;
    private final Optional<Path> orgClassesDir;

    private final Path outDir;
    private final Path outSrcDir;
    private final Path outCompressedSrcDir;
    
    private final Path workDir;

    public PreCompileCommand(String encoding, Path orgSrcDir, Path orgClassesDir, Path outDir) {
        this.encoding = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        this.orgSrcDir = orgSrcDir;
        this.orgClassesDir = Optional.ofNullable(orgClassesDir);

        this.outDir = outDir == null ? Paths.get("./out") : outDir;
        this.outSrcDir = this.outDir.resolve("src");
        this.outCompressedSrcDir = this.outDir.resolve("compressed");
        
        this.workDir = this.outDir.resolve("work");
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();
        this.createWorkDir();

        int cnt = 0;
        CompileResult result = this.compile();

        while (result.error) {
            Set<Path> errorSourceFiles = result.getErrorSourceFiles(this.outSrcDir);

            System.out.println("remove error source files...");
            errorSourceFiles.forEach(System.out::println);
            
            this.copyErrorClassFilesFromOrgToOut(errorSourceFiles);
            this.removeErrorJavaFileFromOut(errorSourceFiles);

            cnt++;
            if (100 < cnt) {
                throw new TooManyCompileErrorException("too many compile errors are occurred.");
            }

            result = this.compile();
        }
        
        this.compressSourceFile();
    }
    
    private void compressSourceFile() throws IOException {
        System.out.println("compressing source...");
        Files.walkFileTree(this.outSrcDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path outSrc, BasicFileAttributes attrs) throws IOException {
                try {
                    CompilationUnit cu = JavaParser.parse(outSrc);

                    PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
                    conf.setIndent("");
                    conf.setPrintComments(false);
                    conf.setPrintJavaDoc(false);
                    conf.setEndOfLineCharacter(" ");

                    String text = cu.toString(conf);

                    Path relativePath = PreCompileCommand.this.outSrcDir.relativize(outSrc);
                    Path compressed = PreCompileCommand.this.outCompressedSrcDir.resolve(relativePath);
                    
                    if (Files.notExists(compressed.getParent())) {
                        Files.createDirectories(compressed.getParent());
                    }

                    Files.write(compressed, text.getBytes(PreCompileCommand.this.encoding));
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to create compressed file.", e);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void copyErrorClassFilesFromOrgToOut(Set<Path> errorSourceFiles) {
        Path orgClassesDir = this.orgClassesDir.orElseThrow(() -> new IllegalStateException("-c options is not set. please set classes directory path."));

        errorSourceFiles.forEach(javaFile -> {
            String classFileName = javaFile.getFileName().toString().replace(".java", ".class");
            Path classFile = javaFile.getParent().resolve(classFileName);

            Path orgClassFile = orgClassesDir.resolve(classFile);
            Path outClassFile = this.outSrcDir.resolve(classFile);
            try {
                Files.copy(orgClassFile, outClassFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                throw new UncheckedIOException("failed to copy file.", e);
            }
        });
    }

    private void removeErrorJavaFileFromOut(Set<Path> errorSourceFiles) {
        errorSourceFiles.forEach(javaFile -> {
            Path outJavaFile = this.outSrcDir.resolve(javaFile);
            try {
                Files.delete(outJavaFile);
            } catch (IOException e) {
                throw new UncheckedIOException("failed to remove file.", e);
            }
        });
    }

    private CompileResult compile() throws IOException {
        this.recreateWorkDir();
        
        List<String> sourceFiles = this.collectSourceFiles();
        String[] args = this.buildJavacArgs(sourceFiles);

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler()
                                .run(null, null, error, args);

        return new CompileResult(resultCode != 0, error);
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
    
    private void createWorkDir() {
        try {
            Files.createDirectories(this.workDir);
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create work directory.", e);
        }
    }

    private List<String> collectSourceFiles() throws IOException {
        try (Stream<Path> s = Files.walk(this.outSrcDir)) {
            return s.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }

    private String[] buildJavacArgs(List<String> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.outSrcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add(this.encoding.toString());
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }

    private void copyOrgToOut() throws IOException {
        if (Files.notExists(this.outSrcDir)) {
            System.out.println("copying source files...");
            FileUtil.copyFileTree(this.orgSrcDir, this.outSrcDir, ".java");
        }
    }


    private static class CompileResult {
        private final boolean error;
        private final ByteArrayOutputStream errorStream;

        private CompileResult(boolean error, ByteArrayOutputStream errorStream) {
            this.error = error;
            this.errorStream = errorStream;
        }

        private Set<Path> getErrorSourceFiles(Path outSrcDir) throws IOException {
            System.out.println("extracting error source files...");
            
            String errorMessage = this.errorStream.toString(Charset.defaultCharset().toString());
            Pattern pattern = Pattern.compile("^([^ \\r\\n]+\\.java):\\d+:", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(errorMessage);
            Set<String> errorSrcPathSet = new HashSet<>();

            while (matcher.find()) {
                String sourcePath = matcher.group(1);
                errorSrcPathSet.add(sourcePath);
            }

            return errorSrcPathSet.stream()
                    .map(Paths::get)
                    .map(outSrcDir.toAbsolutePath()::relativize)
                    .sorted()
                    .collect(toSet());
        }
    }
}
