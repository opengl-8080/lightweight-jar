package lwjar.build;

import lwjar.cli.Command;
import lwjar.OutputDirectory;
import lwjar.packaging.ApplicationMainClass;
import lwjar.packaging.JarName;
import lwjar.packaging.PackageCommand;
import lwjar.packaging.SourceDirectory;
import lwjar.precompile.ApplicationSourceDirectory;
import lwjar.precompile.JavaSourceCompressor;
import lwjar.precompile.LibraryClassDirectory;
import lwjar.precompile.LibrarySourceDirectory;
import lwjar.precompile.PreCompileCommand;
import lwjar.primitive.Directory;

import java.io.IOException;

public class BuildCommand implements Command {
    
    private final PreCompileCommand preCompileCommand;
    private final PackageCommand packageCommand;
    
    public BuildCommand(
        LibrarySourceDirectory librarySourceDirectory,
        LibraryClassDirectory libraryClassDirectory,
        ApplicationSourceDirectory applicationSourceDirectory,
        ApplicationMainClass applicationMainClass,
        JarName jarName,
        OutputDirectory outputDirectory,
        JavaSourceCompressor.CompressLevel compressLevel,
        Integer retryCount,
        boolean springBoot
    ) {
        this.preCompileCommand = new PreCompileCommand(
            librarySourceDirectory,
            libraryClassDirectory,
            applicationSourceDirectory,
            outputDirectory,
            compressLevel,
            retryCount
        );

        Directory preCompiledDirectory = this.preCompileCommand.getPreCompiledDirectory();
        SourceDirectory sourceDirectory = new SourceDirectory(preCompiledDirectory);

        this.packageCommand = new PackageCommand(
            sourceDirectory,
            applicationMainClass,
            outputDirectory,
            jarName,
            springBoot
        );
    }
    
    @Override
    public void execute() throws IOException {
        System.out.println("executing pre-compile command...");
        this.preCompileCommand.execute();
        
        System.out.println("executing package command...");
        this.packageCommand.execute();
    }
}
