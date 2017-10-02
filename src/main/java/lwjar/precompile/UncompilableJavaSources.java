package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

class UncompilableJavaSources {
    private final Set<UncompilableJavaSource> files;

    UncompilableJavaSources(Set<UncompilableJavaSource> files) {
        this.files = files;
    }
    
    Stream<ProcessingFile> map(Function<UncompilableJavaSource, List<ProcessingFile>> mapper) {
        return this.files.stream().map(mapper).flatMap(List::stream);
    }

    void removeFiles() {
        this.files.forEach(UncompilableJavaSource::delete);
    }
}
