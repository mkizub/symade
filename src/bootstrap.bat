set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot
@echo JAVA_HOME = %JAVA_HOME%
set OUT_ROOT=build
@echo OUT_ROOT = %OUT_ROOT%
@if not exist %OUT_ROOT%\classes2\stx-fmt mkdir %OUT_ROOT%\classes2\stx-fmt
rmdir /s /q %OUT_ROOT%\symade1
rmdir /s /q %OUT_ROOT%\symade2
rmdir /s /q %OUT_ROOT%\symade3

"%JAVA_HOME%/bin/java" -ea -verify -Xms256M -Xmx256M -Xfuture -Xnoclassgc -classpath ../bin/symade-06.jar;../bin/xpp3-1.1.4c.jar kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d %OUT_ROOT%/symade1 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -target 8

"%JAVA_HOME%/bin/java" -server -ea -classpath %OUT_ROOT%/symade1;../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d %OUT_ROOT%/symade2 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -target 8

"%JAVA_HOME%/bin/java" -server -ea -classpath %OUT_ROOT%/symade2;../bin/xpp3-1.1.4c.jar -Xnoclassgc -Xms256M -Xmx256M kiev.Main -classpath ../bin/xpp3-1.1.4c.jar -d %OUT_ROOT%/symade3 -verify -enable vnode -enable view -p k5x.prj -prop k5x.props -g -target 8

"%JAVA_HOME%/bin/jar" cf symade-core.jar -C %OUT_ROOT%/symade3 .
rmdir /s /q %OUT_ROOT%\symade1
rmdir /s /q %OUT_ROOT%\symade2
rmdir /s /q %OUT_ROOT%\symade3
