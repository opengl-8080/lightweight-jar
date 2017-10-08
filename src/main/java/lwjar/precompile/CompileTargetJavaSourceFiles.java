package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class CompileTargetJavaSourceFiles {
    private Set<ProcessingFile> sourceFiles;

    CompileTargetJavaSourceFiles(Collection<ProcessingFile> sourceFiles) {
        this.sourceFiles = new HashSet<>(sourceFiles);
    }

    List<String> toStringList() {
        return sourceFiles.stream().map(ProcessingFile::getAbsolutePathString).collect(Collectors.toList());
    }

    Iterable<? extends JavaFileObject> collect(StandardJavaFileManager fileManager) {
        List<File> javaFiles = this.sourceFiles.stream().map(file -> file.getPath().toFile()).collect(Collectors.toList());
        return fileManager.getJavaFileObjectsFromFiles(javaFiles);
    }
}
