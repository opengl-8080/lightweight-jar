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
