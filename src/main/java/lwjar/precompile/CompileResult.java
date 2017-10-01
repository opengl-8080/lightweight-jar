package lwjar.precompile;

import lwjar.primitive.ProcessingFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class CompileResult {
    private final boolean error;
    private final String errorMessage;

    CompileResult(boolean error, String errorMessage) {
        this.error = error;
        this.errorMessage = errorMessage;
    }

    UncompilableJavaSources getErrorSourceFiles(PreCompiledDirectory preCompiledDirectory) throws IOException {
        Pattern pattern = Pattern.compile("^([^ \\r\\n]+\\.java):\\d+:", Pattern.MULTILINE);
        Matcher matcher = pattern.matcher(this.errorMessage);
        Set<UncompilableJavaSource> paths = new HashSet<>();

        while (matcher.find()) {
            Path absoluteSourcePath = Paths.get(matcher.group(1));
            ProcessingFile uncompilableSource = new ProcessingFile(absoluteSourcePath);
            UncompilableJavaSource uncompilableJavaSource = new UncompilableJavaSource(preCompiledDirectory, uncompilableSource);
            paths.add(uncompilableJavaSource);
        }

        return new UncompilableJavaSources(paths);
    }

    boolean isError() {
        return this.error;
    }
}
