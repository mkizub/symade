@if not defined OUT_ROOT set OUT_ROOT=.
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes2\stx-fmt mkdir %OUT_ROOT%\classes2\stx-fmt

c:\java\jdk1.6.0\bin\java -server -ea -classpath %OUT_ROOT%\classes;..\bin\piccolo.jar -Xnoclassgc -Xms128M -Xmx128M kiev.Main -classpath %OUT_ROOT%\classes2 -d %OUT_ROOT%\classes2 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
