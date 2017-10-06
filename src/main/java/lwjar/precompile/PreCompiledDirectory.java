package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.OutputDirectory;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;

class PreCompiledDirectory {
    private final Directory directory;

    PreCompiledDirectory(OutputDirectory outputDirectory) {
        this.directory = outputDirectory.resolveDirectory("src");
    }
    
    RelativePath relative(ProcessingFile file) {
        return this.directory.relativePath(file);
    }

    CompileTargetJavaSourceFiles collectCompileTargetJavaSourceFiles() {
        return new CompileTargetJavaSourceFiles(this.directory.collectFiles(ProcessingFile::isJavaSource));
    }
    
    String getStringPath() {
        return this.directory.getStringPath();
    }

    void copyJavaSourceFiles(LibrarySourceDirectory librarySourceDirectory, JavaSourceCompressor javaSourceCompressor) {
        if (this.exists()) {
            return;
        }

        System.out.println("copy and compressing source files...");
        librarySourceDirectory.walkFiles((file, relativePath) -> {
            ProcessingFile outFile = this.resolve(relativePath);

            if (file.isJavaSource()) {
                String compressedSource = javaSourceCompressor.compress(file);
                outFile.write(compressedSource, GlobalOption.getEncoding());
            } else if (!file.isManifestFile() && !file.isPackageInfo()) {
                file.copyTo(outFile);
            }
        });
    }

    void copyClassFileThatOnlyExistsInBinaryJar(LibraryClassDirectory libraryClassDirectory) {
        libraryClassDirectory.walkFiles((file, relativeClassFilePath) -> {
            if (!file.isClassFile()) {
                return;
            }

            RelativePath relativeJavaSourcePath = libraryClassDirectory.toRelativeJavaSourcePath(file);
            if (this.has(relativeJavaSourcePath)) {
                // precompile was successful.
                return;
            }

            if (this.has(relativeClassFilePath)) {
                // precompile was failed, but java source exists in source jar.
                return;
            }

            ProcessingFile preCompiledClassFile = this.resolve(relativeClassFilePath);

            // class file exists in original jar file, but source file (or compiled class file) doesn't exist.
            file.copyTo(preCompiledClassFile);
        });
    }

    void copyResourceFilesFrom(LibraryClassDirectory libraryClassDirectory) throws IOException {
        libraryClassDirectory.walkFiles((file, relativePath) -> {
            if (file.isJavaSource() || file.isClassFile() || file.isPackageInfo()) {
                return;
            }

            ProcessingFile outFile = this.resolve(relativePath);

            if (outFile.exists()) {
                return;
            }

            file.copyTo(outFile);
        });
    }

    void copyUncompilableClassFiles(LibraryClassDirectory libraryClassDirectory, UncompilableJavaSources uncompilableJavaSources) {
        libraryClassDirectory.walkOriginalClassFiles(uncompilableJavaSources, (originalClassFile, relativePath) -> {
            ProcessingFile outFile = this.resolve(relativePath);
            originalClassFile.copyTo(outFile);
        });
    }

    void copyApplicationSourceFiles(ApplicationSourceDirectory applicationSourceDirectory, JavaSourceCompressor javaSourceCompressor) {
        applicationSourceDirectory.walkFiles((file, relativePath) -> {
            ProcessingFile outFile = this.resolve(relativePath);

            if (file.isJavaSource()) {
                String compressed = javaSourceCompressor.compress(file);
                outFile.write(compressed, GlobalOption.getEncoding());
            } else {
                file.copyTo(outFile);
            }
        });
    }

    private boolean has(RelativePath relativePath) {
        ProcessingFile file = this.directory.resolve(relativePath);
        return file.exists();
    }

    private ProcessingFile resolve(RelativePath relativePath) {
        return this.directory.resolve(relativePath);
    }

    private boolean exists() {
        return this.directory.exists();
    }
}
