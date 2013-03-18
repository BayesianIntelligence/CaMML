JVM_OPTS="-Xmx512m -Dnetica.reg=$NETICAREG"
JAVA=java
CAMML_HOME=`pwd`
for JAR in $CAMML_HOME/jar/*.jar; do JARS=$JARS:$JAR; done

if $JAVA -cp Camml camml.core.Has64; then
	echo Running 64 bit version
	JARS=$JARS:$CAMML_HOME/jar/NeticaJ-Linux/x64/NeticaJ.jar
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CAMML_HOME/jar/NeticaJ-Linux/x64
	$JAVA $JVM_OPTS -classpath $CAMML_HOME/Camml:$JARS camml.plugin.weka.CaMMLClassifier $*
else
	echo Running 32 bit version
	JARS=$JARS:$CAMML_HOME/jar/NeticaJ-Linux/NeticaJ.jar
	export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$CAMML_HOME/jar/NeticaJ-Linux
	$JAVA $JVM_OPTS -classpath $CAMML_HOME/Camml:$JARS camml.plugin.weka.CaMMLClassifier $*
fi