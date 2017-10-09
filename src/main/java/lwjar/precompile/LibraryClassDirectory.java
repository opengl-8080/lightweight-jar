package lwjar.precompile;

import lwjar.primitive.ClassFile;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.util.List;

public class LibraryClassDirectory {
    private final Directory directory;

    public LibraryClassDirectory(Directory directory) {
        this.directory = directory;
    }
    
    void walkFiles(LibraryClassDirectoryVisitor visitor) {
        this.directory.walkFiles(file -> {
            RelativePath relativeClassFilePath = this.directory.relativePath(file);
            visitor.visit(file, relativeClassFilePath);
        });
    }
    
    RelativePath toRelativeJavaSourcePath(ProcessingFile classFile) {
        String className = new ClassFile(classFile).extractClassName();
        ProcessingFile expectedJavaSourceFile = classFile.replaceFileName(className + ".java");
        return this.directory.relativePath(expectedJavaSourceFile);
    }
    
    void walkOriginalClassFiles(UncompilableJavaSources uncompilableJavaSources, LibraryClassDirectoryVisitor visitor) {
        uncompilableJavaSources
            .map(this::toOriginalClassFiles)
            .forEach(originalClassFile -> {
                // Original class files may exist more than one, if source class has some inner classes.
                visitor.visit(originalClassFile, this.directory.relativePath(originalClassFile));
            });
    }

    private List<ProcessingFile> toOriginalClassFiles(UncompilableJavaSource javaSource) {
        RelativePath packagePath = javaSource.getPackagePath();
        Directory packageDirectory = this.directory.resolveDirectory(packagePath);

        String className = javaSource.getClassName();
        return packageDirectory.findFilesStartsWith(className);
    }

    interface LibraryClassDirectoryVisitor {
        void visit(ProcessingFile file, RelativePath relativePath);
    }
}
