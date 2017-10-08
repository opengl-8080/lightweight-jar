package lwjar;

public class Main {
    public static void main(String[] args) {
        try {
            CommandLineOptions options = new CommandLineOptions(args);
            Command command = options.buildCommand();
            command.execute();
        } catch (TooManyCompileErrorException e) {
            System.err.println(e.getMessage());
            System.exit(3);
        } catch (CommandLineOptionException e) {
            e.printErrorMessage();
            System.exit(2);
        } catch (Exception e) {
            e.printStackTrace(System.err);
            System.exit(1);
        }
    }
}
