@echo off

set SAMPLE_OUT_DIR=out\src\sample
mkdir %SAMPLE_OUT_DIR%
copy sample\src\main\java\sample\*.java %SAMPLE_OUT_DIR%

set JAR_FILE=build\libs\lightweight-jar.jar
java -jar %JAR_FILE% package -s out\src --spring-boot --main-class sample.Main --jar-base sample --encoding UTF-8
