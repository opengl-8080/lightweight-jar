package lwjar.packaging;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.LightweightJarExecutor;
import lwjar.OutputDirectory;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

public class PackageCommand implements Command {
    
    private final OutputDirectory outputDirectory;
    private final SourceDirectory sourceDirectory;
    private final JarWorkDirectory jarWorkDirectory;
    
    private final JarName jarName;
    private final ApplicationMainClass applicationMainClass;
    private final boolean springBoot;
    

    public PackageCommand(String encoding, String jarName, Path srcDir, boolean springBoot, String applicationMainClass, Path outDir) {
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        this.outputDirectory = new OutputDirectory(outDir);
        this.sourceDirectory = new SourceDirectory(new Directory(srcDir));
        this.jarWorkDirectory = new JarWorkDirectory(this.outputDirectory);
        this.jarName = new JarName(jarName);
        this.applicationMainClass = new ApplicationMainClass(applicationMainClass);
        this.springBoot = springBoot;
    }

    @Override
    public void execute() throws IOException {
        new ThisJarFile().extractExecutorClassFileTo(this.jarWorkDirectory);
        this.sourceDirectory.copyTo(this.jarWorkDirectory);
        this.createJarFile();
    }
    
    private void createJarFile() throws IOException {
        ProcessingFile jarFile = this.outputDirectory.resolveFile(this.jarName.toRelativePath());
        
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
        
        manifest.getMainAttributes().put(new Attributes.Name("Actual-Main-Class"), this.applicationMainClass.getName());
        manifest.getMainAttributes().put(new Attributes.Name("Is-Spring-Boot"), String.valueOf(this.springBoot));
        manifest.getMainAttributes().put(new Attributes.Name("Javac-Encoding"), GlobalOption.getEncoding().name());
        
        return manifest;
    }
}
