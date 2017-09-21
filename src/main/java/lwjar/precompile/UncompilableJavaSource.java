package lwjar.precompile;

import java.nio.file.Path;

public class UncompilableJavaSource extends RelativePath {
    public UncompilableJavaSource(Path relativePath) {
        super(relativePath);
    }

    public String getClassName() {
        return this.getName().replaceAll("\\.java", "");
    }

    public RelativePath getParentDir() {
        return new RelativePath(this.getPath().getParent());
    }
}
