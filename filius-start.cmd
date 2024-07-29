echo "Filius starten"
SET workdir=%cd%
set PATH=%cd%\java-runtime\bin
set JAVA_HOME=C:\tools\filius-master\java-runtime
echo %PATH%
java -version
java -jar filius-master.jar
