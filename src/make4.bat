@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes4\stx-fmt mkdir %OUT_ROOT%\classes4\stx-fmt

C:\Sun\Java\jdk1.6.0_06\bin\java.exe -server -ea -Xms128M -Xmx128M -Xnoclassgc -classpath %OUT_ROOT%\classes3;..\bin\xpp3-1.1.4c.jar kiev.Main -classpath %OUT_ROOT%\classes4;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes4 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes4;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes4 -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\event\*.java kiev\gui\swing\*.java
