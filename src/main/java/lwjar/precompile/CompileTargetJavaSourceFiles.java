package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

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
}
