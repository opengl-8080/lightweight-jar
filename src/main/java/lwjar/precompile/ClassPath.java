package lwjar.precompile;

import lwjar.primitive.Directory;

import java.util.Objects;

class ClassPath {
    private final Directory directory;

    ClassPath(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
    }
    
    String getStringPath() {
        return this.directory.getStringPath();
    }
}
