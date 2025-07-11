@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-21
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "C:\Users\Pep\Desktop\Game Dev\Minecraft\Fabric\moblist-template-1.21.4"
gradlew build
pause
