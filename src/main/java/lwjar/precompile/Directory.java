package lwjar.precompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.stream.Stream;

public class Directory {
    private final Path dir;

    Directory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("dir is not directory.");
        }
    }
    
    void walkFiles(FileVisitor visitor) {
        try {
            Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    visitor.visit(new ProcessingFile(file));
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    RelativePath relativePath(ProcessingFile file) {
        return new RelativePath(this.dir.relativize(file.path()));
    }

    public Directory resolveDirectory(RelativePath relativePath) {
        return new Directory(this.dir.resolve(relativePath.path()));
    }

    public ProcessingFile findFileStartsWith(String fileNamePrefix) {
        try (Stream<Path> stream = Files.list(this.dir)) {
            Path file = stream.filter(path -> path.getFileName().toString().startsWith(fileNamePrefix))
                            .findFirst()
                            .orElseThrow(() -> new IllegalStateException(fileNamePrefix + "* is not exists."));
            
            return new ProcessingFile(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
