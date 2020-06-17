package visitors;
import syntaxtree.*;
import java.util.*;
import visitor.GJDepthFirst;
import types.*;

public class GenCodeVisitor extends GJDepthFirst<String, String>{

    HashMap<String,ClassInfo> classST;
    private ClassInfo  currentClass;
    private MethodInfo currentMethod;

    private boolean thisUsed;

    private Stack<LinkedList<String>> CallStack;

    private int REG ;
    private int Label;


    public GenCodeVisitor(HashMap<String,ClassInfo> classST) {
        thisUsed = false;
        currentClass = null;
        currentMethod = null;
        CallStack = new Stack<LinkedList<String>>();

        this.classST = classST;

        startOfGenerate();

        REG = 0;
        Label = 0;
    }

    // function to produce a new temp register
    public String newReg(){
        String temp = "_t"+REG;
        REG++;
        return temp;
    }

    //function to produce a new temp label
    public String newLabel(String type){
        String label = type+Label;
        Label++;
        return label;
    }


    // function to return the assembly type of a variable
    public String getAssemblyType(String type){
        if (type.equals("int"))
        {
            return "i32";
        }
        else if (type.equals("boolean"))
        {
            return "i1";
        }
        else if (type.equals("int[]"))
        {
            return "i32*";
        }
        else
        {
            return "i8*";
        }
    }

    //function to build the IR v-table of each class in the start of the code generated IR file
    public void buildVtable(ClassInfo currentClass){

        LinkedHashMap<String,Integer> methodOffsets = currentClass.getMethodOffsets();
        MethodInfo method=null;

        boolean firstMethod = true;
        int wantedOffset = 0 ;
        //search for the method with the correct offset each time
        while(wantedOffset >= 0)
        {
            int offset;
            boolean found = false;
            //take a linked hashmap with all  method offsets and loop
            for (String methodsKeys : methodOffsets.keySet()){

                //ignore main function
                if ( methodsKeys.equals("main"))
                    continue;

                offset = methodOffsets.get(methodsKeys);
                if(offset == wantedOffset)//if found the correct offset
                {   //take the method
                    LinkedHashMap<String,MethodInfo> methods = currentClass.getMethods();
                    method = methods.get(methodsKeys);
                    found = true; //method found
                    break;
                }
            }
            // if method not found in current class search the parent classes if exist
            ClassInfo parent = currentClass.getParentClass();
            if( (found == false) && (parent != null) )
            {
                method = parent.checkParentClassesForMethod(wantedOffset);
                if (method !=null)
                    found = true;
            }

            if (found == true)//method has found
            {
                String methodType = method.getMethodType();
                String methodId = method.getMethodId();
                if (firstMethod == false)
                    System.out.print(",");
                System.out.print("\n\ti8* bitcast ("+getAssemblyType(methodType) +" (i8*");

                LinkedHashMap<String, String> parameters = method.getParametersList();

                Set set = parameters.entrySet();
                // Displaying elements of LinkedHashMap
                Iterator iterator = set.iterator();
                while(iterator.hasNext()) {
                    Map.Entry me = (Map.Entry)iterator.next();
                    String type = me.getValue().toString();
                    System.out.print(","+ getAssemblyType(type));
                }

                String classId = method.getCurrentClassId();
                System.out.print(")* @"+classId+"."+methodId +" to i8*)");
                firstMethod = false;

                wantedOffset = wantedOffset + 8; // for search the next method

            }
            else //the method with wanted offset dont exist so the v-table is produced , exit the function
                wantedOffset = -1;
        }
    }

    //function to start the code generation , build v-tables ,
    // declare functions such as calloc , printf , out of bounds and negative size allocate exceptions
    public void startOfGenerate(){
        for (String keys : classST.keySet())
        {
            ClassInfo currentClass = classST.get(keys);
            System.out.print("@."+currentClass.getClassId()+"_vtable = global ["+currentClass.getMethodsSize()+" x i8*] [");
            buildVtable(currentClass);
            System.out.println("\n]\n");

        }

        System.out.println( "declare i8* @calloc(i32, i32)\n"+
                "declare i32 @printf(i8*, ...)\n"+
                "declare void @exit(i32)\n\n"+
                "@_cint = constant [4 x i8] c\"%d\\0a\\00\"\n"+
                "@_cOOB = constant [15 x i8] c\"Out of bounds\\0a\\00\"\n"+
                "@_cNSZ = constant [15 x i8] c\"Negative size\\0a\\00\"\n\n"+
                "define void @print_int(i32 %i) {\n"+
                "    %_str = bitcast [4 x i8]* @_cint to i8*\n"+
                "    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)\n"+
                "    ret void\n"+
                "}\n\n"+
                "define void @throw_oob() {\n"+
                "    %_str = bitcast [15 x i8]* @_cOOB to i8*\n"+
                "    call i32 (i8*, ...) @printf(i8* %_str)\n"+
                "    call void @exit(i32 1)\n"+
                "    ret void\n"+
                "}\n\n"+
                "define void @throw_nsz() {\n"+
                "    %_str = bitcast [15 x i8]* @_cNSZ to i8*\n"+
                "    call i32 (i8*, ...) @printf(i8* %_str)\n"+
                "    call void @exit(i32 1)\n"+
                "    ret void\n"+
                "}\n\n" +
                "define i32 @main() {\n");
    }

