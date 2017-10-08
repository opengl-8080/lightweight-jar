package lwjar.precompile;

import java.nio.file.Path;
import java.util.Set;

class CompileError {
    final Set<Path> errorFiles;
    final String log;

    CompileError(Set<Path> errorFiles, String log) {
        this.errorFiles = errorFiles;
        this.log = log;
    }
}
