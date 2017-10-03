package lwjar.packaging;

import lwjar.Command;
import lwjar.FileUtil;
import lwjar.LightweightJarExecutor;
import lwjar.Main;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class PackageCommand implements Command {
    private static final String SOURCE_RESOURCE_PATH = "src";
    private static final String EXECUTOR_CLASS_NAME = LightweightJarExecutor.class.getSimpleName() + ".class";
    private final Charset encoding;
    private final String jarBase;
    private final Path srcDir;
    private final boolean springBoot;
    private final String mainClass;
    
    private final Path outDir;

    public PackageCommand(String encoding, String jarBase, Path srcDir, boolean springBoot, String mainClass, Path outDir) {
        this.encoding = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        this.jarBase = jarBase;
        this.srcDir = srcDir;
        this.springBoot = springBoot;
        this.mainClass = mainClass;
        this.outDir = outDir == null ? Paths.get("./out") : outDir;
    }

    @Override
    public void execute() throws IOException {
        Path workDir = this.initWorkDir();
        this.extractExecutorClassFile(workDir);
        this.copySourceFiles(workDir);
        this.createJarFile(workDir);
    }
    
    private Path initWorkDir() throws IOException {
        Path workDir = this.outDir.resolve(this.jarBase);

        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir);
        }
        
        return workDir;
    }
    
    private void extractExecutorClassFile(Path workDir) throws IOException {
        JarEntry mainClass = this.findThisJarFile().stream()
                .filter(this::isMainClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("not found executor main class. > " + EXECUTOR_CLASS_NAME));

        try (InputStream in = PackageCommand.class.getResourceAsStream("/" + mainClass.getName())) {
            Path workMainClass = workDir.resolve(mainClass.getName());
            Files.createDirectories(workMainClass.getParent());
            Files.copy(in, workMainClass, StandardCopyOption.REPLACE_EXISTING);
        }
    }
    
    private void copySourceFiles(Path workDir) throws IOException {
        FileUtil.copyFileTree(this.srcDir, workDir);
    }
    
    private void createJarFile(Path workDir) throws IOException {

        try (
            FileOutputStream file = new FileOutputStream(this.outDir.resolve(this.jarBase + ".jar").toFile());
            JarOutputStream jar = new JarOutputStream(file, this.createManifest());
        ) {
            
            Files.walkFileTree(workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = workDir.toAbsolutePath().relativize(dir.toAbsolutePath());
                    String path = SOURCE_RESOURCE_PATH + "/" + relativePath.toString().replace("\\", "/");
                    if (!path.endsWith("/")) {
                        path = path + "/";
                    }
                    JarEntry entry = new JarEntry(path);
                    jar.putNextEntry(entry);
                    jar.closeEntry();

                    return FileVisitResult.CONTINUE;
                }
                
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Path relativePath = workDir.toAbsolutePath().relativize(file.toAbsolutePath());
                    String path = relativePath.toString().replace("\\", "/");
                    if (!isMainClass(file)) {
                        path = SOURCE_RESOURCE_PATH + "/" + path;
                    }
                    JarEntry entry = new JarEntry(path);
                    jar.putNextEntry(entry);

                    try (InputStream in = new BufferedInputStream(Files.newInputStream(file, StandardOpenOption.READ))) {
                        byte[] buf = new byte[1024];
                        int length;
                        while ((length = in.read(buf)) != -1) {
                            jar.write(buf, 0, length);
                        }
                    } finally {
                        jar.closeEntry();
                    }

                    return FileVisitResult.CONTINUE;
                }
            });

            jar.closeEntry();
        }
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, LightweightJarExecutor.class.getName());
        
        manifest.getMainAttributes().put(new Attributes.Name("Actual-Main-Class"), this.mainClass);
        manifest.getMainAttributes().put(new Attributes.Name("Is-Spring-Boot"), String.valueOf(this.springBoot));
        manifest.getMainAttributes().put(new Attributes.Name("Javac-Encoding"), this.encoding.name());
        
        return manifest;
    }
    
    private boolean isMainClass(JarEntry entry) {
        return entry.getName().endsWith(EXECUTOR_CLASS_NAME);
    }
    
    private boolean isMainClass(Path file) {
        return file.getFileName().toString().equals(EXECUTOR_CLASS_NAME);
    }

    private JarFile findThisJarFile() throws IOException {
        URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
        return new JarFile(location.getFile());
    }
}
