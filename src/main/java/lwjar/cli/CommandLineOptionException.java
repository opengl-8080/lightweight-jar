package lwjar.cli;

import org.apache.commons.cli.Options;

import java.io.IOException;
import java.io.UncheckedIOException;

public class CommandLineOptionException extends RuntimeException {
    private final Options options;
    private final String command;

    CommandLineOptionException(String message) {
        this(null, null, message);
    }
    
    CommandLineOptionException(String command, Options options, String message) {
        super(message);
        this.options = options;
        this.command = command;
    }
    
    public void printErrorMessage() {
        System.err.println(this.getMessage());
        System.err.println();
        
        if (this.options != null) {
            HelpCommand help = new HelpCommand(this.command, this.options);
            try {
                help.execute();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }
    }
}
