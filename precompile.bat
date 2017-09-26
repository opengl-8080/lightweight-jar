@echo off

set DEPENDENCIES_DIR=sample\build\dependencies\unpackaged
set JAR_FILE=build\libs\lightweight-jar.jar

java -jar %JAR_FILE% pre-compile -s %DEPENDENCIES_DIR%\source -c %DEPENDENCIES_DIR%\binary --encoding UTF-8
