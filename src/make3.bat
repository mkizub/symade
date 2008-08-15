@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes3\stx-fmt mkdir %OUT_ROOT%\classes3\stx-fmt

C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes3 -encoding "UTF-8" -g kiev\gui\event\*.java 
C:\Sun\Java\jdk1.6.0_06\bin\java.exe -server -ea -classpath %OUT_ROOT%\classes2;..\bin\xpp3-1.1.4c.jar -Xnoclassgc -Xms128M -Xmx128M kiev.Main -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar;..\bin\swt-win.jar;..\bin\org.eclipse.draw2d.jar;..\bin\org.eclipse.jface_3.3.1.M20070910-0800b.jar;..\bin\org.eclipse.equinox.common_3.3.0.v20070426.jar;..\bin\org.eclipse.core.commands_3.3.0.I20070605-0010.jar -d %OUT_ROOT%\classes3 -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\swt\*.java kiev\gui\swing\*.java
