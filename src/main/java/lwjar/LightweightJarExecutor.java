package lwjar;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import static java.util.stream.Collectors.*;

public class LightweightJarExecutor {

    public static void main(String[] args) throws Exception {
        new LightweightJarExecutor().execute(args);
    }
    
    private Manifest manifest;
    private final Path workDir = Paths.get("./out/runtime");
    private Path classesDir = this.workDir.resolve("classes");
    private Path srcDir = this.workDir.resolve("src");
    
    private void execute(String[] args) throws Exception {
        this.loadManifest();
        
        this.createDirectories(this.workDir);
        this.createDirectories(this.classesDir);

        this.extractSourceFiles();
        this.compile();
        if (this.isSpringBoot()) {
            this.createSpringBootManifest();
        }
        this.executeMainClass(args);
    }
    
    private void createDirectories(Path dir) throws IOException {
        if (Files.exists(dir)) {
            return;
        }
        
        Files.createDirectories(dir);
    }
    
    private boolean isSpringBoot() {
        return Boolean.valueOf(this.getManifestAttribute("Is-Spring-Boot"));
    }
    
    private void createSpringBootManifest() throws IOException {
        Path metaInf = this.classesDir.resolve("META-INF");
        this.createDirectories(metaInf);

        Path manifest = metaInf.resolve("MANIFEST.MF");
        String content = "Start-Class: " + this.getManifestAttribute("Actual-Main-Class") + "\n";
        Files.write(manifest, content.getBytes());
    }
    
    private void loadManifest() {
        try (InputStream in = this.getClass().getResourceAsStream("/META-INF/MANIFEST.MF")) {
            this.manifest = new Manifest(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String getManifestAttribute(String name) {
        return (String)this.manifest.getMainAttributes().get(new Attributes.Name(name));
    }
    
    private void executeMainClass(String[] args) {
        try {
            URL url = this.classesDir.toUri().toURL();
            ClassLoader classLoader = URLClassLoader.newInstance(new URL[]{url}, LightweightJarExecutor.class.getClassLoader());
            String mainClassName = this.resolveMainClassName();
            Class<?> mainClass = Class.forName(mainClassName, true, classLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object)args);
        } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }
    
    private String resolveMainClassName() {
        return this.isSpringBoot() ? "org.springframework.boot.loader.JarLauncher"
                                   : this.getManifestAttribute("Actual-Main-Class");
    }
    
    private void compile() throws IOException {
        List<String> sourceFiles = collectSourceFiles(this.srcDir);
        String[] javacArgs = buildJavacArgs(sourceFiles, this.srcDir, this.classesDir);

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler().run(null, null, null, javacArgs);
        if (resultCode != 0) {
            throw new RuntimeException("javac is failed.");
        }
    }
    
    private List<String> collectSourceFiles(Path srcDir) throws IOException {
        try (Stream<Path> s = Files.walk(srcDir)) {
            return s.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }
    
    private String[] buildJavacArgs(List<String> sourceFiles, Path srcDir, Path classesDir) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(classesDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(srcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add(this.getManifestAttribute("Javac-Encoding"));
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }

    private void extractSourceFiles() throws IOException {
        System.out.println("copy source files...");
        findThisJarFile()
                .stream()
                .filter(e -> e.getName().startsWith("src/") && !e.isDirectory())
                .forEach(entry -> {
                    Path path = Paths.get(entry.getName());
                    Path outPath = this.workDir.resolve(path);
                    if (Files.notExists(outPath.getParent())) {
                        try {
                            Files.createDirectories(outPath.getParent());
                        } catch (IOException e) {
                            throw new UncheckedIOException("failed to create directory.", e);
                        }
                    }

                    try (InputStream in = LightweightJarExecutor.class.getResourceAsStream("/" + entry.getName())) {
                        Files.copy(in, outPath, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        throw new UncheckedIOException("failed to copy source files.", e);
                    }
                });
    }
    
    private JarFile findThisJarFile() throws IOException {
        URL location = LightweightJarExecutor.class.getProtectionDomain().getCodeSource().getLocation();
        return new JarFile(location.getFile());
    }
}
