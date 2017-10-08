package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.OutputDirectory;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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

        System.out.println("[" + PreCompiler.count + "] compiling...");

        DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = this.buildCompileTask(diagnosticCollector);
        
        Boolean success = task.call();

        CompileError compileError = this.collectCompileError(diagnosticCollector);
        
        if (!success) {
            this.compileErrorLog.write(PreCompiler.count, compileError.log);
        }

        PreCompiler.count++;
        return new CompileResult(success, compileError.errorFiles);
    }

    private JavaCompiler.CompilationTask buildCompileTask(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnosticCollector, null, null);

        Iterable<String> options = Arrays.asList(
                "-Xlint:none",
                "-cp",
                this.preCompiledDirectory.getStringPath(),
                "-d",
                this.workDir.getStringPath(),
                "-encoding",
                GlobalOption.getEncoding().name()
        );

        CompileTargetJavaSourceFiles compileTargetJavaSourceFiles = this.preCompiledDirectory.collectCompileTargetJavaSourceFiles();
        Iterable<? extends JavaFileObject> javaFileObjects = compileTargetJavaSourceFiles.collect(standardFileManager);

        return compiler.getTask(null, standardFileManager, diagnosticCollector, options, null, javaFileObjects);
    }
    
    private CompileError collectCompileError(DiagnosticCollector<JavaFileObject> diagnosticCollector) {
        StringBuilder errorMessage = new StringBuilder();
        Set<Path> errorFiles = new HashSet<>();

        diagnosticCollector
                .getDiagnostics()
                .stream()
                .filter(diagnostic -> diagnostic.getKind().equals(Diagnostic.Kind.ERROR))
                .forEach(diagnostic -> {
                    Path errorFile = this.toPath(diagnostic);
                    errorFiles.add(errorFile);
                    String log = this.toErrorLog(diagnostic, errorFile);
                    errorMessage.append(log).append("\n");
                });
        
        return new CompileError(errorFiles, errorMessage.toString());
    }
    
    private Path toPath(Diagnostic<? extends JavaFileObject> diagnostic) {
        JavaFileObject javaFileObject = diagnostic.getSource();
        return Paths.get(javaFileObject.toUri()).toAbsolutePath();
    }
    
    private String toErrorLog(Diagnostic<? extends JavaFileObject> diagnostic, Path errorFile) {
        return diagnostic.getKind() + "\t"
                + diagnostic.getMessage(Locale.getDefault()) + "\t"
                + diagnostic.getLineNumber() + "\t"
                + diagnostic.getColumnNumber() + "\t"
                + diagnostic.getPosition() + "\t"
                + diagnostic.getStartPosition() + "\t"
                + diagnostic.getEndPosition() + "\t"
                + errorFile;
    }
}
