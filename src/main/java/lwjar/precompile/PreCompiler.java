package lwjar.precompile;

import lwjar.GlobalOption;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

class PreCompiler {
    private static int count = 1;
    private final PreCompiledDirectory preCompiledDirectory;
    private final CompileWorkDirectory workDir;
    private final ErrorLogFile compileErrorLog;

    PreCompiler(OutputDirectory outputDirectory, PreCompiledDirectory preCompiledDirectory) {
        this.compileErrorLog = new ErrorLogFile(outputDirectory);
        this.workDir = new CompileWorkDirectory(outputDirectory.resolveDirectory("work"));
        this.preCompiledDirectory = preCompiledDirectory;
    }

    CompileResult compile() throws IOException {
        this.workDir.recreate();

        String[] args = this.buildJavacArgs();

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        System.out.println("[" + PreCompiler.count + "] compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler().run(null, null, error, args);

        String errorMessage = error.toString(Charset.defaultCharset().toString());

        this.compileErrorLog.write(PreCompiler.count, errorMessage);

        PreCompiler.count++;
        return new CompileResult(resultCode != 0, errorMessage);
    }

    private String[] buildJavacArgs() {
        CompileTargetJavaSourceFiles compileTargetJavaSourceFiles = this.preCompiledDirectory.collectCompileTargetJavaSourceFiles();
        String classPath = this.preCompiledDirectory.getStringPath();

        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.getStringPath());
        javacOptions.add("-cp");
        javacOptions.add(classPath);
        javacOptions.add("-encoding");
        javacOptions.add(GlobalOption.getEncoding().toString());
        javacOptions.addAll(compileTargetJavaSourceFiles.toStringList());

        return javacOptions.toArray(new String[javacOptions.size()]);
    }
}
