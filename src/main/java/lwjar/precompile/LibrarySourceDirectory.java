package lwjar.precompile;

import lwjar.FileUtil;
import lwjar.GlobalOption;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class LibrarySourceDirectory {
    
    private final Path dir;

    public LibrarySourceDirectory(Path dir) {
        this.dir = Objects.requireNonNull(dir);
    }

    public void copyTo(Path toDir, JavaSourceCompressor javaSourceCompressor) {
        try {
            FileUtil.copyFileTree(this.dir, toDir, (inPath, outPath) -> {
                if (this.isJavaSource(inPath)) {
                    String text = javaSourceCompressor.compress(inPath);
                    Files.write(outPath, text.getBytes(GlobalOption.getEncoding()), StandardOpenOption.CREATE);
                } else if (!inPath.getFileName().toString().equals("MANIFEST.MF")) {
                    Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);
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
