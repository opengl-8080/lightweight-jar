package lwjar.precompile;

import lwjar.GlobalOption;
import lwjar.primitive.Directory;
import lwjar.primitive.ProcessingFile;
import lwjar.primitive.RelativePath;

class LibrarySourceDirectory {
    
    private final Directory directory;

    LibrarySourceDirectory(Directory directory) {
        this.directory = directory;
    }

    void copyTo(PreCompiledDirectory preCompiledDirectory, JavaSourceCompressor javaSourceCompressor) {
        
        this.directory.walkFiles(file -> {
            RelativePath relativePath = this.directory.relativePath(file);
            ProcessingFile outFile = preCompiledDirectory.resolve(relativePath);
            
            if (file.isJavaSource()) {
                String compressedSource = javaSourceCompressor.compress(file);
                outFile.write(compressedSource, GlobalOption.getEncoding());
            } else if (!file.isManifestFile()) {
                file.copyTo(outFile);
            }
        });
    }
}
