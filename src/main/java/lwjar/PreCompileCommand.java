package lwjar;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
                    Files.copy(javaFile, copyTo);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy source file.", e);
                }
            });
    }

    private void copyClassesFiles() throws IOException {
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
                    Files.copy(classFile, copyTo);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy class file.", e);
                }
            });
    }
}
