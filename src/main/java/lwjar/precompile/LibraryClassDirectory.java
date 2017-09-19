package lwjar.precompile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Set;
import java.util.stream.Stream;

public class LibraryClassDirectory {
    private final Path dir;

    public LibraryClassDirectory(Path dir) {
        this.dir = dir;
    }

    public void copyNoJavaAndClassFiles(Path toDir) throws IOException {
        if (this.dir == null) {
            return;
        }

        System.out.println("coping no java source files...");
        Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (isJavaSource(file) || isClassFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                // like, properties, spring.factories, etc...
                Path relativePath = dir.relativize(file);
                Path toPath = toDir.resolve(relativePath);

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

    public void copyClassFileOnly(Path toDir) throws IOException {
        if (this.dir == null) {
            return;
        }

        System.out.println("coping class files only binary jar files...");

        Files.walkFileTree(this.dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (!isClassFile(file)) {
                    return FileVisitResult.CONTINUE;
                }

                String className = extractClassName(file);
                Path relativeParentDir = dir.relativize(file.getParent());

                Path relativeSource = relativeParentDir.resolve(className + ".java");
                Path expectedSource = toDir.resolve(relativeSource);

                if (Files.exists(expectedSource)) {
                    // ok (this class was succeeded to compile)
                    return FileVisitResult.CONTINUE;
                }

                Path relativeClass = dir.relativize(file);
                Path expectedClass = toDir.resolve(relativeClass);

                if (Files.exists(expectedClass)) {
                    // ok (this class was failed to compile but class file exists.)
                    return FileVisitResult.CONTINUE;
                }

                // class file exists in original jar file, but source file (or compiled class file) doesn't exist.
                Path relativeOrgClass = dir.relativize(file);
                Path outPath = toDir.resolve(relativeOrgClass);
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

    public void copyErrorClassFilesFromOrgToOut(Path toDir, Set<Path> errorSourceFiles) {
        if (this.dir == null) {
            throw new IllegalStateException("-c options is not set. please set classes directory path.");
        }

        errorSourceFiles.forEach(javaFile -> {
            String className = javaFile.getFileName().toString().replaceAll("\\.java$", "");
            Path classFileDir = this.dir.resolve(javaFile.getParent());

            try (Stream<Path> files = Files.list(classFileDir)) {
                files
                        .filter(file -> file.getFileName().toString().startsWith(className))
                        .forEach(orgClassFile -> {
                            Path classFilePath = this.dir.relativize(orgClassFile);
                            Path outClassFile = toDir.resolve(classFilePath);

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

    private boolean isClassFile(Path file) {
        return file.getFileName().toString().endsWith(".class");
    }

    private boolean isJavaSource(Path file) {
        return file.getFileName().toString().endsWith(".java");
    }
}
