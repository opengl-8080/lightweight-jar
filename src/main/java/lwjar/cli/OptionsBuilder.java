package lwjar.cli;

import org.apache.commons.cli.Options;

public class OptionsBuilder {
    
    private Options options = new Options();
    
    public OptionsBuilder required(Option option) {
        this.options.addRequiredOption(
            option.shortName,
            option.longName,
            option.hasArg,
            option.description + " (REQUIRED)"
        );
        return this;
    }

    public OptionsBuilder optional(Option option) {
        this.options.addOption(
            option.shortName,
            option.longName,
            option.hasArg,
            option.description
        );
        return this;
    }
    
    public Options build() {
        return this.options;
    }
}
