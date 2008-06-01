@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes4\stx-fmt mkdir %OUT_ROOT%\classes4\stx-fmt

c:\java\jdk1.6.0\bin\java -server -ea -Xms128M -Xmx128M -Xnoclassgc -classpath %OUT_ROOT%\classes3;..\bin\piccolo.jar kiev.Main -classpath %OUT_ROOT%\classes4 -d %OUT_ROOT%\classes4 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
