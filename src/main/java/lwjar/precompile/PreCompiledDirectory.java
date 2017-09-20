package lwjar.precompile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class PreCompiledDirectory {
    private final Path dir;

    PreCompiledDirectory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
    }
    
    boolean has(Path relativePath) {
        return Files.exists(this.resolve(relativePath));
    }

    ProcessingFile resolve(RelativePath relativePath) {
        return new ProcessingFile(this.dir.resolve(relativePath.path()));
    }
    
    Path resolve(Path relativePath) {
        return this.dir.resolve(relativePath);
    }
    
    boolean exists() {
        return Files.exists(this.dir);
    }

    Path relativize(Path path) {
        return this.dir.toAbsolutePath().relativize(path);
    }

    List<String> collectSourceFiles() throws IOException {
        try (Stream<Path> s = Files.walk(this.dir)) {
            return s.filter(this::isJavaSource)
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }

    private boolean isJavaSource(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }

    String stringPath() {
        return this.dir.toString();
    }
}
