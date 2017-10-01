package lwjar.primitive;

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

    public ProcessingFile replaceFileName(String name) {
        Path parent = this.getParentDir();
        return new ProcessingFile(parent.resolve(name));
    }

    public void delete() {
        try {
            Files.delete(this.file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void copyTo(ProcessingFile to) {
        try {
            to.createParentDirectoriesIfNotExists();
            Files.copy(this.file, to.file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void write(String text, Charset charset) {
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
    
    public boolean isJavaSource() {
        return this.getName().endsWith(".java")
                && !this.getName().equals("package-info.java");
    }

    public boolean isManifestFile() {
        return this.getName().equals("MANIFEST.MF");
    }
    
    public boolean isPackageInfo() {
        return this.getName().equals("package.html")
                || this.getName().equals("package-info.java");
    }

    public String getName() {
        return this.file.getFileName().toString();
    }
    
    public Path getPath() {
        return this.file;
    }

    public boolean isClassFile() {
        return this.getName().endsWith(".class");
    }

    public boolean exists() {
        return Files.exists(this.file);
    }

    public String getAbsolutePathString() {
        return this.file.toAbsolutePath().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProcessingFile that = (ProcessingFile) o;

        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        return file.hashCode();
    }

    public String getContent(Charset encoding) {
        try {
            return new String(Files.readAllBytes(this.file), encoding);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
