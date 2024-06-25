@if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
@echo JAVA_HOME = %JAVA_HOME%
@if not defined OUT_ROOT set OUT_ROOT=%TEMP%
@echo OUT_ROOT = %OUT_ROOT%
@if exist %OUT_ROOT%\classes rmdir /Q /S %OUT_ROOT%\classes
@mkdir %OUT_ROOT%\classes\stx-fmt

rem "%JAVA_HOME%\bin\javac.exe" -classpath ..\bin\symade-05.jar;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\event\*.java kiev\gui\swt\*.java kiev\gui\swing\*.java kiev\dump\*.java kiev\dump\xml\*.java kiev\dump\bin\*.java
"%JAVA_HOME%\bin\java.exe" -ea -verify -Xms512M -Xmx512M -Xfuture -Xnoclassgc -classpath ..\bin\symade-05.jar;..\bin\xpp3-1.1.4c.jar kiev.Main -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g %*
