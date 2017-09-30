package lwjar.precompile;

import lwjar.primitive.ClassFile;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.io.IOException;

class LibraryClassDirectory {
    private final Directory directory;

    LibraryClassDirectory(Directory directory) {
        this.directory = directory;
    }

    void copyResourceFilesTo(PreCompiledDirectory preCompiledDirectory) throws IOException {
        if (this.directory == null) {
            return;
        }

        System.out.println("coping no java source files...");
        this.directory.walkFiles(file -> {
            if (file.isJavaSource() || file.isClassFile()) {
                return;
            }

            RelativePath relativePath = this.directory.relativePath(file);
            ProcessingFile outFile = preCompiledDirectory.resolve(relativePath);

            if (outFile.exists()) {
                return;
            }

            file.copyTo(outFile);
            
            System.out.println(relativePath.getPath() + " is copied.");
        });
    }

    void copyClassFileThatOnlyExistsInBinaryJar(PreCompiledDirectory preCompiledDirectory) throws IOException {
        if (this.directory == null) {
            return;
        }

        System.out.println("coping class files only binary jar files...");

        this.directory.walkFiles(file -> {
            if (!file.isClassFile()) {
                return;
            }

            RelativePath relativeJavaSourcePath = this.toRelativeJavaSourcePath(file);
            if (preCompiledDirectory.has(relativeJavaSourcePath)) {
                // precompile was successful.
                return;
            }

            RelativePath relativeClassFilePath = this.directory.relativePath(file);
            if (preCompiledDirectory.has(relativeClassFilePath)) {
                // precompile was failed, but java source exists in source jar.
                return;
            }

            ProcessingFile preCompiledClassFile = preCompiledDirectory.resolve(relativeClassFilePath);

            // class file exists in original jar file, but source file (or compiled class file) doesn't exist.
            file.copyTo(preCompiledClassFile);

            System.out.println(relativeClassFilePath.getPath() + " is copied.");
        });
    }
    
    private RelativePath toRelativeJavaSourcePath(ProcessingFile classFile) {
        String className = new ClassFile(classFile).extractClassName();
        ProcessingFile expectedJavaSourceFile = classFile.replaceFileName(className + ".java");
        return this.directory.relativePath(expectedJavaSourceFile);
    }

    void copyErrorClassFiles(PreCompiledDirectory preCompiledDirectory, UncompilableJavaSources uncompilableJavaSources) {
        if (this.directory == null) {
            throw new IllegalStateException("-c options is not set. please set classes directory path.");
        }

        uncompilableJavaSources.forEach(uncompilableJavaSource -> {
            ProcessingFile originalClassFile = this.findClassFile(uncompilableJavaSource);
            ProcessingFile outClassFile = this.resolveOutClassFile(preCompiledDirectory, originalClassFile);

            originalClassFile.copyTo(outClassFile);
        });
    }

    private ProcessingFile findClassFile(UncompilableJavaSource javaSource) {
        RelativePath packagePath = javaSource.getPackagePath();
        Directory packageDirectory = this.directory.resolveDirectory(packagePath);

        String className = javaSource.getClassName();
        return packageDirectory.findFileStartsWith(className);
    }

    private ProcessingFile resolveOutClassFile(PreCompiledDirectory preCompiledDirectory, ProcessingFile originalClassFile) {
        RelativePath relativeUncompilableClassFilePath = this.directory.relativePath(originalClassFile);
        return preCompiledDirectory.resolve(relativeUncompilableClassFilePath);
    }
}
