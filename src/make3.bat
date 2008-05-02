@if "$%OUT_ROOT%" == "" then set OUT_ROOT="."
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes3\stx-fmt mkdir %OUT_ROOT%\classes3\stx-fmt

c:\java\jdk1.6.0\bin\java -server -ea -classpath %OUT_ROOT%\classes2;..\bin\piccolo.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath %OUT_ROOT%\classes3 -d %OUT_ROOT%\classes3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g %*