    //function to return the name of a class if the class exists
    public ClassInfo classExists(String id){
        if ( this.classST.containsKey(id) == true){
            return this.classST.get(id) ;
        }
        System.out.println("Error: Class " + id + " not exists");

        return null;
    }

    //function to check if a variable is a local or a parameter of a method and return the type of the variable
    public String checkParametersAndLocals(String id , String type){
        if (currentMethod != null){

            type = currentMethod.getParameterType(id);
            if (type != null)
                return type;

            type = currentMethod.getLocalType(id);
            if (type != null)
                return type;
        }
        return null;
    }

    //function to check if a variable is a class field or a local - parameter in a method
    //and return the register which has the loaded variable
    public String checkVariable(String id , String type){
        FieldInfo field = currentClass.getField(id) ;
        //in this case exists a field and a local/parameter with the same id , so we shadow the field and load the local/parameter
        if ((field != null) && (checkParametersAndLocals(id,type) != null) ) {
            String tempReg1 = newReg();
            System.out.println("\t%" + tempReg1 + " = load "+getAssemblyType(type)+", "+getAssemblyType(type)+"* %" + id);
            return tempReg1;
        }
        else if (field != null)//the id is a class field
        {   String t1 = newReg();
            int offset = field.getFieldOffset()+8; //get the field with the field-offset
            System.out.println("\t%"+t1+" = getelementptr i8, i8* %this, i32 "+offset);
            String t2 = newReg(); // bitcast to the correct type
            System.out.println("\t%"+t2+" = bitcast i8* %"+t1+" to "+getAssemblyType(field.getFieldType())+"*");
            String t3 = newReg(); // load the field
            System.out.println("\t%"+t3+" = load "+getAssemblyType(field.getFieldType())+", "+getAssemblyType(field.getFieldType())+"* %"+t2);
            return t3;
        }
        else if (checkParametersAndLocals(id,type) != null){ // the id is a local or a parameter
            String tempReg1 = newReg();
            System.out.println("\t%" + tempReg1 + " = load "+getAssemblyType(type)+", "+getAssemblyType(type)+"* %" + id);
            return tempReg1;
        }
        else if (checkParametersAndLocals(id,type) == null){ // we didnt find the id nowhere so return the id
            return id;
        }

        return null;
    }

    //function to check if an id which is called to be stored something , is a field or a parameter/local of a method
    public String checkVariableToStore(String id , String type){
        FieldInfo field = currentClass.getField(id) ;
        // shadow exists such as before , so simply return the id
        if ((field != null) && (checkParametersAndLocals(id,type) != null) ){
            return id;
        }
        else if (field != null) // if the id is a field we must do some steps to take the address of the field
        {   String t1 = newReg();
            int offset = field.getFieldOffset()+8;
            System.out.println("\t%"+t1+" = getelementptr i8, i8* %this, i32 "+offset);
            String t2 = newReg();
            System.out.println("\t%"+t2+" = bitcast i8* %"+t1+" to "+getAssemblyType(field.getFieldType())+"*");
            return t2;
        }
        else {
            return id;
        }
    }

    //function to check if an id exists in current class or parent classes or in current method such a local or a parameter and return the id's type
    public String varLookup(String id){
        String type;
        if (currentMethod != null){ // check if exists in current method

            type = currentMethod.getParameterType(id); //parameter
            if (type != null)
                return type;

            type = currentMethod.getLocalType(id); // local
            if (type != null)
                return type;
        }

        if (currentClass != null){ // check if it is a field of the current class
            type = currentClass.getFieldType(id);
            if (type != null)
                return type;
        }
        if (currentClass != null) // check the parent class
        {   ClassInfo parent = currentClass.getParentClass() ;
            if ( parent != null){
                ClassInfo temp = currentClass ;
                currentClass = parent;
                type = this.varLookup(id); // do the same varLookup in parent class

                currentClass = temp;
                if(type != null)
                    return type;

            }
        }
        return null;

    }

