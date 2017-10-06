package lwjar.packaging;

import lwjar.OutputDirectory;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

class JarWorkDirectory {
    private final Directory directory;

    JarWorkDirectory(OutputDirectory outputDirectory) {
        this.directory = outputDirectory.resolveDirectory("jar-work");
    }

    Directory getDirectory() {
        return this.directory;
    }

    ProcessingFile resolveFile(String path) {
        return this.directory.resolve(new RelativePath(path));
    }

    void walkTree(JarWorkDirectoryVisitor visitor) {
        try {
            Files.walkFileTree(this.directory.getPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    ProcessingFile processingFile = new ProcessingFile(file);
                    RelativePath relativePath = JarWorkDirectory.this.directory.relativePath(processingFile);
                    visitor.visit(processingFile, relativePath);
                    
                    return super.visitFile(file, attrs);
                }

                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    ProcessingFile asFile = new ProcessingFile(dir);
                    RelativePath relativePath = JarWorkDirectory.this.directory.relativePath(asFile);
                    visitor.visitDirectory(relativePath);

                    return super.preVisitDirectory(dir, attrs);
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    public interface JarWorkDirectoryVisitor {
        void visit(ProcessingFile file, RelativePath relativePath) throws IOException;
        void visitDirectory(RelativePath relativePath) throws IOException;
    }
}
