package lwjar.packaging;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.LightweightJarExecutor;
import lwjar.Main;
import lwjar.OutputDirectory;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class PackageCommand implements Command {
    private static final String EXECUTOR_CLASS_NAME = LightweightJarExecutor.class.getSimpleName() + ".class";
    
    private final OutputDirectory outputDirectory;
    private final SourceDirectory sourceDirectory;
    private final JarWorkDirectory jarWorkDirectory;
    
    private final String jarBase;
    private final boolean springBoot;
    private final String mainClass;
    

    public PackageCommand(String encoding, String jarBase, Path srcDir, boolean springBoot, String mainClass, Path outDir) {
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        this.outputDirectory = new OutputDirectory(outDir);
        this.sourceDirectory = new SourceDirectory(new Directory(srcDir));
        this.jarWorkDirectory = new JarWorkDirectory(this.outputDirectory);
        this.jarBase = jarBase;
        this.springBoot = springBoot;
        this.mainClass = mainClass;
    }

    @Override
    public void execute() throws IOException {
        this.extractExecutorClassFile();
        this.sourceDirectory.copyTo(this.jarWorkDirectory);
        this.createJarFile();
    }
    
    private void extractExecutorClassFile() throws IOException {
        JarEntry mainClass = this.findThisJarFile().stream()
                .filter(this::isMainClass)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("not found executor main class. > " + EXECUTOR_CLASS_NAME));

        try (InputStream in = PackageCommand.class.getResourceAsStream("/" + mainClass.getName())) {
            ProcessingFile workMainClass = this.jarWorkDirectory.resolveFile(mainClass.getName());
            workMainClass.write(in);
        }
    }
    
    private void createJarFile() throws IOException {
        ProcessingFile jarFile = this.outputDirectory.resolveFile(new RelativePath(this.jarBase + ".jar"));
        
        try (LightweightJarFile jar = new LightweightJarFile(jarFile, this.createManifest())) {
            this.jarWorkDirectory.walkTree(new JarWorkDirectory.JarWorkDirectoryVisitor() {

                @Override
                public void visit(ProcessingFile file, RelativePath relativePath) {
                    jar.put(file, relativePath);
                }

                @Override
                public void visit(Directory directory, RelativePath relativePath) {
                    jar.put(directory, relativePath);
                }
            });
        }
    }

    private Manifest createManifest() {
        Manifest manifest = new Manifest();
        
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, LightweightJarExecutor.class.getName());
        
        manifest.getMainAttributes().put(new Attributes.Name("Actual-Main-Class"), this.mainClass);
        manifest.getMainAttributes().put(new Attributes.Name("Is-Spring-Boot"), String.valueOf(this.springBoot));
        manifest.getMainAttributes().put(new Attributes.Name("Javac-Encoding"), GlobalOption.getEncoding().name());
        
        return manifest;
    }
    
    private boolean isMainClass(JarEntry entry) {
        return entry.getName().endsWith(EXECUTOR_CLASS_NAME);
    }

    private JarFile findThisJarFile() throws IOException {
        URL location = Main.class.getProtectionDomain().getCodeSource().getLocation();
        return new JarFile(location.getFile());
    }
}
