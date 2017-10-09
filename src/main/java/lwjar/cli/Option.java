package lwjar.cli;

public enum Option {
    LIBRARY_SOURCE("s", "library-source", true, "A path of library sources directory."),
    LIBRARY_CLASS("c", "library-class", true, "A path of library class files directory."),
    APPLICATION_SOURCE("a", "application-source", true, "A path of application sources directory."),
    MAIN_CLASS("m", "main-class", true, "A main class name."),
    JAR_NAME("j", "jar-name", true, "A base name of output jar file."),
    SPRING_BOOT(null, "spring-boot", false, "Flag of Spring Boot application."),
    OUTPUT("o", "output", true, "A path of output directory. Default is './out'."),
    ENCODING("e", "encoding", true, "The character encoding. Default is depended on an environment."),
    COMPRESS_LEVEL("l", "compress-level", true, "The source code compress level (0, 1, 2, 3, 4). Default is 4."),
    RETRY_COUNT("r", "retry-count", true, "The number of retry to compile. Default is 100."),
    SOURCE("s", "source", true, "A path of sources directory."),
    HELP("h", "help", false, "Print command help.")
    ;
    
    public final String shortName;
    public final String longName;
    public final boolean hasArg;
    public final String description;

    Option(String shortName, String longName, boolean hasArg, String description) {
        this.shortName = shortName;
        this.longName = longName;
        this.hasArg = hasArg;
        this.description = description;
    }
}
