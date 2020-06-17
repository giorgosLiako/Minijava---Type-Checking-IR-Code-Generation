package visitors;
import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;
import types.*;

public class TypeCheckVisitor extends GJDepthFirst<String, String>{

    private HashMap<String,ClassInfo> classST;

    private ClassInfo  currentClass;
    private MethodInfo currentMethod;

    private boolean thisUsed;
    private boolean errorFound;

    private Stack<LinkedList<String>> CallStack;

    public TypeCheckVisitor(HashMap<String,ClassInfo> classST){
        this.classST  = classST;

        thisUsed = false;
        errorFound = false;
        currentClass = null;
        currentMethod = null;
        CallStack = new  Stack<LinkedList<String>>();
    }

    public boolean getErrorFound(){
        return errorFound;
    }

    public boolean checkSuperClass(String methodParamType , String callParamType){
        ClassInfo childClass = classST.get(callParamType);
        if (childClass != null)
        {   String classId = childClass.getClassId() ;
            if ( classId.equals(methodParamType) )
            {
                return true;
            }
            else if (childClass.getParentClass() != null)
            {
                return checkSuperClass(methodParamType , childClass.getParentClass().getClassId());
            }
        }
        return false;
    }

    public ClassInfo classExists(String id){
        if ( this.classST.containsKey(id) == true){
            return this.classST.get(id) ;
        }
        System.out.println("Error: Class " + id + " not exists");
        errorFound = true;

        return null;
    }

    public String varLookup(String id){
        String type;
        if (currentMethod != null){
            
            type = currentMethod.getParameterType(id);
            if (type != null)
                return type;

            type = currentMethod.getLocalType(id);
            if (type != null)
                return type;
        }

        if (currentClass != null){
            type = currentClass.getFieldType(id);
            if (type != null)
                return type;
        }
        if (currentClass != null)
        {   ClassInfo parent = currentClass.getParentClass() ;
            if ( parent != null){
                ClassInfo temp = currentClass ;
                currentClass = parent;
                type = this.varLookup(id);

                currentClass = temp;
                if(type != null)
                    return type;

            }
        }
        return null;

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
        String classId ;
        
        // if (errorFound == true )
        //     return null;

        classId = n.f1.accept(this, "class");
        currentClass = classExists(classId);
        currentMethod = currentClass.methodExists("main");

        n.f11.accept(this, "method");
       
        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        currentClass = null;
        currentMethod = null;
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
        // if (errorFound == true )
        //     return null;
        
        String _ret=null;
        String classId;
        classId = n.f1.accept(this, "class") ;
        currentClass = classExists(classId);

        n.f3.accept(this, classId);
        n.f4.accept(this, classId);
        currentClass = null;

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
        // if (errorFound == true )
        //     return null;
        String _ret=null;
        String classId , parentClassId;
        classId = n.f1.accept(this, "class");
        
        parentClassId = n.f3.accept(this, "class");
        currentClass = classExists(classId);
        
        n.f5.accept(this, classId);
        n.f6.accept(this, classId);
       
        currentClass = null;
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
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String methodId , methodType , returnType;
        methodType = n.f1.accept(this, argu);
        methodId = n.f2.accept(this, "method");

        currentMethod = currentClass.methodExists(methodId);
        if (currentMethod == null)
        {
            System.out.println("Error: Something really bad happened."  );
            errorFound = true;            
        }
        ClassInfo parent =  currentClass.getParentClass();
        if ( parent != null)
        {   
            MethodInfo methodInCurrentClass = currentClass.methodExistsInCurrentClass(methodId);
            MethodInfo methodInParentClass  = parent.methodExists(methodId);

            if (methodInCurrentClass != null && methodInParentClass != null)
            {
                LinkedHashMap<String, String> currentParameters = methodInCurrentClass.getParametersList() ;
                LinkedHashMap<String, String> parentParameters = methodInParentClass.getParametersList() ;
                    
                
                if ( ! (methodInCurrentClass.getMethodType()).equals(methodInParentClass.getMethodType()) )
                {
                    System.out.println("Error: In class " + currentClass.getClassId() +" tried to overload function "+ methodType + " " + methodId + " ."  );
                    errorFound = true;  
                }

                
                int sizeCallParam = currentParameters.size();
                int sizeMethodParam = parentParameters.size();

                if ( sizeCallParam != sizeMethodParam){
                    System.out.println("Error: In class " + currentClass.getClassId() +" tried to overload function "+ methodType + " " + methodId + " ."  );
                    errorFound = true;  
                }

                Set Cset = currentParameters.entrySet();
                Iterator iteratorCurrent = Cset.iterator();

                Set Pset = parentParameters.entrySet();
                Iterator iteratorParent = Pset.iterator();

                while(iteratorParent.hasNext() && iteratorCurrent.hasNext()) {
                    Map.Entry entryP = (Map.Entry)iteratorParent.next();
                    String parentParamType = entryP.getValue().toString();

                    Map.Entry entryC = (Map.Entry)iteratorCurrent.next();
                    String currentParamType = entryC.getValue().toString();
                    
                    boolean superClass = false;
                    if (!currentParamType.equals(parentParamType) &&  !currentParamType.equals("int") && !currentParamType.equals("boolean") && !currentParamType.equals("int[]") && !currentParamType.equals("boolean[]"))
                    {
                        superClass = checkSuperClass(currentParamType,parentParamType);
                        if (superClass == false)
                        {
                            System.out.println("Error: In class " + currentClass.getClassId() +" tried to overload function "+ methodType + " " + methodId + " ."  );
                            errorFound = true; 
                        }
                    }
                    else if ( !currentParamType.equals(parentParamType) )
                    {
                        System.out.println("Error: In class " + currentClass.getClassId() +" tried to overload function "+ methodType + " " + methodId + " ."  );
                        errorFound = true;  
                    }
                }
            }
        }


        n.f4.accept(this, argu);
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);
        returnType = n.f10.accept(this, argu);
        if ( !methodType.equals(returnType) ){
            boolean parentfound = false;
            ClassInfo cl = classST.get(returnType);
            if (cl != null && cl.getParentClass() != null)
            {
                parentfound = cl.traverseParentClasses(returnType);
            }
            
            if (parentfound == false)
            {
                System.out.println("Error: In Class "+ currentClass.getClassId() + " method "+ methodType + " " + methodId + " should return "+ methodType +".\nInstead " + returnType + " returned."  );
                errorFound = true;
            }
        } 

