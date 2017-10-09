package lwjar.cli;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

import java.io.IOException;
import java.util.Objects;

public class HelpCommand implements Command {
    private final String commandName;
    private final Options options;

    HelpCommand(String commandName, Options options) {
        this.commandName = Objects.requireNonNull(commandName);
        this.options = Objects.requireNonNull(options);
    }

    @Override
    public void execute() throws IOException {
        HelpFormatter help = new HelpFormatter();
        help.printHelp("java -jar lightweight-jar.jar " + this.commandName, this.options);
    }
}