    //function to return the register of a result
    //the result of each visit is a String  type,register
    public String getRegister(String result){
        if(result.contains(",")){ // split the string
            String[] register = result.split(",");
            return register[1]; //register[1] is the register
        }
        else
            return result; //if the string not contain ',' just return the string
    }
    //function to return the type of a result
    //the result of each visit is a String  type,register
    public String getType(String result){
        if(result.contains(",")){
            String[] type = result.split(",");
            return type[0]; //type[0] is the type
        }
        else
            return result;
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

        classId = n.f1.accept(this, "class");
        currentClass = classExists(classId);
        currentMethod = currentClass.methodExists("main");

        n.f11.accept(this, "method");

        n.f14.accept(this, argu);
        n.f15.accept(this, argu);
        currentClass = null;
        currentMethod = null;

        System.out.println("\tret i32 0\n}\n");

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
        String classId;
        classId = n.f1.accept(this, "class") ;
        currentClass = classExists(classId);

        //n.f3.accept(this, classId);
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

        String _ret=null;
        String classId , parentClassId;
        classId = n.f1.accept(this, "class");

        parentClassId = n.f3.accept(this, "class");
        currentClass = classExists(classId);

        //n.f5.accept(this, classId);
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

        String _ret=null;
        String methodId , methodType ;
        methodType = n.f1.accept(this, argu);
        methodId = n.f2.accept(this, "method");

        currentMethod = currentClass.methodExists(methodId);

        System.out.print("define "+ getAssemblyType(methodType) +" @"+currentClass.getClassId()+"."+methodId+"(i8* %this");
        n.f4.accept(this, argu);
        System.out.println(") {");
        n.f4.accept(this, "alloca"); //to save the values of the parameters
        n.f7.accept(this, argu);
        n.f8.accept(this, argu);

        String returnResult , returnType , returnReg;
        returnResult = n.f10.accept(this, argu);
        returnReg = getRegister(returnResult);
        returnType = getType(returnResult);

        returnReg = checkVariable(returnReg,returnType);

        System.out.println("\tret "+getAssemblyType(returnType)+" %"+returnReg+"\n}");

        currentMethod = null;
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     */
    public String visit(FormalParameter n, String argu) {

        String _ret=null;
        String type ,result, id;
        type = n.f0.accept(this, argu);
        result = n.f1.accept(this, argu);
        id = getRegister(result);

        if (argu.equals("alloca")){ //to save the values of the paremeters
            System.out.println("\t%"+id+" = alloca "+getAssemblyType(type));
            System.out.println("\tstore "+getAssemblyType(type)+" %."+id+", "+getAssemblyType(type)+"* %"+id);
        }
        else {//the type id in parmaters list in method declaration
            System.out.print(" , " + getAssemblyType(type) + " %." + id);
        }
        return _ret;
    }

    /**
     * f0 -> Type()
     * f1 -> Identifier()
     * f2 -> ";"
     */
    public String visit(VarDeclaration n, String argu) {
        String _ret=null;
        String id , type , result;

        type = n.f0.accept(this, argu);
        result = n.f1.accept(this, argu);
        id = getRegister(result);
        System.out.println("\t%"+id+" = "+"alloca "+ getAssemblyType(type));

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

        String _ret=null;
        String exprType , reg, result;
        result = n.f2.accept(this, argu);
        exprType = getType(result);

        String labelIf = newLabel("if");
        String labelElse = newLabel("else");
        String labelEnd = newLabel("end_if");

        reg = getRegister(result);

        reg = checkVariable(reg,exprType);
        System.out.println("\tbr i1 %" + reg + ", label %" + labelIf + ", label %" + labelElse);
        System.out.println("\t"+labelIf+":");
        n.f4.accept(this, argu);
        System.out.println("\tbr label %"+labelEnd+"\n");

        System.out.println("\t"+labelElse+":");
        n.f6.accept(this, argu);
        System.out.println("\tbr label %"+labelEnd+"\n");

        System.out.println("\t"+labelEnd+":");
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

        String whileCondition = newLabel("while_condition");
        String whileBody = newLabel("while_body");
        String whileEnd = newLabel("end_while");

        String _ret=null;
        String exprType , reg , result;
        System.out.println("\tbr label %"+whileCondition+"\n");
        System.out.println("\t"+whileCondition+":");//label to compute the condition

        result = n.f2.accept(this, argu);
        exprType = getType(result);
        reg = getRegister(result);
        reg = checkVariable(reg,exprType); //and then br to body or exit the loop
        System.out.println("\tbr i1 %"+reg+", label %"+whileBody+", label %"+whileEnd+"\n");

        System.out.println("\t"+whileBody+":");
        n.f4.accept(this, argu);
        System.out.println("\tbr label %"+whileCondition+"\n");
        System.out.println("\t"+whileEnd+":");
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

        String _ret=null;
        String result ,exprType , reg;
        result = n.f2.accept(this, argu);
        exprType = getType(result);
        reg = getRegister(result);

        reg = checkVariable(reg,exprType);
        System.out.println("\tcall void (i32) @print_int(i32 %" + reg + ")");

        return _ret;
    }

    /**
     * f0 -> Identifier()
     * f1 -> "="
     * f2 -> Expression()
     * f3 -> ";"
     */
    public String visit(AssignmentStatement n, String argu) {


        String _ret=null;
        String idType ,id , result , exprType , returnReg , result2;

        result = n.f0.accept(this, argu);
        idType = getType(result);
        id = getRegister(result);

        result2 = n.f2.accept(this, id);
        exprType = getType(result2);
        returnReg = getRegister(result2);

        String lreg = checkVariableToStore(id,idType);
        String rreg = checkVariable(returnReg,exprType);
        //store the register which has the result of the expression to the register of the identifier
        System.out.println("\tstore " + getAssemblyType(idType) + " %" + rreg + "," + getAssemblyType(idType) + "* %" + lreg);

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

        String _ret=null;
        String idType , exprType1 , exprType2 , reg1 , reg2 ,result1 , result2 , resultID;
        resultID = n.f0.accept(this, argu);
        result1 = n.f2.accept(this, argu);
        result2 = n.f5.accept(this, argu);

        idType = getType(resultID);
        exprType1 = getType(result1);
        exprType2 = getType(result2);

        String idreg = getRegister(resultID);
        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        if (idType.equals("int[]")){ //int array

            idreg = checkVariable(idreg,idType); //id reg has already the address of the array
            String size = newReg(); //load the size of the array
            System.out.println("\t %"+size+" = load i32, i32* %"+idreg);

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
            reg1 = checkVariable(reg1,exprType1); //check if the reg1 which is the index of the array , is between 0-size
            System.out.println("\t%"+temp3+" = icmp sge i32 %"+reg1+",0\n" +
                    "\t%"+temp4+" = icmp slt i32 %"+reg1+", %"+size+"\n" +
                    "\t%"+temp5+" = and i1 %"+temp3+", %"+temp4+"\n" +
                    "\tbr i1 %"+temp5+", label %"+label+", label %"+errorLabel+"\n");

            //out of bounds error label
            System.out.println("\t"+errorLabel+":\n" +
                    "\tcall void @throw_oob()\n" +
                    "\tbr label %"+label);

            // index the array
            System.out.println("\t"+label+":\n");
            String index = newReg();
            String indexPtr = newReg();//the index 0 has the size of the array so the corrent index is index+1
            System.out.println( "\t%"+index+" = add i32 1, %"+reg1+"\n"+
                    "\t%"+indexPtr+" = getelementptr i32, i32* %"+idreg+", i32 %"+index);

            reg2 = checkVariable(reg2,exprType2); //store the result in the correct index of the array
            System.out.println("\tstore i32 %"+ reg2 +",i32* %"+indexPtr);

        }
        else{ //boolean array
            idreg = checkVariable(idreg,idType);//id reg has already the address of the array
            String treg = newReg(); // bitcast to i32 to take the size
            System.out.println("\t%"+treg+" = bitcast i8* %"+idreg+" to i32*");
            String size = newReg(); //load the size of the array
            System.out.println("\t %"+size+" = load i32, i32* %"+treg);

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
            reg1 = checkVariable(reg1,exprType1); //same as before for out of bounds
            System.out.println("\t%"+temp3+" = icmp sge i32 %"+reg1+",0\n" +
                    "\t%"+temp4+" = icmp slt i32 %"+reg1+", %"+size+"\n" +
                    "\t%"+temp5+" = and i1 %"+temp3+", %"+temp4+"\n" +
                    "\tbr i1 %"+temp5+", label %"+label+", label %"+errorLabel+"\n");

            //out of bounds error label
            System.out.println("\t"+errorLabel+":\n" +
                    "\tcall void @throw_oob()\n" +
                    "\tbr label %"+label);

            // index the array
            System.out.println("\t"+label+":\n");
            reg2 = checkVariable(reg2,exprType2);
            String index = newReg();
            String indexPtr = newReg(); // the size in the boolean arrays covers the first 4 cells , so the correct index is index+4
            System.out.println("\t%"+index+" = add i32 4, %"+reg1+"\n");
            String regzext = newReg();
            System.out.println("\t%"+regzext+" = zext i1 %"+reg2+" to i8"); // extend reg2 from type i1 to type i8 to be compatible with the array
            System.out.println("\t%"+indexPtr+" = getelementptr i8, i8* %"+idreg+", i32 %"+index); //take the address of the correct index

            System.out.println("\tstore i8 %"+ regzext +",i8* %"+indexPtr); //store to correct index of array
        }
        return _ret;
    }



    /**
     * f0 -> Clause()
     * f1 -> "&&"
     * f2 -> Clause()
     */
    public String visit(AndExpression n, String argu) {

        String exprType1 , exprType2 ,reg1 ,reg2, result1 , result2;

        result1 = n.f0.accept(this, argu);
        exprType1 = getType(result1);
        reg1 = getRegister(result1);

        reg1 = checkVariable(reg1,exprType1); //reg1 is the first expression to check of the AND

        String and0 , and1 , and2 , and3;
        and0 = newLabel("and");
        and1 = newLabel("and");
        and2 = newLabel("and");
        and3 = newLabel("and");
        System.out.println("\tbr i1 %"+reg1+", label %"+and1+", label %"+and0+"\n");
        //if reg1 is false
        System.out.println("\t"+and0+":");
        System.out.println("\tbr label %"+and3); //branch to and3 and dont check the other expression

        //if reg1 is true
        System.out.println("\n\t"+and1+":");

        result2 = n.f2.accept(this, argu);
        exprType2 = getType(result2);
        reg2 = getRegister(result2);

        reg2 = checkVariable(reg2,exprType2); //get the register of the right expression of the AND
        System.out.println("\tbr label %"+and2);

        System.out.println("\n\t"+and2+":");
        System.out.println("\tbr label %"+and3);

        System.out.println("\n\t"+and3+":");
        String returnReg = newReg(); //if we came from the label and0 the return register is 0 , otherwise the return register takes the value of register2 from right expression in AND
        System.out.println("\t%"+returnReg+" = phi i1  [ 0, %"+and0+" ], [ %"+reg2+", %"+and2+" ]");

        return exprType1+","+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "<"
     * f2 -> PrimaryExpression()
     */
    public String visit(CompareExpression n, String argu) {

        String _ret=null;
        String exprType1 , exprType2 , reg1 , reg2 , result1 , result2 ;
        result1 = n.f0.accept(this, argu);
        result2 = n.f2.accept(this, argu);

        exprType1 = getType(result1);
        exprType2 = getType(result2);

        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        String returnReg = newReg();
        reg1 = checkVariable(reg1,exprType1);
        reg2 = checkVariable(reg2,exprType2);

        System.out.println("\t%" + returnReg + " = icmp slt " + getAssemblyType(exprType1) + " %" + reg1 + ", %" + reg2);

        return "boolean,"+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "+"
     * f2 -> PrimaryExpression()
     */
    public String visit(PlusExpression n, String argu) {

        String exprType1 , exprType2 ,reg1 , reg2 , result1 , result2;
        result1 = n.f0.accept(this, argu);
        result2 = n.f2.accept(this, argu);

        exprType1 = getType(result1);
        exprType2 = getType(result2);

        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        String returnReg = newReg();
        reg1 = checkVariable(reg1,exprType1);
        reg2 = checkVariable(reg2,exprType2);

        System.out.println("\t%" + returnReg + " = add " + getAssemblyType(exprType1) + " %" + reg1 + ", %" + reg2);

        return exprType1+","+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "-"
     * f2 -> PrimaryExpression()
     */
    public String visit(MinusExpression n, String argu) {

        String exprType1 , exprType2 ,reg1 , reg2 , result1 , result2;
        result1 = n.f0.accept(this, argu);
        result2 = n.f2.accept(this, argu);

        exprType1 = getType(result1);
        exprType2 = getType(result2);

        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        String returnReg = newReg();
        reg1 = checkVariable(reg1,exprType1);
        reg2 = checkVariable(reg2,exprType2);
        System.out.println("\t%" + returnReg + " = sub " + getAssemblyType(exprType1) + " %" + reg1 + ", %" + reg2);

        return exprType1+","+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "*"
     * f2 -> PrimaryExpression()
     */
    public String visit(TimesExpression n, String argu) {

        String exprType1 , exprType2 ,reg1 , reg2 , result1 , result2;
        result1 = n.f0.accept(this, argu);
        result2 = n.f2.accept(this, argu);

        exprType1 = getType(result1);
        exprType2 = getType(result2);

        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        String returnReg = newReg();
        reg1 = checkVariable(reg1,exprType1);
        reg2 = checkVariable(reg2,exprType2);

        System.out.println("\t%" + returnReg + " = mul " + getAssemblyType(exprType1) + " %" + reg1 + ", %" + reg2);
        return exprType1+","+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "["
     * f2 -> PrimaryExpression()
     * f3 -> "]"
     */
    public String visit(ArrayLookup n, String argu) {


        String  result1 , result2 , exprType1 , exprType2 , reg1 , reg2 ;
        result1 = n.f0.accept(this, argu);
        result2 = n.f2.accept(this, argu);

        exprType1 = getType(result1);
        exprType2 = getType(result2);

        reg1 = getRegister(result1);
        reg2 = getRegister(result2);

        String returnReg  = newReg();
        if (exprType1.equals("int[]")){ //int array

            reg1 = checkVariable(reg1,exprType1); //reg1 has already the address of the array
            String size = newReg();
            System.out.println("\t %"+size+" = load i32, i32* %"+reg1); //load the size of the array

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
            reg2 = checkVariable(reg2,exprType2);//check for out of bounds
            System.out.println("\t%"+temp3+" = icmp sge i32 %"+reg2+",0\n" +
                    "\t%"+temp4+" = icmp slt i32 %"+reg2+", %"+size+"\n" +
                    "\t%"+temp5+" = and i1 %"+temp3+", %"+temp4+"\n" +
                    "\tbr i1 %"+temp5+", label %"+label+", label %"+errorLabel+"\n");

            //out of bounds error label
            System.out.println("\t"+errorLabel+":\n" +
                    "\tcall void @throw_oob()\n" +
                    "\tbr label %"+label);

            // index the array
            System.out.println("\t"+label+":\n");
            String index = newReg();
            String indexPtr = newReg(); //add 1 to index , because the index 0 is the size of the array
            System.out.println( "\t%"+index+" = add i32 1, %"+reg2+"\n"+
                    "\t%"+indexPtr+" = getelementptr i32, i32* %"+reg1+", i32 %"+index);


            System.out.println("\t%"+returnReg+" = load i32, i32* %"+indexPtr); //load the value of the index in Return Register

        }
        else{
            reg1 = checkVariable(reg1,exprType1);//reg1 has already the address of the array

            String bitcastReg = newReg();//bitcast from i8 to i32 to load the size of the array
            System.out.println("\t%"+bitcastReg+" = bitcast i8* %"+reg1+" to i32*");
            String size = newReg();
            System.out.println("\t %"+size+" = load i32, i32* %"+bitcastReg); //load the size

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
            reg2 = checkVariable(reg2,exprType2); // check for out of bounds
            System.out.println("\t%"+temp3+" = icmp sge i32 %"+reg2+",0\n" +
                    "\t%"+temp4+" = icmp slt i32 %"+reg2+", %"+size+"\n" +
                    "\t%"+temp5+" = and i1 %"+temp3+", %"+temp4+"\n" +
                    "\tbr i1 %"+temp5+", label %"+label+", label %"+errorLabel+"\n");

            //out of bounds error label
            System.out.println("\t"+errorLabel+":\n" +
                    "\tcall void @throw_oob()\n" +
                    "\tbr label %"+label);

            // index the array
            System.out.println("\t"+label+":\n");
            String index = newReg();
            String indexPtr = newReg();//add 4 to index , because in the first 4 indexes it is located the size of the array
            System.out.println( "\t%"+index+" = add i32 4, %"+reg2+"\n"+
                    "\t%"+indexPtr+" = getelementptr i8, i8* %"+reg1+", i32 %"+index);

            String tempreg = newReg();
            System.out.println("\t%"+tempreg+" = load i8, i8* %"+indexPtr); //load the value of the index
            System.out.println("\t%"+returnReg+" = trunc i8 %"+tempreg+" to i1"); //truncate the value from i8 to i1 and place it to Return Register

        }

        if (exprType1.equals("int[]"))
            return "int,"+returnReg;
        else
            return "boolean,"+returnReg;
    }

    /**
     * f0 -> PrimaryExpression()
     * f1 -> "."
     * f2 -> "length"
     */
    public String visit(ArrayLength n, String argu) {

        String result , reg, exprType;
        result = n.f0.accept(this, argu);
        exprType = getType(result);
        reg = getRegister(result);

        String returnreg = newReg();
        reg = checkVariable(reg,exprType);
        //return the size of the array
        if (exprType.equals("int[]")){
            System.out.println("\t%"+returnreg+" = load i32, i32* %"+reg);
        }
        if(exprType.equals("boolean[]")){
            String bitcastReg = newReg();
            System.out.println("\t%"+bitcastReg+" = bitcast i8* %"+reg+" to i32*");
            System.out.println("\t%"+returnreg+" = load i32, i32* %"+bitcastReg);
        }

        return "int,"+returnreg;
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

        String _ret=null;
        LinkedList<String> call = new LinkedList<String>(); //make a new linkedlist for parameters
        CallStack.push(call); //push the new parameter-list in the stack which has the calls

        String classType ,result, methodId , reg ;
        MethodInfo method = null;
        thisUsed = false;

        result = n.f0.accept(this, argu);
        classType = getType(result);
        reg = getRegister(result);
        methodId = n.f2.accept(this, "method");
        if (classType != null)
        {
            if (thisUsed)//if this is used
            {
                method = currentClass.methodExists(methodId );
            }
            else //if the method is from other class
            {
                ClassInfo classOfMethod = classST.get(classType);
                method = classOfMethod.methodExists(methodId );
            }
            n.f4.accept(this,argu); //parameters of message Send
            LinkedHashMap<String, String> par = method.getParametersList();
            LinkedList<String> callParams = CallStack.pop(); //pop the linked list with the parameters from the call stack

            reg = checkVariable(reg,classType);
            String t2 = newReg();
            System.out.println("\t%" + t2 + " = bitcast i8* %" + reg + " to i8***");

            String t3 = newReg(); //Load vtable_ptr
            System.out.println("\t%"+t3+" = load i8**, i8*** %"+t2);
            String t4 = newReg(); //Get a pointer to the 0-th entry in the vtable , find the function with the offset
            int position = method.getMethodOffset()/8;
            System.out.println("\t%"+t4+" = getelementptr i8*, i8** %"+t3+", i32 "+position);
            String t5 = newReg(); //Get the actual function pointer
            System.out.println("\t%"+t5+" = load i8*, i8** %"+t4);
            String t6 = newReg(); //Cast the function pointer from i8* to a function ptr type that matches its signature.
            System.out.print("\t%"+t6+" = bitcast i8* %"+t5+" to "+getAssemblyType(method.getMethodType())+" (i8*");

            //loop to print all the parameter types which are need in the above bitcast
            Set set = par.entrySet();
            Iterator iterator = set.iterator();
            while(iterator.hasNext()) {
                Map.Entry entry = (Map.Entry) iterator.next();
                String methodParamType = entry.getValue().toString();
                System.out.print(", "+getAssemblyType(methodParamType));
            }
            System.out.println(")*");

            LinkedList<String> loadedParameters = new LinkedList<String>();
            int counter = 0 ;
            set = par.entrySet();
            iterator = set.iterator();
            while(iterator.hasNext()) { //loop to load the parameters if it is needed , and put them in one linked list
                Map.Entry entry = (Map.Entry) iterator.next();
                String methodParamType = entry.getValue().toString();
                String register =  callParams.get(counter);

                register = checkVariable(register,methodParamType);

                loadedParameters.addLast(register);

                counter++;
            }

            String returnReg = newReg();
            reg = checkVariable(reg,classType); //reg is the object pointer which is always the first argument
            System.out.print("\t%" + returnReg + " = call "+ getAssemblyType(method.getMethodType())  +" %" + t6 + "(i8* %" + reg);

            counter = 0 ;
            set = par.entrySet();
            iterator = set.iterator();
            while(iterator.hasNext()) { //print every parameter to complete the above call
                Map.Entry entry = (Map.Entry) iterator.next();
                String methodParamType = entry.getValue().toString();
                String register =  loadedParameters.get(counter);
                System.out.print(", "+getAssemblyType(methodParamType) +"  %" + register  );
                counter++;
            }
            System.out.println(")");

            thisUsed = false;

            return method.getMethodType()+","+returnReg;
        }
        return null;
    }


    /**
     * f0 -> Expression()
     * f1 -> ExpressionTail()
     */
    public String visit(ExpressionList n, String argu) {

        String _ret=null;
        LinkedList<String> callParams = CallStack.pop(); //pop the callParameters list from last call

        String register , result;
        result = n.f0.accept(this, argu);
        register = getRegister(result);
        callParams.addLast(register); //add the last parameter

        CallStack.push(callParams); //push the list again
        n.f1.accept(this, argu);
        return _ret;
    }

    /**
     * f0 -> ( ExpressionTerm() )*
     */
    public String visit(ExpressionTail n, String argu) {
        return n.f0.accept(this, argu);
    }

    /**
     * f0 -> ","
     * f1 -> Expression()
     */
    public String visit(ExpressionTerm n, String argu) {

        String _ret=null;
        String result , register ;
        LinkedList<String> callParams = CallStack.pop(); //pop the callParameters list from last call

        result = n.f1.accept(this, argu);
        register = getRegister(result);

        callParams.addLast(register); //add the last parameter
        CallStack.push(callParams); //push the list again

        return _ret;
    }


    /**
     * f0 -> ArrayType()
     *       | BooleanType()
     *       | IntegerType()
     *       | Identifier()
     */
    public String visit(Type n, String argu) {
        return n.f0.accept(this, "type");
    }

    /**
     * f0 -> <INTEGER_LITERAL>
     */
    public String visit(IntegerLiteral n, String argu) {

        String lit , reg;
        lit =  n.f0.accept(this, argu);
        reg = newReg(); //save the integer literals in reg and return reg
        System.out.println("\t%"+reg+" = add i32 0, "+lit);

        return "int,"+ reg;
    }

    /**
     * f0 -> "true"
     */
    public String visit(TrueLiteral n, String argu) {

        String reg = newReg(); //save the true literals in reg as 1 and return reg
        System.out.println("\t%"+reg+" = add i1 1, 0");
        return "boolean,"+reg;
    }

    /**
     * f0 -> "false"
     */
    public String visit(FalseLiteral n, String argu) {

        String reg = newReg(); //save the true literals in reg as 0 and return reg
        System.out.println("\t%"+reg+" = add i1 0, 0");
        return "boolean,"+reg;
    }


    /**
     * f0 -> <IDENTIFIER>
     */
    public String visit(Identifier n, String argu) {


        if ( (argu != null ) && ( argu.equals("type")) ){ //if argu is "type" return the id of the class
            ClassInfo temp =  classExists(n.f0.accept(this, argu));
            if (temp != null)
                return temp.getClassId();
            else
                return null;
        }
        //if argu is "class" or "method return the id
        if ( (argu != null) && ( argu.equals("class") || argu.equals("method") )){
            return n.f0.accept(this, argu);
        }

        String idType;
        idType = this.varLookup(n.f0.accept(this, argu));
        //return type,id
        return idType+","+n.f0.accept(this, argu);
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

    /**
     * f0 -> "this"
     */
    public String visit(ThisExpression n, String argu) {
        thisUsed = true;
        return currentClass.getClassId()+",this";
    }


    /**
     * f0 -> "new"
     * f1 -> "boolean"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(BooleanArrayAllocationExpression n, String argu) {

        String result , exprType , numCellsReg;
        result = n.f3.accept(this, argu);
        numCellsReg = getRegister(result);
        exprType = getType(result);

        numCellsReg = checkVariable(numCellsReg,exprType);
        String arraySizeReg = newReg(); // add 4 to number of cells to have space to save the size of the array
        System.out.println("\t%"+arraySizeReg+" = add i32 4, %"+numCellsReg);

        String reg1 = newReg(); //check for negative size allocation
        System.out.println("\t%"+reg1+" = icmp sge i32 %"+arraySizeReg+", 4");

        String label = newLabel("if");
        String error_label = newLabel("if_error");
        System.out.println("\tbr i1 %"+reg1+", label %"+label+", label %"+error_label);

        String allocReg = newReg();
        String bitcastReg = newReg();
        System.out.println("\t"+error_label+":\n"+
                "\tcall void @throw_nsz()\n" +
                "\tbr label %"+label+"\n\n"+
                "\t"+label+":\n" +
                "\t%"+allocReg+" = call i8* @calloc(i32 %"+arraySizeReg+", i32 1)\n");

        System.out.println("\t%"+bitcastReg+" = bitcast i8* %"+allocReg+" to i32*");
        System.out.println("\tstore i32 %"+numCellsReg +", i32* %"+bitcastReg); //store the size in the first 4 indexes of the array
        String bitcastReg2 = newReg();//bitcast to i8*
        System.out.println("\t%"+bitcastReg2+" = bitcast i32* %"+bitcastReg+" to i8*");

        return "boolean[],"+allocReg;
    }

    /**
     * f0 -> "new"
     * f1 -> "int"
     * f2 -> "["
     * f3 -> Expression()
     * f4 -> "]"
     */
    public String visit(IntegerArrayAllocationExpression n, String argu) {


        String result , exprType , numCellsReg;
        result = n.f3.accept(this, argu);
        numCellsReg = getRegister(result);
        exprType = getType(result);

        numCellsReg = checkVariable(numCellsReg,exprType);
        String arraySizeReg = newReg(); // add 1 to number of cells to have space to save the size of the array
        System.out.println("\t%"+arraySizeReg+" = add i32 1, %"+numCellsReg);

        String reg1 = newReg(); //check for negative size allocation
        System.out.println("\t%"+reg1+" = icmp sge i32 %"+arraySizeReg+", 1");

        String label = newLabel("if");
        String error_label = newLabel("if_error");
        System.out.println("\tbr i1 %"+reg1+", label %"+label+", label %"+error_label);

        String allocReg = newReg();
        String bitcastReg = newReg();
        System.out.println("\t"+error_label+":\n"+
                "\tcall void @throw_nsz()\n" +
                "\tbr label %"+label+"\n\n"+
                "\t"+label+":\n" +
                "\t%"+allocReg+" = call i8* @calloc(i32 %"+arraySizeReg+", i32 4)\n" +
                "\t%"+bitcastReg+" = bitcast i8* %"+allocReg+" to i32*");

        System.out.println("\tstore i32 %"+numCellsReg +", i32* %"+bitcastReg); //store the size of the array in index 0
        return "int[],"+bitcastReg;
    }

    /**
     * f0 -> "new"
     * f1 -> Identifier()
     * f2 -> "("
     * f3 -> ")"
     */
    public String visit(AllocationExpression n, String argu) {

        String classId = n.f1.accept(this, "class");

        ClassInfo objectClass = classST.get(classId);
        int bytes = objectClass.getFieldNextOffset() + 8;
        String t1 = newReg(); //allocate the required memory on heap for our object
        System.out.println("\t%"+t1+" = call i8* @calloc(i32 1, i32 "+ bytes +")"); //bytes is a summary of 8 bytes (this) and the bytes which are required for the fields
        String t2 = newReg(); //set the vtable pointer to point to the correct vtable
        System.out.println("\t%"+t2+" = bitcast i8* %"+t1+" to i8***");
        String t3 = newReg(); //Get the address of the first element of the class_vtable
        System.out.println("\t %"+t3+" = getelementptr ["+objectClass.getMethodsSize()+" x i8*], ["+objectClass.getMethodsSize()+" x i8*]* @."+classId+"_vtable, i32 0, i32 0");
        System.out.println("\tstore i8** %"+t3+", i8*** %"+t2); //set the vtable to the correct address.

        return classId+","+t1;
    }

    /**
     * f0 -> "!"
     * f1 -> Clause()
     */
    public String visit(NotExpression n, String argu) {

        String result , reg, exprType ;
        result = n.f1.accept(this, argu);
        exprType = getType(result);
        reg = getRegister(result);

        String returnReg = newReg();
        reg = checkVariable(reg,exprType); //not expression is equal to xor a register with 1
        System.out.println("\t%"+returnReg+" = xor i1 1, %"+reg);
        return exprType+","+returnReg;


    }

    /**
     * f0 -> "("
     * f1 -> Expression()
     * f2 -> ")"
     */
    public String visit(BracketExpression n, String argu) {
        return n.f1.accept(this, argu);
    }

    public String visit(NodeToken n, String argu) {
        return n.toString();
    }
}