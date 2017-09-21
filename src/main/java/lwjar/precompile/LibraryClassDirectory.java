package lwjar.precompile;

import java.io.IOException;

class LibraryClassDirectory {
    private final Directory directory;

    LibraryClassDirectory(Directory directory) {
        this.directory = directory;
    }

    void copyNoJavaAndClassFiles(PreCompiledDirectory preCompiledDirectory) throws IOException {
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

    void copyClassFileOnly(PreCompiledDirectory preCompiledDirectory) throws IOException {
        if (this.directory == null) {
            return;
        }

        System.out.println("coping class files only binary jar files...");

        this.directory.walkFiles(file -> {
            if (!file.isClassFile()) {
                return;
            }

            String className = new ClassFile(file).extractClassName();
            ProcessingFile javaSourceFile = file.replaceFileName(className);
            RelativePath relativeJavaSourceFile = this.directory.relativePath(javaSourceFile);

            ProcessingFile preCompiledJavaSourceFile = preCompiledDirectory.resolve(relativeJavaSourceFile);

            if (preCompiledJavaSourceFile.exists()) {
                // ok (this class was succeeded to compile)
                return;
            }

            RelativePath relativeClassFilePath = this.directory.relativePath(file);
            ProcessingFile outClassFile = preCompiledDirectory.resolve(relativeClassFilePath);

            if (outClassFile.exists()) {
                // ok (this class was failed to compile but class file exists.)
                return;
            }

            // class file exists in original jar file, but source file (or compiled class file) doesn't exist.
            file.copyTo(outClassFile);

            System.out.println(relativeClassFilePath.getPath() + " is copied.");
        });
    }

    void copyErrorClassFilesFromOrgToOut(PreCompiledDirectory preCompiledDirectory, UncompilableJavaSources uncompilableJavaSources) {
        if (this.directory == null) {
            throw new IllegalStateException("-c options is not set. please set classes directory path.");
        }

        uncompilableJavaSources.forEach(uncompilableJavaSource -> {
            Directory originalUncompilableClassFileDirectory = this.directory.resolveDirectory(uncompilableJavaSource.getParentDir());

            String uncompilableClassName = uncompilableJavaSource.getClassName();
            ProcessingFile originalUncompilableClassFile = originalUncompilableClassFileDirectory.findFileStartsWith(uncompilableClassName);
            RelativePath relativeUncompilableClassFilePath = this.directory.relativePath(originalUncompilableClassFile);
            ProcessingFile outFile = preCompiledDirectory.resolve(relativeUncompilableClassFilePath);
            
            originalUncompilableClassFile.copyTo(outFile);
        });
    }
}
