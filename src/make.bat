@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if exist %OUT_ROOT%\classes rmdir /Q /S %OUT_ROOT%\classes
@mkdir %OUT_ROOT%\classes\stx-fmt

C:\Sun\Java\jdk1.6.0_06\bin\java.exe -ea -verify -Xms128M -Xmx128M -Xfuture -Xnoclassgc -classpath ..\bin\symade-05.jar;..\bin\xpp3-1.1.4c.jar kiev.Main -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g %*
@rem C:\Sun\Java\jdk1.6.0_06\bin\javac -classpath %OUT_ROOT%\classes;..\bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\classes -encoding "UTF-8" -g kiev\gui\*.java kiev\gui\event\*.java kiev\gui\swing\*.java
