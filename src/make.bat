@if "$%OUT_ROOT%" == "" then set OUT_ROOT="."
@echo OUT_ROOT = %OUT_ROOT%
@if exist %OUT_ROOT%\classes rmdir /Q /S %OUT_ROOT%\classes
@mkdir %OUT_ROOT%\classes\stx-fmt

c:\java\jdk1.6.0\bin\java -ea -verify -Xms320M -Xmx320M -Xfuture -Xnoclassgc -classpath ..\bin\symade-04g.jar;..\bin\piccolo.jar kiev.Main -classpath %OUT_ROOT%\classes -d %OUT_ROOT%\classes -verify -enable vnode -enable view -p k5.prj -prop k5.props -g %*
