REM Batch file to run CaMML under windows
path=%path%;lib
echo %path%
java -Xmx512m -Djava.library.path=lib -classpath Camml;jar\cdms.jar;jar\NeticaJ.jar;jar\junit.jar;jar\weka.jar;.\;jar\tetrad4.jar camml.core.newgui.RunGUI
