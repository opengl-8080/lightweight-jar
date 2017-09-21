package lwjar.precompile;

import java.util.Objects;

class ClassFile {
    private final ProcessingFile file;

    ClassFile(ProcessingFile file) {
        this.file = Objects.requireNonNull(file);
        if (!file.isClassFile()) {
            throw new IllegalArgumentException("file is not class file.");
        }
    }
    
    String extractClassName() {
        String name = this.file.name();

        if (name.contains("$")) {
            // inner class
            String[] tokens = name.split("\\$");
            return tokens[0];
        }

        return name.replace(".class", "");
    }
}
