package lwjar.packaging;

import lwjar.primitive.Directory;

import java.util.Objects;

public class SourceDirectory {
    private final Directory directory;

    public SourceDirectory(Directory directory) {
        this.directory = Objects.requireNonNull(directory);
    }

    void copyTo(JarWorkDirectory jarWorkDirectory) {
        this.directory.copyTo(jarWorkDirectory.getDirectory());
    }
}
