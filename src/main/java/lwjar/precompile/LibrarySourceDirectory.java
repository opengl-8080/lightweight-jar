package lwjar.precompile;

import lwjar.GlobalOption;

public class LibrarySourceDirectory {
    
    private final Directory directory;

    public LibrarySourceDirectory(Directory directory) {
        this.directory = directory;
    }

    public void copyTo(PreCompiledDirectory preCompiledDirectory, JavaSourceCompressor javaSourceCompressor) {
        
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
