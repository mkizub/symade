@if not defined JAVA_HOME set JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-8.0.412.8-hotspot
@echo JAVA_HOME = %JAVA_HOME%
@if not defined ANT_HOME set ANT_HOME=\Java\apache-ant-1.7.1
@echo ANT_HOME = %ANT_HOME%
@if not defined OUT_ROOT set OUT_ROOT=%TEMP%
@echo OUT_ROOT = %OUT_ROOT%

"%JAVA_HOME%\bin\java.exe" -server -ea -classpath %OUT_ROOT%\classes-ant;%OUT_ROOT%\classes2;%ANT_HOME%\lib\ant.jar -Xnoclassgc -Xms128M -Xmx128M kiev.Main -d %OUT_ROOT%\classes-ant -verify -enable vnode -enable view -p ant.prj -prop k5x.props -g %*
