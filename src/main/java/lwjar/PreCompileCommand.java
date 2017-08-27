package lwjar;

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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PreCompileCommand implements Command {

    private final Path orgSrcDir;
    private final Optional<Path> orgClassesDir;

    private final Path outDir;
    private final Path outSrcDir;
    private final Path outClassesDir;

    private final Path workDir;
    private final Path workSrcDir;
    private final Path workClassesDir;

    public PreCompileCommand(Path orgSrcDir, Path orgClassesDir, Path outDir) {
        this.orgSrcDir = orgSrcDir;
        this.orgClassesDir = Optional.ofNullable(orgClassesDir);

        this.outDir = outDir == null ? Paths.get("./out") : outDir;
        this.outSrcDir = this.outDir.resolve("src");
        this.outClassesDir = this.outDir.resolve("classes");

        this.workDir = this.outDir.resolve("work");
        this.workSrcDir = this.workDir.resolve("src");
        this.workClassesDir = this.workDir.resolve("classes");
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();

        int cnt = 0;
        CompileResult result = this.compile();

        while (result.error) {
            Set<Path> errorSourceFiles = result.getErrorSourceFiles(this.workSrcDir);
            this.copyErrorClassFilesFromOrgToOut(errorSourceFiles);
            this.removeErrorJavaFileFromOut(errorSourceFiles);
            System.out.println("remove error source files...");
            errorSourceFiles.forEach(System.out::println);

            cnt++;
            if (100 < cnt) {
                break;
            }

            result = this.compile();
        }
    }

    private void copyErrorClassFilesFromOrgToOut(Set<Path> errorSourceFiles) {
        Path orgClassesDir = this.orgClassesDir.orElseThrow(() -> new IllegalStateException("-c options is not set. please set classes directory path."));

        errorSourceFiles.forEach(javaFile -> {
            String classFileName = javaFile.getFileName().toString().replace(".java", ".class");
            Path classFile = javaFile.getParent().resolve(classFileName);

            Path orgClassFile = orgClassesDir.resolve(classFile);
            Path outClassFile = this.outClassesDir.resolve(classFile);
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

    private void removeWorkDir() throws IOException {
        if (Files.notExists(this.workDir)) {
            return;
        }

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
    }

    private CompileResult compile() throws IOException {
        this.removeWorkDir();
        this.copyOutToWork();

        List<String> sourceFiles = this.collectSourceFiles();
        String[] args = this.buildJavacArgs(sourceFiles);

        ByteArrayOutputStream error = new ByteArrayOutputStream();
        int resultCode = ToolProvider.getSystemJavaCompiler()
                                .run(null, null, error, args);

        return new CompileResult(resultCode != 0, error);
    }

    private void copyOutToWork() throws IOException {
        this.copyFileTree(this.outSrcDir, this.workSrcDir, ".java");
        this.copyFileTree(this.outClassesDir, this.workClassesDir, ".class");
    }

    private List<String> collectSourceFiles() throws IOException {
        return Files.walk(this.workSrcDir)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(file -> file.toAbsolutePath().toString())
                .collect(toList());
    }

    private String[] buildJavacArgs(List<String> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workClassesDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.workSrcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add("UTF-8");
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }

    private void copyOrgToOut() throws IOException {
        this.copyFileTree(this.orgSrcDir, this.outSrcDir, ".java");

        Files.createDirectories(this.outClassesDir);

        if (this.orgClassesDir.isPresent()) {
            this.copyFileTree(this.orgClassesDir.get(), this.outClassesDir, ".class");
        }
    }

    private void copyFileTree(Path from, Path to, String endsWith) throws IOException {
        Files.createDirectories(to);

        Files.walk(from)
            .filter(path -> path.getFileName().toString().endsWith(endsWith))
            .forEach(classFile -> {
                Path relativePath = from.relativize(classFile);
                Path copyTo = to.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    Files.copy(classFile, copyTo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy file.", e);
                }
            });
    }

    private static class CompileResult {
        private final boolean error;
        private final ByteArrayOutputStream errorStream;

        private CompileResult(boolean error, ByteArrayOutputStream errorStream) {
            this.error = error;
            this.errorStream = errorStream;
        }

        private Set<Path> getErrorSourceFiles(Path workSrcDir) throws IOException {
            String errorMessage = this.errorStream.toString(Charset.defaultCharset().toString());
            Pattern pattern = Pattern.compile("([^ \\r\\n]+\\.java)");
            Matcher matcher = pattern.matcher(errorMessage);
            Set<String> errorSrcPathSet = new HashSet<>();

            while (matcher.find()) {
                String sourcePath = matcher.group();
                errorSrcPathSet.add(sourcePath);
            }

            return errorSrcPathSet.stream()
                    .map(Paths::get)
                    .map(workSrcDir.toAbsolutePath()::relativize)
                    .sorted()
                    .collect(toSet());
        }
    }
}
