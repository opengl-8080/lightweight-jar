@echo off

set JAR_FILE=build\libs\lightweight-jar.jar
java -jar %JAR_FILE% package -s out\src --spring-boot --main-class sample.Main --jar-name sample --encoding UTF-8
