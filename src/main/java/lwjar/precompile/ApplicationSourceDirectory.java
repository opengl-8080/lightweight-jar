package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.util.Objects;

public class ApplicationSourceDirectory {
    private final Directory directory;

    public ApplicationSourceDirectory(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    void walkFiles(ApplicationDirectoryVisitor visitor) {
        this.directory.walkFiles(file -> {
            RelativePath relativePath = this.directory.relativePath(file);
            visitor.visit(file, relativePath);
        });
    }
    
    interface ApplicationDirectoryVisitor {
        void visit(ProcessingFile file, RelativePath relativePath);
    }
}
