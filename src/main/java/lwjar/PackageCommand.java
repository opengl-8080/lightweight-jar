package lwjar;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

public class PackageCommand implements Command {
    
    private final Path srcDir;
    private final boolean springBoot;
    private final Optional<String> mainClass;

    public PackageCommand(Path srcDir, boolean springBoot, String mainClass) {
        this.srcDir = srcDir;
        this.springBoot = springBoot;
        this.mainClass = Optional.ofNullable(mainClass);
    }

    @Override
    public void execute() throws IOException {
        
    }
}
