package lwjar.packaging;

import lwjar.LightweightJarExecutor;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

class LightweightJarFile implements AutoCloseable {
    private static final String SOURCE_RESOURCE_PATH = "src";
    private static final String EXECUTOR_CLASS_NAME = LightweightJarExecutor.class.getSimpleName() + ".class";
    
    private final ProcessingFile file;
    private final JarOutputStream jar;

    LightweightJarFile(ProcessingFile file, Manifest manifest) {
        this.file = Objects.requireNonNull(file);
        try {
            this.jar = new JarOutputStream(file.getOutputStream(), manifest);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void close() {
        try {
            this.jar.closeEntry();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
        try {
            this.jar.close();
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }

    void put(ProcessingFile file, RelativePath relativePath) {
        try {
            String path = relativePath.getPath().toString().replace("\\", "/");
            if (!isMainClass(file.getPath())) {
                path = SOURCE_RESOURCE_PATH + "/" + path;
            }
            JarEntry entry = new JarEntry(path);
            jar.putNextEntry(entry);

            try (InputStream in = file.getInputStream()) {
                byte[] buf = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    jar.write(buf, 0, length);
                }
            } finally {
                jar.closeEntry();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private boolean isMainClass(Path file) {
        return file.getFileName().toString().equals(EXECUTOR_CLASS_NAME);
    }

    void put(Directory directory, RelativePath relativePath) {
        String path = SOURCE_RESOURCE_PATH + "/" + relativePath.getPath().toString().replace("\\", "/");
        if (!path.endsWith("/")) {
            path = path + "/";
        }
        JarEntry entry = new JarEntry(path);
        try {
            jar.putNextEntry(entry);
            jar.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
