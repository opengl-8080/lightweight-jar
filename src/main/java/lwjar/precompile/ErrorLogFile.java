package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.nio.file.Paths;
import java.util.Objects;

class ErrorLogFile {
    private final static String FILE_NAME = "compile-errors-%d.log";
    private final OutputDirectory outputDirectory;
    
    ErrorLogFile(OutputDirectory outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory);
    }
    
    void write(int compileCounter, String text) {
        if (text.isEmpty()) {
            return;
        }

        String fileName = String.format(FILE_NAME, compileCounter);
        ProcessingFile file = this.outputDirectory.resolveFile(new RelativePath(Paths.get(fileName)));
        file.write(text, GlobalOption.getEncoding());
    }
}
