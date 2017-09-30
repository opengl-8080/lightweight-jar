package lwjar.precompile;

import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

import java.nio.file.Path;
import java.util.Objects;

class UncompilableJavaSource {
    private final PreCompiledDirectory baseDirectory;
    private final ProcessingFile file;

    UncompilableJavaSource(PreCompiledDirectory baseDirectory, ProcessingFile file) {
        this.baseDirectory = Objects.requireNonNull(baseDirectory);
        this.file = Objects.requireNonNull(file);
    }

    String getClassName() {
        return this.file.getName().replaceAll("\\.java", "");
    }

    RelativePath getPackagePath() {
        RelativePath relativePath = this.baseDirectory.relative(this.file);
        Path packagePath = relativePath.getPath().getParent();

        return new RelativePath(packagePath);
    }

    public void delete() {
        this.file.delete();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UncompilableJavaSource that = (UncompilableJavaSource) o;

        if (!baseDirectory.equals(that.baseDirectory)) return false;
        return file.equals(that.file);
    }

    @Override
    public int hashCode() {
        int result = baseDirectory.hashCode();
        result = 31 * result + file.hashCode();
        return result;
    }
}
