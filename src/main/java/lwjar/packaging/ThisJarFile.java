package lwjar.packaging;

import lwjar.LightweightJarExecutor;
import lwjar.Main;
import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class ThisJarFile {
    private static final String EXECUTOR_CLASS_NAME = LightweightJarExecutor.class.getSimpleName() + ".class";
    
    void extractExecutorClassFileTo(JarWorkDirectory jarWorkDirectory) {
        try {
            JarEntry executorClass = this.findThisJarFile().stream()
                    .filter(this::isExecutorClass)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("not found executor executor class. > " + EXECUTOR_CLASS_NAME));

            try (InputStream in = PackageCommand.class.getResourceAsStream("/" + executorClass.getName())) {
                ProcessingFile executorClassFile = jarWorkDirectory.resolveFile(executorClass.getName());
                executorClassFile.write(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private JarFile findThisJarFile() throws IOException {
        URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
        return new JarFile(location.getFile());
    }

    private boolean isExecutorClass(JarEntry entry) {
        return entry.getName().endsWith(EXECUTOR_CLASS_NAME);
    }
}
