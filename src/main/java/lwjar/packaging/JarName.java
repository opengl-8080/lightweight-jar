package lwjar.packaging;

import lwjar.primitive.RelativePath;

import java.util.Objects;

public class JarName {
    private final String name;
    
    public JarName(String name) {
        this.name = Objects.requireNonNull(name);
    }
    
    RelativePath toRelativePath() {
        return new RelativePath(this.name + ".jar");
    }
    
}
