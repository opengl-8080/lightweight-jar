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
    
    ProcessingFile replaceFileName(String name) {
        Path parent = this.getParentDir();
        return new ProcessingFile(parent.resolve(name));
    }
    
    void delete() {
        try {
            Files.delete(this.file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
        if (Files.exists(this.getParentDir())) {
            return;
        }
        Files.createDirectories(this.getParentDir());
    }
    
    private Path getParentDir() {
        return this.file.getParent();
    }
    
    boolean isJavaSource() {
        return this.getName().endsWith(".java")
                && !this.getName().equals("package-info.java");
    }
    
    boolean isManifestFile() {
        return this.getName().equals("MANIFEST.MF");
    }
    
    String getName() {
        return this.file.getFileName().toString();
    }
    
    Path getPath() {
        return this.file;
    }

    boolean isClassFile() {
        return this.getName().endsWith(".class");
    }
    
    boolean exists() {
        return Files.exists(this.file);
    }
    
    String getAbsolutePathString() {
        return this.file.toAbsolutePath().toString();
    }
}
