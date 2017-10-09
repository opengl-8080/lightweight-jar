package lwjar.packaging;

import lwjar.cli.Command;
import lwjar.GlobalOption;
import lwjar.OutputDirectory;
import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.util.Objects;

public class PackageCommand implements Command {
    
    private final SourceDirectory sourceDirectory;
    private final JarWorkDirectory jarWorkDirectory;
    
    private final LightweightJarFile lightweightJarFile;
    

    public PackageCommand(
        SourceDirectory sourceDirectory,
        ApplicationMainClass applicationMainClass,
        OutputDirectory outputDirectory,
        JarName jarName,
        boolean springBoot
    ) {
        this.sourceDirectory = Objects.requireNonNull(sourceDirectory);
        this.jarWorkDirectory = new JarWorkDirectory(outputDirectory);
        
        ManifestFile manifestFile = new ManifestFile(applicationMainClass, springBoot, GlobalOption.getEncoding());
        
        ProcessingFile jarFile = outputDirectory.resolveFile(jarName.toRelativePath());
        this.lightweightJarFile = new LightweightJarFile(jarFile, manifestFile);
    }

    @Override
    public void execute() throws IOException {
        new ThisJarFile().extractExecutorClassFileTo(this.jarWorkDirectory);
        this.sourceDirectory.copyTo(this.jarWorkDirectory);
        this.lightweightJarFile.buildFrom(this.jarWorkDirectory);
    }
}
