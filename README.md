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

## 苦悩点
- ソースの収集
    - Gradle の依存関係の解決を利用
- コンパイル時のエラー
    - 推移的な依存関係の解決では解消できない依存関係が存在するする
    - そのまま実行時に参照されると、 NoClassDefError が発生するはず
    - 必要と言われたソースを集めようとすると、手作業でソースを集めてこなければならない
    - エラーになったソースはコンパイルをあきらめ、 class ファイルをそのまま利用
        - インナークラスを含んだものもあるので、単純にソース名と class ファイル名が一対一では対応していない
    - 一回の javac で出力されるエラーファイルの数には限りがあるので、エラー→対象ソースの除去を２０周ほど繰り返す
    - Java EE の API への推移的な依存が全くなく、実際にコンパイルしてエラーが起こったもの（必要なもの）だけを抽出
- 実行時のエラー
    - それでも実行すると NoClassDefError が発生する
    - Spring の一部のモジュールが、バイナリの jar には class ファイルがあるのに、ソースをまとめた jar には対応するソースコードが入っていないものがある
    - バイナリ jar の中にある class ファイルとソース jar ファイルの中にある java ファイルを比較し、 class ファイルしかないものを抽出
        - こちらもインナークラスを考慮しないといけない



