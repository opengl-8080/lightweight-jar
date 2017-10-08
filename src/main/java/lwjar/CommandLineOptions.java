package lwjar;

import lwjar.packaging.PackageCommand;
import lwjar.precompile.PreCompileCommand;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

class CommandLineOptions {
    private final String[] arguments;
    private final List<String> args;

    CommandLineOptions(String[] args) {
        this.args = Arrays.asList(args);
        
        if (this.args.isEmpty()) {
            throw new CommandLineOptionException("please set command 'pre-compile' or 'package'.");
        }

        this.arguments = Arrays.copyOfRange(args, 1, args.length);
    }

    Command buildCommand() {
        String commandName = this.args.get(0);

        if ("pre-compile".equals(commandName)) {
            return this.buildPreCompileCommand();
        } else if ("package".equals(commandName)) {
            return this.buildPackageCommand();
        }

        throw new CommandLineOptionException("unknown command > '" + commandName + "'");
    }

    private PackageCommand buildPackageCommand() {
        Options options = new Options()
                .addRequiredOption("s", "source", true, "A path of sources directory. (required)")
                .addRequiredOption("m", "main-class", true, "A main class name. (required)")
                .addRequiredOption("j", "jar-name", true, "A base name of output jar file. (required)")
                .addOption("o", "output", true, "A path of output directory. Default is './out'.")
                .addOption("e", "encoding", true, "The character encoding. Default is depended on an environment.")
                .addOption(null, "spring-boot", false, "Flag of Spring Boot application.");

        DefaultParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, this.arguments);

            // required
            Path source = this.getRequiredPath(commandLine, "s");
            String mainClass = this.getRequiredString(commandLine, "m");
            String jarName = this.getRequiredString(commandLine, "j");
            // optional
            Path outDir = this.getOptionalPath(commandLine, "o");
            String encoding = this.getOptionalString(commandLine, "e");
            boolean springBoot = this.existsOption(commandLine, "spring-boot");

            return new PackageCommand(encoding, jarName, source, springBoot, mainClass, outDir);
        } catch (ParseException e) {
            throw new CommandLineOptionException("package", options, e.getMessage());
        }
    }

    private PreCompileCommand buildPreCompileCommand() {
        Options options = new Options()
                .addRequiredOption("s", "library-source", true, "A path of library sources directory. (required)")
                .addRequiredOption("c", "library-class", true, "A path of library class files directory. (required)")
                .addRequiredOption("a", "application-source", true, "A path of application sources directory. (required)")
                .addOption("o", "output", true, "A path of output directory. Default is './out'.")
                .addOption("e", "encoding", true, "The character encoding. Default is depended on an environment.")
                .addOption("l", "compress-level", true, "The source code compress level (0, 1, 2, 3, 4). Default is 4.")
                .addOption("r", "retry-count", true, "The number of retry to compile. Default is 100.");

        DefaultParser parser = new DefaultParser();
        try {
            CommandLine commandLine = parser.parse(options, this.arguments);

            // required
            Path librarySourceDir = this.getRequiredPath(commandLine, "s");
            Path applicationSourceDir = this.getRequiredPath(commandLine, "a");
            Path libraryClassDir = this.getRequiredPath(commandLine, "c");
            // optional
            Path outputDir = this.getOptionalPath(commandLine, "o");
            String encoding = this.getOptionalString(commandLine, "e");
            Integer compressLevel = this.getOptionalInteger(commandLine, "l");
            Integer retryCount = this.getOptionalInteger(commandLine, "r");

            return new PreCompileCommand(encoding, applicationSourceDir, librarySourceDir, libraryClassDir, outputDir, compressLevel, retryCount);
        } catch (ParseException e) {
            throw new CommandLineOptionException("pre-compile", options, e.getMessage());
        }
    }

    private Path getRequiredPath(CommandLine commandLine, String optionName) {
        return Paths.get(commandLine.getOptionValue(optionName));
    }

    private Path getOptionalPath(CommandLine commandLine, String optionName) {
        return commandLine.hasOption(optionName) ? Paths.get(commandLine.getOptionValue(optionName)) : null;
    }

    private Integer getOptionalInteger(CommandLine commandLine, String optionName) {
        return commandLine.hasOption(optionName) ? Integer.valueOf(commandLine.getOptionValue(optionName)) : null;
    }

    private String getRequiredString(CommandLine commandLine, String optionName) {
        return commandLine.getOptionValue(optionName);
    }

    private String getOptionalString(CommandLine commandLine, String optionName) {
        return commandLine.hasOption(optionName) ? commandLine.getOptionValue(optionName) : null;
    }

    private boolean existsOption(CommandLine commandLine, String optionName) {
        return commandLine.hasOption(optionName);
    }
}