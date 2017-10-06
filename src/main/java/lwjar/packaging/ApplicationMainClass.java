package lwjar.packaging;

import java.util.Objects;

class ApplicationMainClass {
    private final String mainClass;

    ApplicationMainClass(String mainClass) {
        this.mainClass = Objects.requireNonNull(mainClass);
    }
    
    String getName() {
        return this.mainClass;
    }
    
}
