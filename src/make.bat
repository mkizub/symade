@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if exist %OUT_ROOT%\classes rmdir /Q /S %OUT_ROOT%\classes
@mkdir %OUT_ROOT%\classes\stx-fmt

C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes -encoding "UTF-8" -g kiev\gui\event\*.java 
C:\Sun\Java\jdk1.6.0_06\bin\java.exe -ea -verify -Xms128M -Xmx128M -Xfuture -Xnoclassgc -classpath ..\bin\symade-05.jar;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar kiev.Main -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g %*
C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar -d %OUT_ROOT%\classes -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\swt\*.java kiev\gui\swing\*.java
