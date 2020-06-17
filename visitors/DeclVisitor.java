package visitors;
import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;
import types.*;


public class DeclVisitor extends GJDepthFirst<String, String>{


    private HashMap<String,ClassInfo> classST;
    private LinkedHashMap<String,MethodInfo> methodST;

    private ClassInfo  currentClass;
    private MethodInfo currentMethod;

    private boolean methodDeclaration;
    private boolean classExtends;

    private boolean errorFound;

    public DeclVisitor(){
        classST  = new HashMap<String, ClassInfo>();
        methodST = new 
        LinkedHashMap<String, MethodInfo>();

        currentClass  = null;
        currentMethod = null;
        
        errorFound = false;
    }

    public  HashMap<String,ClassInfo> getClassST(){
        return classST;
    }

    public boolean getErrorFound(){
        return errorFound;
    }

    public boolean classExists(String classId ){

        if ( classST.containsKey(classId) == true){
            return true;
        }
        return false;
    }

    /**
     * f0 -> MainClass()
     * f1 -> ( TypeDeclaration() )*
     * f2 -> <EOF>
     */
    public String visit(Goal n, String argu) {
        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        n.f2.accept(this, argu);
        return _ret;
    }

/**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "{"
    * f3 -> "public"
    * f4 -> "static"
    * f5 -> "void"
    * f6 -> "main"
    * f7 -> "("
    * f8 -> "String"
    * f9 -> "["
    * f10 -> "]"
    * f11 -> Identifier()
    * f12 -> ")"
    * f13 -> "{"
    * f14 -> ( VarDeclaration() )*
    * f15 -> ( Statement() )*
    * f16 -> "}"
    * f17 -> "}"
    */
   public String visit(MainClass n, String argu) {
        String _ret=null;
        String classId , paramId;
        classId = n.f1.accept(this, argu);
        paramId = n.f11.accept(this, argu);
      
        ClassInfo cl = new ClassInfo(classId);

        MethodInfo method = new MethodInfo(cl , "main" , "void" , 0);
        method.addParameter(paramId,"String []");
        
        cl.addMethod( "main" , method);        
        
        this.methodST.put( "main" , method);

        this.classST.put( classId , cl);

        this.methodDeclaration = true;
        this.classExtends = false;

        n.f14.accept(this, "main");
        n.f15.accept(this, argu);
      
        return _ret;
   }



    /**
     * f0 -> "class"
     * f1 -> Identifier()
     * f2 -> "{"
     * f3 -> ( VarDeclaration() )*
     * f4 -> ( MethodDeclaration() )*
     * f5 -> "}"
     */
    public String visit(ClassDeclaration n, String argu) {
        String _ret=null;

        String classId ;

        classId = n.f1.accept(this, argu);

        if ( classExists(classId) == true){
            System.out.println("Error: Class " + classId + " already exists");
            errorFound = true;
        }

        ClassInfo cl = new ClassInfo(classId);
        this.classST.put( classId , cl);

        this.methodDeclaration = false;
        this.classExtends = false;

        n.f3.accept(this, classId);
        n.f4.accept(this, classId);

        return _ret;
    }

       /**
    * f0 -> "class"
    * f1 -> Identifier()
    * f2 -> "extends"
    * f3 -> Identifier()
    * f4 -> "{"
    * f5 -> ( VarDeclaration() )*
    * f6 -> ( MethodDeclaration() )*
    * f7 -> "}"
    */
   public String visit(ClassExtendsDeclaration n, String argu) {
        String _ret=null;
        String classId , parentClass;

        classId = n.f1.accept(this, argu);
        parentClass = n.f3.accept(this, argu);

        currentClass = classST.get(parentClass);

        if ( classExists(classId) == true){
            System.out.println("Error: Class " + classId + " already exists");
            errorFound = true;
        }

        ClassInfo cl = new ClassInfo(classId , currentClass , currentClass.getFieldNextOffset() , currentClass.getMethodNextOffset() );
        this.classST.put( classId , cl);

        this.methodDeclaration = false;
        this.classExtends = true;

        n.f5.accept(this, classId);
        n.f6.accept(this, classId);
        return _ret;
    }



