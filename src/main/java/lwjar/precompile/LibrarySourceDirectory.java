package lwjar.precompile;

import lwjar.GlobalOption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public class LibrarySourceDirectory {
    
    private final Path dir;

    public LibrarySourceDirectory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
    }

    public void copyTo(PreCompiledDirectory preCompiledDirectory, JavaSourceCompressor javaSourceCompressor) {
        try {
            Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = dir.relativize(file);
                    Path copyTo = preCompiledDirectory.resolve(relativePath);

                    if (Files.notExists(copyTo.getParent())) {
                        try {
                            Files.createDirectories(copyTo.getParent());
                        } catch (IOException e) {
                            throw new UncheckedIOException("failed to create output directory.", e);
                        }
                    }

                    try {
                        if (isJavaSource(file)) {
                            String text = javaSourceCompressor.compress(file);
                            Files.write(copyTo, text.getBytes(GlobalOption.getEncoding()), StandardOpenOption.CREATE);
                        } else if (!file.getFileName().toString().equals("MANIFEST.MF")) {
                            Files.copy(file, copyTo, StandardCopyOption.REPLACE_EXISTING);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to copy file.", e);
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isJavaSource(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }
}
