package lwjar.precompile;

import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

public class LibrarySourceDirectory {
    
    private final Directory directory;

    public LibrarySourceDirectory(Directory directory) {
        this.directory = directory;
    }
    
    void walkFiles(LibrarySourceVisitor visitor) {
        this.directory.walkFiles(file -> {
            RelativePath relativePath = this.directory.relativePath(file);
            visitor.visit(file, relativePath);
        });
    }
    
    interface LibrarySourceVisitor {
        void visit(ProcessingFile file, RelativePath relativePath);
    }
}