    /**
     * f0 -> "public"
     * f1 -> Type()
     * f2 -> Identifier()
     * f3 -> "("
     * f4 -> ( FormalParameterList() )?
     * f5 -> ")"
     * f6 -> "{"
     * f7 -> ( VarDeclaration() )*
     * f8 -> ( Statement() )*
     * f9 -> "return"
     * f10 -> Expression()
     * f11 -> ";"
     * f12 -> "}"
     */
    public String visit(MethodDeclaration n, String argu) {
        String _ret=null;
        String type , methodId ; 

        type = n.f1.accept(this, argu);
        methodId = n.f2.accept(this, argu);

        currentClass = classST.get(argu);
        if ( currentClass.methodExistsInCurrentClass(methodId) != null){
            System.out.println("Error: Method " + type + " " + methodId + " already exists");
            errorFound = true;
        }


        int methodOffset=0;
        boolean virtualMethod = false;
        if (classExtends == true)
        {
            ClassInfo parentClass = currentClass.getParentClass();

            methodOffset = parentClass.searchMethod(methodId);
            if (methodOffset >= 0)
                virtualMethod = true;

        }

        if (virtualMethod == false )
        {
            methodOffset = currentClass.getMethodNextOffset();
            currentClass.setMethodNextOffset(methodOffset+8);
            //System.out.println(argu + "." + methodId + " : " + methodOffset);

        }

        MethodInfo method = new MethodInfo(currentClass , methodId , type , methodOffset);
        currentClass.addMethod( methodId , method);        
        
        this.methodST.put( methodId , method);

        this.methodDeclaration = true;
        n.f4.accept(this, methodId);
    

        n.f7.accept(this, methodId);
        
        n.f8.accept(this, argu);
        n.f10.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String argu) {
        String _ret=null;
        String type , id;

        type = n.f0.accept(this, argu);
        id =n.f1.accept(this, argu);
       
        if ( this.methodDeclaration == false )
        {
            currentClass = classST.get(argu);
            if ( currentClass.fieldExists(id)  == true){
                System.out.println("Error: Field " + id + " already exists");
                errorFound = true;
            }
            
            int fieldOffset = currentClass.getFieldNextOffset();
            if ( type.equals("int"))
                currentClass.setFieldNextOffset(fieldOffset+4);
            else if ( type.equals("boolean"))
                currentClass.setFieldNextOffset(fieldOffset+1);
            else
                currentClass.setFieldNextOffset(fieldOffset+8);

            FieldInfo field = new FieldInfo(currentClass , id , type , fieldOffset);
            currentClass.addField( id , field);
            
            //System.out.println(argu + "." + id + " : " + fieldOffset);
        }

        if ( this.methodDeclaration == true)
        {
            currentMethod = methodST.get(argu);
            if ( currentMethod.localExists(id)  == true){
                System.out.println("Error: Local variable "+ id + " in method "+ currentMethod.getMethodType() + " "+ argu + " already exists");
                errorFound = true;
            }

            currentMethod.addLocal( id , type);
        }

        return _ret;
    }


   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) {
        String _ret=null;
        String id , type;
        type = n.f0.accept(this, argu);
        id = n.f1.accept(this, argu);

        currentMethod = methodST.get(argu);
        if ( currentMethod.parameterExists(id)  == true){
            System.out.println("Error: Parameter " + id + " in method "+ currentMethod.getMethodType() + " "+ argu + " already exists");
            errorFound = true;
        }

        currentMethod.addParameter( id , type);

        
        return _ret;
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, String argu) {
        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n,String argu) {
        return "int[]";
    }



    public String visit(NodeToken n, String argu) { return n.toString(); }

}


