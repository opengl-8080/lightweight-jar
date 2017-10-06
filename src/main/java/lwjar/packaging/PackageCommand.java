package lwjar.packaging;

import lwjar.Command;
import lwjar.GlobalOption;
import lwjar.OutputDirectory;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public class PackageCommand implements Command {
    
    private final SourceDirectory sourceDirectory;
    private final JarWorkDirectory jarWorkDirectory;
    
    private final LightweightJarFile lightweightJarFile;
    

    public PackageCommand(String encoding, String strJarName, Path srcDir, boolean springBoot, String applicationMainClass, Path outDir) {
        GlobalOption.setEncoding(encoding == null ? Charset.defaultCharset() : Charset.forName(encoding));
        
        OutputDirectory outputDirectory = new OutputDirectory(outDir);
        this.sourceDirectory = new SourceDirectory(new Directory(srcDir));
        this.jarWorkDirectory = new JarWorkDirectory(outputDirectory);
        
        JarName jarName = new JarName(strJarName);
        ManifestFile manifestFile = new ManifestFile(new ApplicationMainClass(applicationMainClass), springBoot, GlobalOption.getEncoding());
        
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
