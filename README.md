# lightweight-jar

```bash
# collect dependencies
$ cd sample

$ gradle collectDependencies
```

```bash
# build lightweight.jar
$ gradle fatJar

# build sample.jar
$ java -jar build/libs/lightweight-jar.jar build -a sample/src/main/java -s sample/build/dependencies/unpackaged/source -c sample/build/dependencies/unpackaged/binary --encoding UTF-8 -m sample.Main -j sample --spring-boot

# help
$ java -jar build/libs/lightweight-jar.jar build --help
usage: java -jar lightweight-jar.jar build
 -a,--application-source <arg>   A path of application sources directory.
                                 (REQUIRED)
 -c,--library-class <arg>        A path of library class files directory.
                                 (REQUIRED)
 -e,--encoding <arg>             The character encoding. Default is
                                 depended on an environment.
 -h,--help                       Print command help.
 -j,--jar-name <arg>             A base name of output jar file.
                                 (REQUIRED)
 -l,--compress-level <arg>       The source code compress level (0, 1, 2,
                                 3, 4). Default is 4.
 -m,--main-class <arg>           A main class name. (REQUIRED)
 -o,--output <arg>               A path of output directory. Default is
                                 './out'.
 -r,--retry-count <arg>          The number of retry to compile. Default
                                 is 100.
 -s,--library-source <arg>       A path of library sources directory.
                                 (REQUIRED)
    --spring-boot                Flag of Spring Boot application.
```
