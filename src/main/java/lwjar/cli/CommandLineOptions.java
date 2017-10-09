package lwjar.cli;

import lwjar.GlobalOption;
import lwjar.OutputDirectory;
import lwjar.build.BuildCommand;
import lwjar.packaging.ApplicationMainClass;
import lwjar.packaging.JarName;
import lwjar.packaging.PackageCommand;
import lwjar.packaging.SourceDirectory;
import lwjar.precompile.ApplicationSourceDirectory;
import lwjar.precompile.JavaSourceCompressor;
import lwjar.precompile.LibraryClassDirectory;
import lwjar.precompile.LibrarySourceDirectory;
import lwjar.precompile.PreCompileCommand;
import lwjar.primitive.Directory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

public class CommandLineOptions {
    private final String commandName;
    private final String[] arguments;

    public CommandLineOptions(String[] args) {
        if (args.length == 0) {
            throw new CommandLineOptionException("please set command 'pre-compile' or 'package' or 'build'.");
        }

        this.commandName = args[0];
        this.arguments = Arrays.copyOfRange(args, 1, args.length);
    }

    public Command buildCommand() {
        if ("pre-compile".equals(this.commandName)) {
            return this.buildPreCompileCommand();
        } else if ("package".equals(this.commandName)) {
            return this.buildPackageCommand();
        } else if ("build".equals(this.commandName)) {
            return this.buildBuildCommand();
        }

        throw new CommandLineOptionException("unknown command > '" + this.commandName + "'");
    }
    
    private Command buildBuildCommand() {
        Options options = new OptionsBuilder()
                .required(Option.LIBRARY_SOURCE)
                .required(Option.LIBRARY_CLASS)
                .required(Option.APPLICATION_SOURCE)
                .required(Option.MAIN_CLASS)
                .required(Option.JAR_NAME)
                .optional(Option.SPRING_BOOT)
                .optional(Option.OUTPUT)
                .optional(Option.ENCODING)
                .optional(Option.COMPRESS_LEVEL)
                .optional(Option.RETRY_COUNT)
                .optional(Option.HELP)
                .build();
        
        if (this.hasHelp()) {
            return new HelpCommand("build", options);
        }
        
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, this.arguments);

            // required
            LibrarySourceDirectory librarySourceDirectory = this.getLibrarySourceDirectory(commandLine);
            ApplicationSourceDirectory applicationSourceDirectory = this.getApplicationSourceDirectory(commandLine);
            LibraryClassDirectory libraryClassDirectory = this.getLibraryClassDirectory(commandLine);
            ApplicationMainClass applicationMainClass = this.getApplicationMainClass(commandLine);
            JarName jarName = this.getJarName(commandLine);
            
            // optional
            OutputDirectory outputDirectory = this.getOutputDirectory(commandLine);
            JavaSourceCompressor.CompressLevel compressLevel = this.getCompressLevel(commandLine);
            Integer retryCount = this.getRetryCount(commandLine);
            boolean springBoot = this.getSpringBoot(commandLine);

            GlobalOption.setEncoding(this.getEncoding(commandLine));
            
