package lwjar.precompile;

import lwjar.primitive.Directory;

import java.util.Objects;

class CompileWorkDirectory {
    private final Directory directory;

    CompileWorkDirectory(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    void recreate() {
        if (this.directory.exists()) {
            this.directory.remove();
        }
        this.directory.create();
    }
    
    String getStringPath() {
        return this.directory.getStringPath();
    }
}