        currentMethod = null;
        return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    */
   public String visit(FormalParameter n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        return _ret;
   }

   /**
    * f0 -> Type()
    * f1 -> Identifier()
    * f2 -> ";"
    */
   public String visit(VarDeclaration n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String id , type;
        type = n.f0.accept(this, argu);
        id = n.f1.accept(this, argu);
        return _ret; 
   }

   /**
    * f0 -> "if"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    * f5 -> "else"
    * f6 -> Statement()
    */
   public String visit(IfStatement n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType;
        exprType = n.f2.accept(this, argu);

        if ( !exprType.equals("boolean") ){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + ".\nFound expression with "+ exprType + " type in ifStatement."  );
            errorFound = true;
        }        
        n.f4.accept(this, argu);
        n.f6.accept(this, argu);
        return _ret;
   }

   /**
    * f0 -> "while"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> Statement()
    */
   public String visit(WhileStatement n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType;
        exprType = n.f2.accept(this, argu);

        if ( !exprType.equals("boolean") ){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + ".\nFound expression with "+ exprType + " type in WhileStatement."  );
            errorFound = true;
        }        
        n.f4.accept(this, argu);
        return _ret;
   }

 /**
    * f0 -> "System.out.println"
    * f1 -> "("
    * f2 -> Expression()
    * f3 -> ")"
    * f4 -> ";"
    */
   public String visit(PrintStatement n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType;
        exprType = n.f2.accept(this, argu);

        if (!exprType.equals("int"))
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad System.Out.println()" +".\nAn expression with type " + exprType + " tried to be printed."  );
            errorFound = true;
        }
        return _ret;
   }


   /**
    * f0 -> Identifier()
    * f1 -> "="
    * f2 -> Expression()
    * f3 -> ";"
    */
   public String visit(AssignmentStatement n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String idType, exprType; 
     
        idType = n.f0.accept(this, argu);

        exprType = n.f2.accept(this, argu);        

        boolean superClass = false;
        if (idType != null)
        {
            if (!idType.equals(exprType) &&  !idType.equals("int") && !idType.equals("boolean") && !idType.equals("int[]") && !idType.equals("boolean[]"))
            {
                superClass = checkSuperClass(idType,exprType);
                if (superClass == false)
                {
                    System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad assignment" +".\nAn expression with type " + exprType + " tried assign to a variable with type "+ idType + "."  );
                    errorFound = true; 
                }
            }
            else if ( !idType.equals(exprType)){
                System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad assignment" +".\nAn expression with type " + exprType + " tried assign to a variable with type "+ idType + "."  );
                errorFound = true;            
            }
        }
        return _ret;
   }

   /**
    * f0 -> Identifier()
    * f1 -> "["
    * f2 -> Expression()
    * f3 -> "]"
    * f4 -> "="
    * f5 -> Expression()
    * f6 -> ";"
    */
   public String visit(ArrayAssignmentStatement n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String idType , exprType1 , exprType2;
        idType = n.f0.accept(this, argu);
        exprType1 = n.f2.accept(this, argu);
        exprType2 = n.f5.accept(this, argu);

        if ( !exprType1.equals("int")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried to be index of an array ."  );
            errorFound = true;           
        }

        if ( idType.equals("int[]") && !exprType2.equals("int"))
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad assignment" +".\nAn expression with type " + exprType2 + " tried assigned to int[]."  );
            errorFound = true;   
        }        

        if ( idType.equals("boolean[]") && !exprType2.equals("boolean"))
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad assignment" +".\nAn expression with type " + exprType1 + " tried assigned to boolean[] ."  );
            errorFound = true;               
        }      

        return _ret;
   }



    /**
    * f0 -> Clause()
    * f1 -> "&&"
    * f2 -> Clause()
    */
   public String visit(AndExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2 ;
        exprType1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("boolean") || !exprType2.equals("boolean"))
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried logical AND with an expression of type "+ exprType2 + "."  );
            errorFound = true;    
        }    

        return exprType1;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "<"
    * f2 -> PrimaryExpression()
    */
   public String visit(CompareExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2 ;
        exprType1 = n.f0.accept(this, argu);
        n.f1.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("int") || !exprType2.equals("int"))
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried compare with an expression of type "+ exprType2 + "."  );
            errorFound = true;    
        }    

        return "boolean";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "+"
    * f2 -> PrimaryExpression()
    */
   public String visit(PlusExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2;
        exprType1 = n.f0.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("int") || !exprType2.equals("int")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried sum to an expression with type "+ exprType2 + "."  );
            errorFound = true;   
        }            

        return exprType1;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "-"
    * f2 -> PrimaryExpression()
    */
   public String visit(MinusExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2;
        exprType1 = n.f0.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("int") || !exprType2.equals("int")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried minus to an expression with type "+ exprType2 + "."  );
            errorFound = true;   
        }            

        return exprType1;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "*"
    * f2 -> PrimaryExpression()
    */
   public String visit(TimesExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2;
        exprType1 = n.f0.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("int") || !exprType2.equals("int")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried multiplication with an expression with type "+ exprType2 + "."  );
            errorFound = true;   
        }            

        return exprType1;
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "["
    * f2 -> PrimaryExpression()
    * f3 -> "]"
    */
   public String visit(ArrayLookup n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType1 , exprType2;
        exprType1 = n.f0.accept(this, argu);
        exprType2 = n.f2.accept(this, argu);

        if ( !exprType1.equals("int[]") && !exprType1.equals("boolean[]")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried to be indexed ."  );
            errorFound = true;   
        }  

        if ( !exprType2.equals("int")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType2 + " tried to be index of an array with type "+ exprType1 + "."  );
            errorFound = true;   
        }    

        if (exprType1.equals("int[]"))
            return "int";
        else
            return "boolean";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> "length"
    */
   public String visit(ArrayLength n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType;
        exprType = n.f0.accept(this, argu);
        
        if ( !exprType.equals("int[]") && !exprType.equals("boolean[]")){
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + ". Tried to take length field but type is "+exprType +" ."  );
            errorFound = true;   
        }    
        return "int";
   }

   /**
    * f0 -> PrimaryExpression()
    * f1 -> "."
    * f2 -> Identifier()
    * f3 -> "("
    * f4 -> ( ExpressionList() )?
    * f5 -> ")"
    */
   public String visit(MessageSend n, String argu) {
        // if (errorFound == true )
        //     return null;
        LinkedList<String> call = new LinkedList<String>();
        CallStack.push(call);
        String _ret=null;
        String classType , methodId ;
        MethodInfo method = null;
        thisUsed = false;

        classType = n.f0.accept(this, argu);
        methodId = n.f2.accept(this, "method");
      
        if (classType != null)
        {                      
            if (thisUsed == true)
            {
                method = currentClass.methodExists(methodId );
                if (method == null){
                    System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nUndefined method call."  );
                    errorFound = true; 
                }
            }
            else
            {  
                ClassInfo classOfMethod = classST.get(classType);
                if (classOfMethod == null)
                {
                    System.out.println("Error: Something wrong happened."  );
                    errorFound = true; 
                }
                method = classOfMethod.methodExists(methodId );
                if (method == null){
                    System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nUndefined method call."  );
                    errorFound = true; 
                }
            }
            n.f4.accept(this,argu);
            // System.out.println(method.getMethodType() + " " + method.getMethodId());
            LinkedHashMap<String, String> par = method.getParametersList();

            LinkedList<String> callParams = CallStack.pop();
                
            int sizeCallParam = callParams.size();
            int sizeMethodParam = par.size();

            if ( sizeCallParam > sizeMethodParam){
                System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nFunction call "+ methodId +" had more parameters than the method signature."  );
                errorFound = true;  
            }
            else if ( sizeCallParam < sizeMethodParam){
                System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nFunction call "+ methodId +" had less parameters than the method signature."  );
                errorFound = true;  
            }
            else
            {
                int counter = 0 ; 

                Set Pset = par.entrySet();
                Iterator iterator = Pset.iterator();
                while(iterator.hasNext()) {
                    Map.Entry entry = (Map.Entry)iterator.next();
                    String methodParamType = entry.getValue().toString();
                    String callParamType =  callParams.get(counter);
                    
                    boolean superClass = false;
                    if (!methodParamType.equals(callParamType) &&  !methodParamType.equals("int") && !methodParamType.equals("boolean") && !methodParamType.equals("int[]") && !methodParamType.equals("boolean[]"))
                    {
                        superClass = checkSuperClass(methodParamType,callParamType);
                        if (superClass == false)
                        {
                            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nFunction call "+ methodId +" had one or more wrong type parameters than the method signature."  );
                            errorFound = true; 
                        }
                    }
                    else if ( !methodParamType.equals(callParamType) )
                    {
                        System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nFunction call "+ methodId +" had one or more wrong type parameters than the method signature."  );
                        errorFound = true;  
                    }

                    

                    counter++;
                }
            }
            thisUsed = false;
            //popCall();
            return method.getMethodType();
        }
        
        //popCall();
        return null;
   }


   /**
    * f0 -> Expression()
    * f1 -> ExpressionTail()
    */
   public String visit(ExpressionList n, String argu) {
        // if (errorFound == true )
        //     return null;
        LinkedList<String> callParams = CallStack.pop();
        String _ret=null;
        String exprType;
        exprType = n.f0.accept(this, argu);
        callParams.addLast(exprType);

        CallStack.push(callParams);

        n.f1.accept(this, argu);
        return _ret;
   }

   /**
    * f0 -> ( ExpressionTerm() )*
    */
   public String visit(ExpressionTail n, String argu) {
        // if (errorFound == true )
        //     return null;

        return n.f0.accept(this, argu);
   }

   /**
    * f0 -> ","
    * f1 -> Expression()
    */
   public String visit(ExpressionTerm n, String argu) {
        // if (errorFound == true )
        //     return null;

        String _ret=null;
        String exprType;
        LinkedList<String> callParams = CallStack.pop();
        exprType = n.f1.accept(this, argu);
        callParams.addLast(exprType);
        CallStack.push(callParams);

       return _ret;
   }


    /**
    * f0 -> ArrayType()
    *       | BooleanType()
    *       | IntegerType()
    *       | Identifier()
    */
   public String visit(Type n, String argu) {
        // if (errorFound == true )
        //     return null;

        return n.f0.accept(this, "type");
   }

   /**
    * f0 -> <INTEGER_LITERAL>
    */
   public String visit(IntegerLiteral n, String argu) {
        // if (errorFound == true )
        //     return null;
        
        return "int";
   }

   /**
    * f0 -> "true"
    */
   public String visit(TrueLiteral n, String argu) {
        // if (errorFound == true )
        //     return null;
        return "boolean";
   }

   /**
    * f0 -> "false"
    */
   public String visit(FalseLiteral n, String argu) {
        // if (errorFound == true )
        //     return null;

        return "boolean";
   }


    /**
    * f0 -> <IDENTIFIER>
    */
   public String visit(Identifier n, String argu) {
        // if (errorFound == true )
        //     return null;

        if ( (argu != null ) && ( argu.equals("type")) ){
            ClassInfo temp =  classExists(n.f0.accept(this, argu));
            if (temp != null)
                return temp.getClassId();
            else
                return null;   
        }

        if ( (argu != null) && ( argu.equals("class") || argu.equals("method") )){
            return n.f0.accept(this, argu);
        }

        String idType; 
        idType = this.varLookup(n.f0.accept(this, argu));
        if ( idType == null )
        {
                
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nVariable "+ n.f0.accept(this, argu) + " not found."  );
            errorFound = true;  
        }
        return idType;
    }

    /**
     * f0 -> "boolean"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(BooleanArrayType n, String argu) {
        // if (errorFound == true )
        //     return null;

        return "boolean[]";
    }

    /**
     * f0 -> "int"
     * f1 -> "["
     * f2 -> "]"
     */
    public String visit(IntegerArrayType n,String argu) {
        // if (errorFound == true )
        //     return null;

        return "int[]";
    }

    /**
    * f0 -> "this"
    */
    public String visit(ThisExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        thisUsed = true;
        return currentClass.getClassId();
    }


  /**
    * f0 -> "new"
    * f1 -> "boolean"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(BooleanArrayAllocationExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String exprType;
        exprType = n.f3.accept(this, argu);
        if ( !exprType.equals("int") )
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nExpression with type "+ exprType+" tried to be index in array allocation."  );
            errorFound = true;  
        }
        return "boolean[]";
   }

   /**
    * f0 -> "new"
    * f1 -> "int"
    * f2 -> "["
    * f3 -> Expression()
    * f4 -> "]"
    */
   public String visit(IntegerArrayAllocationExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String exprType;
        exprType = n.f3.accept(this, argu);
        if ( !exprType.equals("int") )
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() +".\nExpression with type "+ exprType+" tried to be index in array allocation."  );
            errorFound = true;  
        }
        return "int[]";
   }

   /**
    * f0 -> "new"
    * f1 -> Identifier()
    * f2 -> "("
    * f3 -> ")"
    */
   public String visit(AllocationExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

      return n.f1.accept(this, "class");
   }

   /**
    * f0 -> "!"
    * f1 -> Clause()
    */
   public String visit(NotExpression n, String argu) {
        // if (errorFound == true )
        //     return null;

        String exprType1 ;
        exprType1 = n.f1.accept(this, argu);

        if ( !exprType1.equals("boolean") )
        {
            System.out.println("Error: In method "+ currentMethod.getMethodType() + " " + currentMethod.getMethodId() + " of class " + currentClass.getClassId() + " found bad expression" +".\nAn expression with type " + exprType1 + " tried logical NOT ."  );
            errorFound = true;    
        }    

        return exprType1;


   }

   /**
    * f0 -> "("
    * f1 -> Expression()
    * f2 -> ")"
    */
   public String visit(BracketExpression n, String argu) {
        // if (errorFound == true )
        //     return null;
        String _ret=null;
        return n.f1.accept(this, argu);
      
   }

    public String visit(NodeToken n, String argu) { 
        return n.toString(); 
    }

}