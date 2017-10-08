package lwjar;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;

class CommandLineOptionException extends RuntimeException {
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
    
    void printErrorMessage() {
        System.err.println(this.getMessage());
        System.err.println();
        
        if (this.options != null) {
            HelpFormatter help = new HelpFormatter();
            help.printHelp("java -jar lightweight-jar.jar " + command, this.options);
        }
    }
}
