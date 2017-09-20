package lwjar.precompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class Directory {
    private final Path dir;

    public Directory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
        if (!Files.isDirectory(dir)) {
            throw new IllegalArgumentException("dir is not directory.");
        }
    }
    
    public void walkFiles(FileVisitor visitor) {
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

    public RelativePath relativePath(ProcessingFile file) {
        return new RelativePath(this.dir.relativize(file.path()));
    }
}
