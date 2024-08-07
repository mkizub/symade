@echo echo Stage 1

@if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot
@echo JAVA_HOME = %JAVA_HOME%
@if not defined OUT_ROOT set OUT_ROOT=%~dp0\buildCustom
@echo OUT_ROOT = %OUT_ROOT%

@if exist %OUT_ROOT%\classes1 rmdir /Q /S %OUT_ROOT%\classes1
@mkdir %OUT_ROOT%\classes1\stx-fmt

"%JAVA_HOME%\bin\java.exe" -ea -verify -Xms512M -Xmx512M -Xfuture -Xnoclassgc -classpath bin\symade-06.jar kiev.Main -classpath %OUT_ROOT%\classes1;bin\swt-win.jar -d %OUT_ROOT%\classes1 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 %*
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
@echo Kiev Compiler Exit code: %errorlevel%
@rem  javac kiev\dump\*.java kiev\dump\xml\*.java
"%JAVA_HOME%\bin\javac" -classpath %OUT_ROOT%\classes1;bin\swt-win.jar;bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes1 -encoding "UTF-8" -g symade-gui\src\main\java\kiev\gui\*.java symade-gui\src\main\java\kiev\gui\event\*.java symade-gui\src\main\java\kiev\gui\swt\*.java symade-gui\src\main\java\kiev\gui\swing\*.java
@if %errorlevel% NEQ 0 (exit /b %errorlevel%)
@echo Javac exit code: %errorlevel%

@echo echo Stage 1 PASS
