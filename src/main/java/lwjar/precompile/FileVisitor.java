package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import java.io.IOException;

@FunctionalInterface
public interface FileVisitor {
    
    void visit(ProcessingFile file) throws IOException;
}
