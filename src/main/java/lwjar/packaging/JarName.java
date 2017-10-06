package lwjar.packaging;

import lwjar.primitive.RelativePath;

import java.util.Objects;

class JarName {
    private final String name;
    
    JarName(String name) {
        this.name = Objects.requireNonNull(name);
    }
    
    RelativePath toRelativePath() {
        return new RelativePath(this.name + ".jar");
    }
    
}
