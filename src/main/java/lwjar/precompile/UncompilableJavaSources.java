package lwjar.precompile;

import java.util.Set;
import java.util.function.Consumer;

class UncompilableJavaSources {
    private final Set<UncompilableJavaSource> files;

    UncompilableJavaSources(Set<UncompilableJavaSource> files) {
        this.files = files;
    }
    
    void forEach(Consumer<UncompilableJavaSource> consumer) {
        this.files.forEach(consumer);
    }
}
