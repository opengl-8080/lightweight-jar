package lwjar;

import java.nio.file.Path;

public class PreCompileCommand implements Command {

    private final Path sourceFileDir;
    private final Path classesFileDir;
    private final Path outputDir;

    public PreCompileCommand(Path sourceFileDir, Path classesFileDir, Path outputDir) {
        this.sourceFileDir = sourceFileDir;
        this.classesFileDir = classesFileDir;
        this.outputDir = outputDir;
    }

    @Override
    public void execute() {
        System.out.println(sourceFileDir);
        System.out.println(classesFileDir);
        System.out.println(outputDir);
    }
}
