package lwjar;

import javax.tools.ToolProvider;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
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
        Path workDir = Paths.get("./out/execute");
        if (Files.notExists(workDir)) {
            Files.createDirectories(workDir);
        }
        Path classesDir = workDir.resolve("classes");
        if (Files.notExists(classesDir)) {
            Files.createDirectories(classesDir);
        }
        Path srcDir = workDir.resolve("src");

        extractSourceFiles(workDir);
        compile(srcDir, classesDir);
        execute(classesDir, args);
    }
    
    private static void execute(Path classesDir, String[] args) {
        try (InputStream in = LightweightJarExecutor.class.getResourceAsStream("/META-INF/MANIFEST.MF")) {
            Manifest manifest = new Manifest(in);
            String actualMainClass = (String)manifest.getMainAttributes().get(new Attributes.Name("Actual-Main-Class"));
            
            URL url = classesDir.toUri().toURL();
            ClassLoader classLoader = URLClassLoader.newInstance(new URL[]{url}, LightweightJarExecutor.class.getClassLoader());
            Class<?> mainClass = Class.forName(actualMainClass, true, classLoader);
            Method mainMethod = mainClass.getMethod("main", String[].class);
            mainMethod.invoke(null, (Object)args);
        } catch (IOException | NoSuchMethodException | InvocationTargetException | IllegalAccessException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    
    private static void compile(Path srcDir, Path classesDir) throws IOException {
        List<String> sourceFiles = collectSourceFiles(srcDir);
        String[] javacArgs = buildJavacArgs(sourceFiles, srcDir, classesDir);

        System.out.println("compiling...");
        int resultCode = ToolProvider.getSystemJavaCompiler().run(null, null, null, javacArgs);
        if (resultCode != 0) {
            throw new RuntimeException("javac is failed.");
        }
    }
    
    private static List<String> collectSourceFiles(Path srcDir) throws IOException {
        try (Stream<Path> s = Files.walk(srcDir)) {
            return s.filter(path -> path.getFileName().toString().endsWith(".java"))
                    .map(file -> file.toAbsolutePath().toString())
                    .collect(toList());
        }
    }
    
    private static String[] buildJavacArgs(List<String> sourceFiles, Path srcDir, Path classesDir) {
        List<String> javacOptions = new ArrayList<>();
        javacOptions.add("-Xlint:none");
        javacOptions.add("-d");
        javacOptions.add(classesDir.toString());
        javacOptions.add("-cp");
        javacOptions.add(srcDir.toString());
        javacOptions.add("-encoding");
        javacOptions.add("UTF-8");
        javacOptions.addAll(sourceFiles);

        return javacOptions.toArray(new String[javacOptions.size()]);
    }


    private static void extractSourceFiles(Path workDir) throws IOException {
        System.out.println("copy source files...");
        findThisJarFile()
                .stream()
                .filter(e -> e.getName().startsWith("src/") && !e.isDirectory())
                .forEach(entry -> {
                    Path path = Paths.get(entry.getName());
                    Path outPath = workDir.resolve(path);
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
    
    private static JarFile findThisJarFile() throws IOException {
        URL location = LightweightJarExecutor.class.getProtectionDomain().getCodeSource().getLocation();
        return new JarFile(location.getFile());
    }
}
