all: compile

compile:
	java -jar jar/jtb132di.jar minijava.jj
	java -jar jar/javacc5.jar minijava-jtb.jj
	javac Main.java types/ClassInfo.java types/MethodInfo.java types/FieldInfo.java visitors/TypeCheckVisitor.java visitors/DeclVisitor.java visitors/GenCodeVisitor.java file/FileController.java

clean:
	rm -f *.class types/*.class visitors/*.class file/*.class  *~
