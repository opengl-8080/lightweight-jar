package lwjar.precompile;

import lwjar.primitive.RelativePath;

import java.nio.file.Path;

class UncompilableJavaSource extends RelativePath {
    UncompilableJavaSource(Path relativePath) {
        super(relativePath);
    }

    String getClassName() {
        return this.getName().replaceAll("\\.java", "");
    }

    RelativePath getParentDir() {
        return new RelativePath(this.getPath().getParent());
    }
}
