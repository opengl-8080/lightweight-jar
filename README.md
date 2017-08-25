# lightweight-jar

```groovy
import groovy.io.FileType
task('collectSource') {
    configurations.runtime.each { jarFile ->
        jarFile.parentFile.parentFile.eachFileRecurse(FileType.FILES) { file ->
            if (file.name =~ /^.*\.jar$/ && file.name.contains('source')) {
                copy {
                    from file
                    into "$buildDir/libs"
                }
            }
        }
    }
}
```

## pre compile comamnd image
```bash
$ java -jar lightweight-jar.jar pre-compile -s to/src/dir -c to/classes/dir
compiling...

<output error source file paths (absolute path to use 'rm' command) ...>

--- or ---

completion!!
```



