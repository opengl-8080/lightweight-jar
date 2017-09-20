package lwjar.precompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public class ProcessingFile {
    private final Path file;

    public ProcessingFile(Path file) {
        this.file = Objects.requireNonNull(file);
    }
    
    void copyTo(ProcessingFile to) {
        try {
            to.createParentDirectoriesIfNotExists();
            Files.copy(this.file, to.file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    void write(String text, Charset charset) {
        try {
            this.createParentDirectoriesIfNotExists();
            Files.write(this.file, text.getBytes(charset), StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private void createParentDirectoriesIfNotExists() throws IOException {
        if (Files.exists(this.parentDir())) {
            return;
        }
        Files.createDirectories(this.parentDir());
    }
    
    private Path parentDir() {
        return this.file.getParent();
    }
    
    boolean isJavaSource() {
        return this.name().endsWith(".java")
                && !this.name().equals("package-info.java");
    }
    
    boolean isManifestFile() {
        return this.name().equals("MANIFEST.MF");
    }
    
    private String name() {
        return this.file.getFileName().toString();
    }
    
    Path path() {
        return this.file;
    }
}
