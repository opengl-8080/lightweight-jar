package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.primitive.Directory;
import lwjar.primitive.RelativePath;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class PreCompiler {
    private final CompileWorkDirectory workDir;
    private final ErrorLogFile compileErrorLog;

    PreCompiler(ErrorLogFile compileErrorLog, Directory outputDir) {
        this.compileErrorLog = compileErrorLog;
        this.workDir = new CompileWorkDirectory(outputDir.resolveDirectory(new RelativePath(Paths.get("work"))));
    }

    CompileResult compile(JavaSourceFiles javaSourceFiles, ClassPath classPath) throws IOException {
        this.workDir.recreate();

        String[] args = this.buildJavacArgs(javaSourceFiles, classPath);

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler()
                .run(null, null, error, args);

        String errorMessage = error.toString(Charset.defaultCharset().toString());

        this.compileErrorLog.append(errorMessage);

        return new CompileResult(resultCode != 0, errorMessage);
    }

    private String[] buildJavacArgs(JavaSourceFiles javaSourceFiles, ClassPath classPath) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.getStringPath());
        javacOptions.add("-cp");
        javacOptions.add(classPath.getStringPath());
        javacOptions.add("-encoding");
        javacOptions.add(GlobalOption.getEncoding().toString());
        javacOptions.addAll(javaSourceFiles.toStringList());

        return javacOptions.toArray(new String[javacOptions.size()]);
    }
}
