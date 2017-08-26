package lwjar;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

public class PreCompileCommand implements Command {

    private final Path sourceFileDir;
    private final Path classesFileDir;
    private final Path outputDir;
    private final Path workSrcDir;
    private final Path workClassesDir;

    public PreCompileCommand(Path sourceFileDir, Path classesFileDir, Path outputDir) {
        this.sourceFileDir = sourceFileDir;
        this.classesFileDir = classesFileDir;
        this.outputDir = outputDir;
        this.workSrcDir = this.outputDir.resolve("src");
        this.workClassesDir = this.outputDir.resolve("classes");
    }

    @Override
    public void execute() throws IOException {
        this.copySourceFiles();
        this.copyClassesFiles();
        this.compile();
    }

    private void compile() throws IOException {
        List<String> sourceFiles = this.collectSourceFiles();
        String[] args = this.buildJavacArgs(sourceFiles);

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        int resultCode = ToolProvider.getSystemJavaCompiler()
                            .run(null, null, error, args);

        if (resultCode != 0) {
            this.printErrorSourceCodePaths(error);
        }
    }

    private List<String> collectSourceFiles() throws IOException {
        return Files.walk(this.workSrcDir)
                .filter(path -> path.getFileName().toString().endsWith(".java"))
                .map(file -> file.toAbsolutePath().toString())
                .collect(toList());
    }

    private String[] buildJavacArgs(List<String> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workClassesDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.workSrcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add("UTF-8");
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }

    private void printErrorSourceCodePaths(ByteArrayOutputStream error) throws UnsupportedEncodingException {
        String errorMessage = error.toString(Charset.defaultCharset().toString());
        Pattern pattern = Pattern.compile("([^ \\r\\n]+\\.java)");
        Matcher matcher = pattern.matcher(errorMessage);
        Set<String> errorSrcPathSet = new HashSet<>();

        while (matcher.find()) {
            String sourcePath = matcher.group();
            errorSrcPathSet.add(sourcePath);
        }

        System.err.println("Compile Errors are occurred!!");
        System.err.println("Error source code paths are...");
        System.err.println();
        errorSrcPathSet.stream()
                .map(Paths::get)
                .map(this.workSrcDir.toAbsolutePath()::relativize)
                .map(Path::toString)
                .sorted()
                .forEach(System.err::println);
    }

    private void copySourceFiles() throws IOException {
        Files.createDirectories(this.workSrcDir);

        Files.walk(this.sourceFileDir)
            .filter(path -> path.getFileName().toString().endsWith(".java"))
            .forEach(javaFile -> {
                Path relativePath = this.sourceFileDir.relativize(javaFile);
                Path copyTo = this.workSrcDir.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    Files.copy(javaFile, copyTo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy source file.", e);
                }
            });
    }

    private void copyClassesFiles() throws IOException {
        if (Files.notExists(this.workClassesDir)) {
            Files.createDirectories(this.workClassesDir);
        }

        if (this.classesFileDir == null) {
            return;
        }

        Files.walk(this.classesFileDir)
            .forEach(classFile -> {
                Path relativePath = this.classesFileDir.relativize(classFile);
                Path copyTo = this.workClassesDir.resolve(relativePath);

                if (Files.notExists(copyTo.getParent())) {
                    try {
                        Files.createDirectories(copyTo.getParent());
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to create output directory.", e);
                    }
                }

                try {
                    Files.copy(classFile, copyTo, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new UncheckedIOException("failed to copy class file.", e);
                }
            });
    }
}
