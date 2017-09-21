package lwjar.precompile;

import java.util.Set;
import java.util.function.Consumer;

public class UncompilableJavaSources {
    private final Set<RelativeJavaSourcePath> files;

    public UncompilableJavaSources(Set<RelativeJavaSourcePath> files) {
        this.files = files;
    }
    
    void forEach(Consumer<RelativeJavaSourcePath> consumer) {
        this.files.forEach(consumer);
    }
}
