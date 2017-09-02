package lwjar;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;

public class FileUtil {
    private static final String ALL_SUFFIX = "*";
    
    public static void copyFileTree(Path from, Path to, String suffix) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!ALL_SUFFIX.equals(suffix)) {
                    if (!file.getFileName().toString().endsWith(suffix)) {
                        return FileVisitResult.CONTINUE;
                    }
                }

                Path relativePath = from.relativize(file);
                Path copyTo = to.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    Files.copy(file, copyTo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy file.", e);
                }
                
                return FileVisitResult.CONTINUE;
            }
        });
    }

    public static void copyFileTree(Path from, Path to, FileExporter converter) throws IOException {
        Files.walkFileTree(from, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relativePath = from.relativize(file);
                Path copyTo = to.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    converter.output(file, copyTo);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy file.", e);
                }

                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    public interface FileExporter {
        void output(Path inPath, Path outPath) throws IOException;
    }

    public static void copyFileTree(Path from, Path to) throws IOException {
        copyFileTree(from, to, ALL_SUFFIX);
    }
    
    private FileUtil() {}
}
