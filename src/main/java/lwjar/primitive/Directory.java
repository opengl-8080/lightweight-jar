package lwjar.primitive;

import lwjar.precompile.FileVisitor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class Directory {
    private final Path dir;

    public Directory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
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
        Path path = file.getPath();
        Path dir = this.dir;
        
        if (dir.isAbsolute() && !path.isAbsolute()) {
            path = path.toAbsolutePath();
        } else if (!dir.isAbsolute() && path.isAbsolute()) {
            dir = dir.toAbsolutePath();
        }
        
        return new RelativePath(dir.relativize(path));
    }

    public Directory resolveDirectory(RelativePath relativePath) {
        return new Directory(this.dir.resolve(relativePath.getPath()));
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

    public ProcessingFile resolve(RelativePath relativePath) {
        return new ProcessingFile(this.dir.resolve(relativePath.getPath()));
    }
    
    public boolean exists() {
        return Files.exists(this.dir);
    }
    
    public String getStringPath() {
        return this.dir.toString();
    }

    public List<ProcessingFile> collectFiles(Predicate<ProcessingFile> predicate) {
        try (Stream<Path> s = Files.walk(this.dir)) {
            return s.map(ProcessingFile::new)
                    .filter(predicate)
                    .collect(toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void create() {
        try {
            Files.createDirectories(this.dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void remove() {
        try {
            Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (Files.exists(file)) {
                        Files.delete(file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (Files.exists(dir)) {
                        Files.delete(dir);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("failed remove work directory.", e);
        }
    }
}
