package lwjar.primitive;

import java.util.Objects;

public class ClassFile {
    private final ProcessingFile file;

    public ClassFile(ProcessingFile file) {
        this.file = Objects.requireNonNull(file);
        if (!file.isClassFile()) {
            throw new IllegalArgumentException("file is not class file.");
        }
    }

    public String extractClassName() {
        String name = this.file.getName();

        if (name.contains("$")) {
            // inner class
            String[] tokens = name.split("\\$");
            return tokens[0];
        }

        return name.replace(".class", "");
    }
}
