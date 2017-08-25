package lwjar;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class PreCompileCommand implements Command {

    private final Path sourceFileDir;
    private final Path classesFileDir;
    private final Path outputDir;
    private final Path workSrcDir;
    private final Path workClassesDir;

    public PreCompileCommand(Path sourceFileDir, Path classesFileDir, Path outputDir) {
        this.sourceFileDir = sourceFileDir;
        this.classesFileDir = classesFileDir;
        this.outputDir = outputDir;
        this.workSrcDir = this.outputDir.resolve("src");
        this.workClassesDir = this.outputDir.resolve("classes");
    }

    @Override
    public void execute() throws IOException {
        this.copySourceFiles();
        this.copyClassesFiles();
        this.compile();
    }

    private void compile() throws IOException {
        List<String> sourceFiles = Files.walk(this.workSrcDir)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(file -> file.toAbsolutePath().toString())
                .collect(toList());

        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workClassesDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.workSrcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add("UTF-8");
        javacOptions.addAll(sourceFiles);

        String[] args = javacOptions.toArray(new String[javacOptions.size()]);
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        compiler.run(null, null, null, args);
    }

    private void copySourceFiles() throws IOException {
        Files.createDirectories(this.workSrcDir);

        Files.walk(this.sourceFileDir)
            .filter(path -> path.getFileName().toString().endsWith(".java"))
            .forEach(javaFile -> {
                Path relativePath = this.sourceFileDir.relativize(javaFile);
                Path copyTo = this.workSrcDir.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    Files.copy(javaFile, copyTo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy source file.", e);
                }
            });
    }

    private void copyClassesFiles() throws IOException {
        if (Files.notExists(this.workClassesDir)) {
            Files.createDirectories(this.workClassesDir);
        }

        if (this.classesFileDir == null) {
            return;
        }

        Files.walk(this.classesFileDir)
            .forEach(classFile -> {
                Path relativePath = this.classesFileDir.relativize(classFile);
                Path copyTo = this.workClassesDir.resolve(relativePath);

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
                    throw new UncheckedIOException("failed to copy class file.", e);
                }
            });
    }
}
