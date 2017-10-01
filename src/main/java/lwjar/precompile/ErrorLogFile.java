package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.nio.file.Paths;
import java.util.Objects;

class ErrorLogFile {
    private static int n = 1;
    private final static String FILE_NAME = "compile-errors-%d.log";
    private final OutputDirectory outputDirectory;
    
    ErrorLogFile(OutputDirectory outputDirectory) {
        this.outputDirectory = Objects.requireNonNull(outputDirectory);
    }
    
    synchronized void write(String text) {
        if (text.isEmpty()) {
            return;
        }

        String fileName = String.format(FILE_NAME, n);
        ProcessingFile file = this.outputDirectory.resolveFile(new RelativePath(Paths.get(fileName)));
        file.write(text, GlobalOption.getEncoding());
        
        n++;
    }
}
