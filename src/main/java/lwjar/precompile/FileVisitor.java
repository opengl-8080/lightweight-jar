package lwjar.precompile;

import java.io.IOException;

@FunctionalInterface
public interface FileVisitor {
    
    void visit(ProcessingFile file) throws IOException;
}
