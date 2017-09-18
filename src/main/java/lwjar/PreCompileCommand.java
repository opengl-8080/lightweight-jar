package lwjar;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.javaparser.ast.nodeTypes.modifiers.NodeWithPrivateModifier;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.PrettyPrinterConfiguration;

import javax.tools.ToolProvider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class PreCompileCommand implements Command {
    
    private final Charset encoding;

    private final Path orgSrcDir;
    private final Optional<Path> orgClassesDir;

    private final Path outSrcDir;
    private final Path compileErrorLog;
    
    private final Path workDir;

    /**
     * 0: no compress
     * 1: remove comments
     * 2: remove line separators
     * 3: remove annotations (@Override, @SuppressWarnings, ...)
     * 4: use asterisk import.
     * 5: remove private modifiers.
     * 6: rename local variables to more shorter.
     * 7: and more...?
     */
    private final int compressLevel;
    
    private static class CompressLevel {
        private static final int REMOVE_COMMENTS = 1;
        private static final int REMOVE_LINE_SEPARATOR = 2;
        private static final int REMOVE_ANNOTATIONS = 3;
        private static final int REMOVE_PRIVATE_MODIFIERS = 4;
        private static final int RENAME_LOCAL_VARIABLES = 5;
    }

    public PreCompileCommand(String encoding, Path orgSrcDir, Path orgClassesDir, Path outDir, Integer compressLevel) {
        this.encoding = encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
        this.orgSrcDir = orgSrcDir;
        this.orgClassesDir = Optional.ofNullable(orgClassesDir);
        this.compressLevel = compressLevel == null ? CompressLevel.RENAME_LOCAL_VARIABLES : compressLevel;

        if (outDir == null) {
            outDir = Paths.get("./out");
        }
        
        this.outSrcDir = outDir.resolve("src");
        this.compileErrorLog = outDir.resolve("compile-errors.log");
        
        this.workDir = outDir.resolve("work");
    }

    @Override
    public void execute() throws IOException {
        this.copyOrgToOut();
        this.createWorkDir();

        int cnt = 0;
        CompileResult result = this.compile();

        while (result.error) {
            this.replaceErrorFiles(result);

            cnt++;
            if (100 < cnt) {
                throw new TooManyCompileErrorException("too many compile errors are occurred.");
            }

            result = this.compile();
        }
        
        this.copyClassFileOnly();
        this.copyNoJavaAndClassFiles();
    }
    
    private void copyNoJavaAndClassFiles() throws IOException {
        if (!this.orgClassesDir.isPresent()) {
            return;
        }
        
        System.out.println("coping no java source files...");
        Path orgClassesDir = this.orgClassesDir.get();

        Files.walkFileTree(orgClassesDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isJavaSource(file) || isClassFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                // like, properties, spring.factories, etc...
                Path relativePath = orgClassesDir.relativize(file);
                Path toPath = outSrcDir.resolve(relativePath);

                if (Files.exists(toPath)) {
                    return FileVisitResult.CONTINUE;
                }

                if (Files.notExists(toPath.getParent())) {
                    Files.createDirectories(toPath.getParent());
                }
                
                Files.copy(file, toPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println(relativePath + " is copied.");

                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private void copyClassFileOnly() throws IOException {
        if (!this.orgClassesDir.isPresent()) {
            return;
        }

        System.out.println("coping class files only binary jar files...");
        Path orgClassesDir = this.orgClassesDir.get();
        
        Files.walkFileTree(orgClassesDir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!isClassFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                String className = extractClassName(file);
                Path relativeParentDir = orgClassesDir.relativize(file.getParent());
                
                Path relativeSource = relativeParentDir.resolve(className + ".java");
                Path expectedSource = outSrcDir.resolve(relativeSource);
                
                if (Files.exists(expectedSource)) {
                    // ok (this class was succeeded to compile)
                    return FileVisitResult.CONTINUE;
                }

                Path relativeClass = orgClassesDir.relativize(file);
                Path expectedClass = outSrcDir.resolve(relativeClass);
                
                if (Files.exists(expectedClass)) {
                    // ok (this class was failed to compile but class file exists.)
                    return FileVisitResult.CONTINUE;
                }
                
                // class file exists in original jar file, but source file (or compiled class file) doesn't exist.
                Path relativeOrgClass = orgClassesDir.relativize(file);
                Path outPath = outSrcDir.resolve(relativeOrgClass);
                if (Files.notExists(outPath.getParent())) {
                    Files.createDirectories(outPath.getParent());
                }
                Files.copy(file, outPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println(relativeOrgClass + " is copied.");
                
                return FileVisitResult.CONTINUE;
            }
        });
    }
    
    private String extractClassName(Path file) {
        String name = file.getFileName().toString();
        
        if (name.contains("$")) {
            // inner class
            String[] tokens = name.split("\\$");
            return tokens[0];
        }
        
        return name.replace(".class", "");
    }
    
    private boolean isClassFile(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }

    private void copyOrgToOut() throws IOException {
        if (Files.exists(this.outSrcDir)) {
            return;
        }

        System.out.println("copy and compressing source files...");
        FileUtil.copyFileTree(this.orgSrcDir, this.outSrcDir, (inPath, outPath) -> {
            if (this.isJavaSource(inPath)) {
                String text = this.compress(inPath);
                Files.write(outPath, text.getBytes(this.encoding), StandardOpenOption.CREATE);
            } else if (!inPath.getFileName().toString().equals("MANIFEST.MF")) {
                Files.copy(inPath, outPath, StandardCopyOption.REPLACE_EXISTING);
            }
        });
    }
    
    private String compress(Path javaFile) throws IOException {
        CompilationUnit cu = JavaParser.parse(javaFile, this.encoding);
        return this.removeLineSeparatorAndComments(cu);
    }
    
    private String removeLineSeparatorAndComments(CompilationUnit cu) {
        PrettyPrinterConfiguration conf = new PrettyPrinterConfiguration();
        
        if (CompressLevel.REMOVE_LINE_SEPARATOR <= this.compressLevel) {
            conf.setIndent("");
            conf.setEndOfLineCharacter(" ");
        }

        if (CompressLevel.REMOVE_COMMENTS <= this.compressLevel) {
            conf.setPrintComments(false);
            conf.setPrintJavaDoc(false);
        }
        
        if (CompressLevel.REMOVE_ANNOTATIONS <= this.compressLevel) {
            List<AnnotationExpr> removeTargetAnnotations = new ArrayList<>();
            
            cu.accept(new VoidVisitorAdapter<Void>() {
                @Override
                public void visit(MarkerAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }
                @Override
                public void visit(SingleMemberAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }
                @Override
                public void visit(NormalAnnotationExpr n, Void arg) {
                    if (isRemoveTarget(n)) {
                        removeTargetAnnotations.add(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(FieldDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(MethodDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ConstructorDeclaration n, Void arg) {
                    this.removePrivateModifier(n);
                    super.visit(n, arg);
                }

                @Override
                public void visit(ClassOrInterfaceDeclaration n, Void arg) {
                    if (n.isInnerClass() || n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(EnumDeclaration n, Void arg) {
                    if (n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                @Override
                public void visit(AnnotationDeclaration n, Void arg) {
                    if (n.isNestedType()) {
                        this.removePrivateModifier(n);
                    }
                    super.visit(n, arg);
                }

                private void removePrivateModifier(NodeWithPrivateModifier node) {
                    if (CompressLevel.REMOVE_PRIVATE_MODIFIERS <= compressLevel) {
                        if (node.isPrivate()) {
                            node.removeModifier(Modifier.PRIVATE);
                        }
                    }
                }
            }, null);

            // remove annotations.
            removeTargetAnnotations.forEach(a -> a.getParentNode().ifPresent(p -> p.remove(a)));
        }
        
        return cu.toString(conf);
    }

    private static final Set<String> REMOVE_TARGET_ANNOTATION_NAMES;
    static {
        Set<String> set = new HashSet<>();
        set.add("Override");
        set.add("Deprecated");
        set.add("Documented");
        set.add("SuppressWarnings");
        set.add("FunctionalInterface");
        set.add("SafeVarargs");
        REMOVE_TARGET_ANNOTATION_NAMES = Collections.unmodifiableSet(set);
    }
    
    private boolean isRemoveTarget(AnnotationExpr annotation) {
        String name = annotation.getName().asString();
        return REMOVE_TARGET_ANNOTATION_NAMES.contains(name);
    }

    private boolean isJavaSource(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }

    private void createWorkDir() {
        try {
            Files.createDirectories(this.workDir);
        } catch (AccessDeniedException e) {
            // skip this. because this error is sometimes occurred without any problems. (I don't understand well...)
            System.err.println("Warning: " + e.getClass() + " : " + e.getMessage());
        } catch (IOException e) {
            throw new UncheckedIOException("failed to create work directory.", e);
        }
    }

    private void replaceErrorFiles(CompileResult result) throws IOException {
        Set<Path> errorSourceFiles = result.getErrorSourceFiles(this.outSrcDir);

        System.out.println("remove error source files...");
        errorSourceFiles.forEach(System.out::println);

        this.copyErrorClassFilesFromOrgToOut(errorSourceFiles);
        this.removeErrorJavaFileFromOut(errorSourceFiles);
    }

    private void copyErrorClassFilesFromOrgToOut(Set<Path> errorSourceFiles) {
        Path orgClassesDir = this.orgClassesDir.orElseThrow(() -> new IllegalStateException("-c options is not set. please set classes directory path."));

        errorSourceFiles.forEach(javaFile -> {
            String className = javaFile.getFileName().toString().replaceAll("\\.java$", "");
            Path classFileDir = orgClassesDir.resolve(javaFile.getParent());
            
            try (Stream<Path> files = Files.list(classFileDir)) {
                files
                    .filter(file -> file.getFileName().toString().startsWith(className))
                    .forEach(orgClassFile -> {
                        Path classFilePath = orgClassesDir.relativize(orgClassFile);
                        Path outClassFile = this.outSrcDir.resolve(classFilePath);

                        try {
                            Files.copy(orgClassFile, outClassFile, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            throw new UncheckedIOException("failed to copy file.", e);
                        }
                    });
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    private void removeErrorJavaFileFromOut(Set<Path> errorSourceFiles) {
        errorSourceFiles.forEach(javaFile -> {
            Path outJavaFile = this.outSrcDir.resolve(javaFile);
            try {
                Files.delete(outJavaFile);
            } catch (IOException e) {
                throw new UncheckedIOException("failed to remove file.", e);
            }
        });
    }

    private CompileResult compile() throws IOException {
        this.recreateWorkDir();

        List<String> sourceFiles = this.collectSourceFiles();
        String[] args = this.buildJavacArgs(sourceFiles);

        ByteArrayOutputStream error = new ByteArrayOutputStream();

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler()
                .run(null, null, error, args);

        String errorMessage = error.toString(Charset.defaultCharset().toString());

        if (!errorMessage.isEmpty()) {
            Files.write(this.compileErrorLog, error.toByteArray(), StandardOpenOption.APPEND, StandardOpenOption.CREATE);
        }

        return new CompileResult(resultCode != 0, errorMessage);
    }
    
    private void recreateWorkDir() throws IOException {
        System.out.println("recreate work directory...");
        this.removeWorkDir();
        this.createWorkDir();
    }
    
    private void removeWorkDir() {
        try {
            Files.walkFileTree(this.workDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }
    
                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException("failed remove work directory.", e);
        }
    }

    private List<String> collectSourceFiles() throws IOException {
        try (Stream<Path> s = Files.walk(this.outSrcDir)) {
            return s.filter(this::isJavaSource)
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }

    private String[] buildJavacArgs(List<String> sourceFiles) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(this.workDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(this.outSrcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add(this.encoding.toString());
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }
    

    private static class CompileResult {
        private final boolean error;
        private final String errorMessage;

        private CompileResult(boolean error, String errorMessage) {
            this.error = error;
            this.errorMessage = errorMessage;
        }

        private Set<Path> getErrorSourceFiles(Path outSrcDir) throws IOException {
            System.out.println("extracting error source files...");
            
            Pattern pattern = Pattern.compile("^([^ \\r\\n]+\\.java):\\d+:", Pattern.MULTILINE);
            Matcher matcher = pattern.matcher(this.errorMessage);
            Set<String> errorSrcPathSet = new HashSet<>();

            while (matcher.find()) {
                String sourcePath = matcher.group(1);
                errorSrcPathSet.add(sourcePath);
            }

            return errorSrcPathSet.stream()
                    .map(Paths::get)
                    .map(outSrcDir.toAbsolutePath()::relativize)
                    .sorted()
                    .collect(toSet());
        }
    }
}
