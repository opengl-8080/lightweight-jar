@echo off

set DEPENDENCIES_DIR=sample\build\dependencies\unpackaged
set JAR_FILE=build\libs\lightweight-jar.jar
set COMPRESS_LEVEL=9

java -jar %JAR_FILE% build -a sample\src\main\java -s %DEPENDENCIES_DIR%\source -c %DEPENDENCIES_DIR%\binary --encoding UTF-8 --compress-level %COMPRESS_LEVEL% -m sample.Main -j sample --spring-boot
