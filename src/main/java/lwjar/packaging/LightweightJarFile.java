package lwjar.packaging;

import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

class LightweightJarFile {
    private static final String SOURCE_RESOURCE_PATH = "src";
    
    private final ProcessingFile file;
    private final ManifestFile manifestFile;

    LightweightJarFile(ProcessingFile file, ManifestFile manifestFile) {
        this.file = Objects.requireNonNull(file);
        this.manifestFile = Objects.requireNonNull(manifestFile);
    }

    void buildFrom(JarWorkDirectory jarWorkDirectory) {
        try (JarOutputStream jar = new JarOutputStream(this.file.getOutputStream(), this.manifestFile.toManifest())) {
            jarWorkDirectory.walkTree(new JarBuilder(jar));
            jar.closeEntry();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    
    private static class JarBuilder implements JarWorkDirectory.JarWorkDirectoryVisitor {
        private final JarOutputStream jar;

        private JarBuilder(JarOutputStream jar) {
            this.jar = jar;
        }

        @Override
        public void visit(ProcessingFile file, RelativePath relativePath) throws IOException {
            String path = relativePath.toSlashPathString();
            if (!file.isExecutorClassFile()) {
                path = SOURCE_RESOURCE_PATH + "/" + path;
            }
            JarEntry entry = new JarEntry(path);
            this.jar.putNextEntry(entry);

            try (InputStream in = file.getInputStream()) {
                byte[] buf = new byte[1024];
                int length;
                while ((length = in.read(buf)) != -1) {
                    this.jar.write(buf, 0, length);
                }
            } finally {
                this.jar.closeEntry();
            }
        }

        @Override
        public void visitDirectory(RelativePath relativePath) throws IOException {
            String path = SOURCE_RESOURCE_PATH + "/" + relativePath.toSlashPathString();
            if (!path.endsWith("/")) {
                path = path + "/";
            }
            JarEntry entry = new JarEntry(path);
            this.jar.putNextEntry(entry);
            this.jar.closeEntry();
        }
    }
}
