@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Repos\MobList"

powershell -Command ".\gradlew clean build 2>&1 | Tee-Object -FilePath build_log.txt"

pause
