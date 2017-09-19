package lwjar;

import lwjar.precompile.PreCompileCommand;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class CommandLineOptions {
    private final List<String> args;

    public CommandLineOptions(String[] args) {
        this.args = Arrays.asList(args);
    }

    public Command buildCommand() {
        if (this.args.isEmpty()) {
            throw new CommandLineOptionException("please set command 'pre-compile' or 'package'.");
        }
        
        String commandName = this.args.get(0);
        
        if ("pre-compile".equals(commandName)) {
            return this.buildPreCompileCommand();
        } else if ("package".equals(commandName)) {
            return this.buildPackageCommand();
        }

        throw new CommandLineOptionException("unknown command > '" + commandName + "'");
    }

    private PackageCommand buildPackageCommand() {
        String jarBase = null;
        String encoding = null;
        Path srcDir = null;
        boolean springBoot = false;
        String mainClass = null;
        Path outDir = null;

        Iterator<String> ite = this.args.iterator();
        ite.next(); // skip command token.
        
        while (ite.hasNext()) {
            String arg = ite.next();
            
            if ("--spring-boot".equals(arg)) {
                springBoot = true;
            } else if ("-s".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'-s' needs source directory path.");
                }
                srcDir = Paths.get(ite.next());
            } else if ("--main-class".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'--main-class' needs Main Class FQCN.");
                }
                mainClass = ite.next();
            } else if ("--encoding".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'--encoding' needs charset name of source code.");
                }
                encoding = ite.next();
            } else if ("-o".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'-o' needs output directory path.");
                }
                outDir = Paths.get(ite.next());
            } else if ("--jar-base".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'--jar-base' needs output jar name.");
                }
                jarBase = ite.next();
            } else {
                throw new CommandLineOptionException("'package' command accepts options '-s', '--main-class' and '--spring-boot'. But you set unknown option > '" + arg + "'.");
            }
        }
        
        if (srcDir == null) {
            throw new CommandLineOptionException("'-s' is required. Set source files directory path.");
        }
        
        if (mainClass == null) {
            throw new CommandLineOptionException("'--main-class' is required. Set main class FQCN.");
        }
        
        if (jarBase == null) {
            throw new CommandLineOptionException("'--jar-base' is required. Set output jar file name.");
        }
        
        return new PackageCommand(encoding, jarBase, srcDir, springBoot, mainClass, outDir);
    }
    
    private PreCompileCommand buildPreCompileCommand() {
        String encoding = null;
        Path sourceDir = null;
        Path classesDir = null;
        Path outDir = null;
        Integer compressLevel = null;

        Iterator<String> ite = this.args.iterator();
        ite.next(); // skip command token.

        while (ite.hasNext()) {
            String arg = ite.next();
            if ("-s".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'-s' needs source directory path.");
                }
                sourceDir = Paths.get(ite.next());
            } else if ("-c".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'-c' needs classes directory path.");
                }
                classesDir = Paths.get(ite.next());
            } else if ("-o".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'-o' needs output directory path.");
                }
                outDir = Paths.get(ite.next());
            } else if ("--encoding".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'--encoding' needs charset name of source code.");
                }
                encoding = ite.next();
            } else if ("--compress-level".equals(arg)) {
                if (!ite.hasNext()) {
                    throw new CommandLineOptionException("'--compress-level' needs number of level (0, 1, 2, 3, ...).");
                }
                String strCompressLevel = ite.next();
                try {
                    compressLevel = Integer.valueOf(strCompressLevel);
                } catch (NumberFormatException e) {
                    throw new CommandLineOptionException("--compress-level (" + strCompressLevel + ") must be number.");
                }
            } else {
                throw new CommandLineOptionException("'pre-compile' command accepts options '-s', '-c' and '-o'. But you set unknown option > '" + arg + "'.");
            }
        }

        if (sourceDir == null) {
            throw new CommandLineOptionException("'-s' option is required. Set source files directory path.");
        }

        return new PreCompileCommand(encoding, sourceDir, classesDir, outDir, compressLevel);
    }
}