@if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Java\jdk1.6.0_45
@echo JAVA_HOME = %JAVA_HOME%
@if not defined OUT_ROOT set OUT_ROOT=%TEMP%
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes2\stx-fmt mkdir %OUT_ROOT%\classes2\stx-fmt

%JAVA_HOME%\bin\java.exe -server -ea -classpath ..\bin\symade-05.jar;..\bin\xpp3-1.1.4c.jar -Xnoclassgc -Xms128M -Xmx128M kiev.Main -d %OUT_ROOT%\classes2 -verify -enable vnode -enable view -p demo.prj -prop k5x.props -g -v -gui:swing %*
