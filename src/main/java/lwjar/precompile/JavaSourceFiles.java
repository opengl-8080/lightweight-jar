package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import java.util.List;
import java.util.stream.Collectors;

class JavaSourceFiles {
    private List<ProcessingFile> sourceFiles;

    JavaSourceFiles(List<ProcessingFile> sourceFiles) {
        this.sourceFiles = sourceFiles;
    }

    List<String> toStringList() {
        return sourceFiles.stream().map(ProcessingFile::getAbsolutePathString).collect(Collectors.toList());
    }
}
