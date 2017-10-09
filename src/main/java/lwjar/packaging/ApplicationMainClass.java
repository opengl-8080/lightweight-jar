package lwjar.packaging;

import java.util.Objects;

public class ApplicationMainClass {
    private final String mainClass;

    public ApplicationMainClass(String mainClass) {
        this.mainClass = Objects.requireNonNull(mainClass);
    }
    
    String getName() {
        return this.mainClass;
    }
    
}
