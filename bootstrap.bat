set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot
@echo JAVA_HOME = %JAVA_HOME%
set OUT_ROOT=buildBootstrap
@echo OUT_ROOT = %OUT_ROOT%
rmdir /s /q %OUT_ROOT%

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath  bin\symade-06.jar;bin\xpp3-1.1.4c.jar kiev.Main -classpath bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\symade1 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath %OUT_ROOT%\symade1;bin\xpp3-1.1.4c.jar kiev.Main -classpath bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\symade2 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd

"%JAVA_HOME%\bin\java" -ea -verify -Xfuture -classpath %OUT_ROOT%\symade2;bin\xpp3-1.1.4c.jar kiev.Main -classpath bin\xpp3-1.1.4c.jar -d %OUT_ROOT%\symade3 -verify -enable vnode -enable view -p k6.prj -prop k6.props -g -target 8 -no-btd

"%JAVA_HOME%\bin\jar" cf symade-core.jar -C %OUT_ROOT%\symade3 .

@rem rmdir /s /q %OUT_ROOT%
