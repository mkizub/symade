@if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
@echo JAVA_HOME = %JAVA_HOME%
@if not defined OUT_ROOT set OUT_ROOT=%TEMP%
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes3\stx-fmt mkdir %OUT_ROOT%\classes3\stx-fmt

"%JAVA_HOME%\bin\java.exe" -server -ea -classpath %OUT_ROOT%\classes2;..\bin\xpp3-1.1.4c.jar -Xnoclassgc -Xms512M -Xmx512M kiev.Main -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
"%JAVA_HOME%\bin\javac" -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes3 -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\event\*.java kiev\gui\swt\*.java kiev\gui\swing\*.java kiev\dump\*.java kiev\dump\xml\*.java
