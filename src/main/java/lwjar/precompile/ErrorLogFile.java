package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.primitive.Directory;
import lwjar.primitive.RelativePath;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

class ErrorLogFile {
    private final static String FILE_NAME = "compile-errors.log";
    private final Path file;

    ErrorLogFile(OutputDirectory outputDirectory) {
        this.file = outputDirectory.resolve(FILE_NAME).getPath();
    }
    
    void append(String text) {
        if (text.isEmpty()) {
            return;
        }

        try {
            Files.write(this.file, text.getBytes(GlobalOption.getEncoding()), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