            return new BuildCommand(
                librarySourceDirectory,
                libraryClassDirectory,
                applicationSourceDirectory,
                applicationMainClass,
                jarName,
                outputDirectory,
                compressLevel,
                retryCount,
                springBoot
            );
        } catch (ParseException e) {
            throw new CommandLineOptionException("build", options, e.getMessage());
        }
    }

    private Command buildPackageCommand() {
        Options options = new OptionsBuilder()
                .required(Option.SOURCE)
                .required(Option.MAIN_CLASS)
                .required(Option.JAR_NAME)
                .optional(Option.OUTPUT)
                .optional(Option.ENCODING)
                .optional(Option.SPRING_BOOT)
                .optional(Option.HELP)
                .build();

        if (this.hasHelp()) {
            return new HelpCommand("package", options);
        }

        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, this.arguments);
            
            // required
            SourceDirectory sourceDirectory = this.getSourceDirectory(commandLine);
            ApplicationMainClass applicationMainClass = this.getApplicationMainClass(commandLine);
            JarName jarName = this.getJarName(commandLine);
            // optional
            OutputDirectory outputDirectory = this.getOutputDirectory(commandLine);
            boolean springBoot = this.getSpringBoot(commandLine);

            GlobalOption.setEncoding(this.getEncoding(commandLine));
            
            return new PackageCommand(
                sourceDirectory,
                applicationMainClass,
                outputDirectory,
                jarName,
                springBoot
            );
        } catch (ParseException e) {
            throw new CommandLineOptionException("package", options, e.getMessage());
        }
    }
    

    private Command buildPreCompileCommand() {
        Options options = new OptionsBuilder()
                .required(Option.LIBRARY_SOURCE)
                .required(Option.LIBRARY_CLASS)
                .required(Option.APPLICATION_SOURCE)
                .optional(Option.OUTPUT)
                .optional(Option.ENCODING)
                .optional(Option.COMPRESS_LEVEL)
                .optional(Option.RETRY_COUNT)
                .optional(Option.HELP)
                .build();

        if (this.hasHelp()) {
            return new HelpCommand("pre-compile", options);
        }
        
        try {
            DefaultParser parser = new DefaultParser();
            CommandLine commandLine = parser.parse(options, this.arguments);
            
            // required
            LibrarySourceDirectory librarySourceDirectory = this.getLibrarySourceDirectory(commandLine);
            LibraryClassDirectory libraryClassDirectory = this.getLibraryClassDirectory(commandLine);
            ApplicationSourceDirectory applicationSourceDirectory = this.getApplicationSourceDirectory(commandLine);
            // optional
            OutputDirectory outputDirectory = this.getOutputDirectory(commandLine);
            JavaSourceCompressor.CompressLevel compressLevel = this.getCompressLevel(commandLine);
            Integer retryCount = this.getRetryCount(commandLine);
            
            GlobalOption.setEncoding(this.getEncoding(commandLine));

            return new PreCompileCommand(
                librarySourceDirectory,
                libraryClassDirectory,
                applicationSourceDirectory,
                outputDirectory,
                compressLevel,
                retryCount
            );
        } catch (ParseException e) {
            throw new CommandLineOptionException("pre-compile", options, e.getMessage());
        }
    }


    private boolean hasHelp() {
        Options options = new OptionsBuilder().optional(Option.HELP).build();
        try {
            CommandLine commandLine = new DefaultParser().parse(options, this.arguments, true);
            return this.existsOption(commandLine, Option.HELP.shortName);
        } catch (ParseException e) {
            throw new CommandLineOptionException(e.getMessage());
        }
    }

    private LibrarySourceDirectory getLibrarySourceDirectory(CommandLine commandLine) {
        Path path = this.getRequiredPath(commandLine, Option.LIBRARY_SOURCE.shortName);
        return new LibrarySourceDirectory(new Directory(path));
    }

    private ApplicationSourceDirectory getApplicationSourceDirectory(CommandLine commandLine) {
        Path path = this.getRequiredPath(commandLine, Option.APPLICATION_SOURCE.shortName);
        return new ApplicationSourceDirectory(new Directory(path));
    }

    private LibraryClassDirectory getLibraryClassDirectory(CommandLine commandLine) {
        Path path = this.getRequiredPath(commandLine, Option.LIBRARY_CLASS.shortName);
        return new LibraryClassDirectory(new Directory(path));
    }

    private ApplicationMainClass getApplicationMainClass(CommandLine commandLine) {
        String mainClass = this.getRequiredString(commandLine, Option.MAIN_CLASS.shortName);
        return new ApplicationMainClass(mainClass);
    }

    private JarName getJarName(CommandLine commandLine) {
        String jarName = this.getRequiredString(commandLine, Option.JAR_NAME.shortName);
        return new JarName(jarName);
    }

    private OutputDirectory getOutputDirectory(CommandLine commandLine) {
        Path path = this.getOptionalPath(commandLine, Option.OUTPUT.shortName);
        return new OutputDirectory(path);
    }

    private Charset getEncoding(CommandLine commandLine) {
        String encoding = this.getOptionalString(commandLine, Option.ENCODING.shortName);
        return encoding == null ? Charset.defaultCharset() : Charset.forName(encoding);
    }

    private JavaSourceCompressor.CompressLevel getCompressLevel(CommandLine commandLine) {
        Integer compressLevel = this.getOptionalInteger(commandLine, Option.COMPRESS_LEVEL.shortName);
        return JavaSourceCompressor.CompressLevel.valueOf(compressLevel);
    }

    private SourceDirectory getSourceDirectory(CommandLine commandLine) {
        Path path = this.getRequiredPath(commandLine, Option.SOURCE.shortName);
        return new SourceDirectory(new Directory(path));
    }

    private boolean getSpringBoot(CommandLine commandLine) {
        return this.existsOption(commandLine, Option.SPRING_BOOT.longName);
    }
    
    private Integer getRetryCount(CommandLine commandLine) {
        return this.getOptionalInteger(commandLine, Option.RETRY_COUNT.shortName);
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