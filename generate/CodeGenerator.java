package generate;
import java.util.*;
import types.*;


public class CodeGenerator {

    int REG ;
    int Label;

    public CodeGenerator(){
        REG = 0;
        Label = 0;
    }

    public String newReg(){
        String temp = "_t"+REG;
        REG++;
        return temp;
    }

    public String newLabel(String type){
        String label = type+Label;
        Label++;
        return label;
    }


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

    public void buildVtable(ClassInfo currentClass){

        LinkedHashMap<String,Integer> methodOffsets = currentClass.getMethodOffsets();
        MethodInfo method=null;

        boolean firstMethod = true;
        int wantedOffset = 0 ; 
        while(wantedOffset >= 0)
        {   
            int offset;
            boolean found = false;

            for (String methodsKeys : methodOffsets.keySet()){

                if ( methodsKeys.equals("main")) 
                    continue;

                offset = methodOffsets.get(methodsKeys);    
                if(offset == wantedOffset)
                {   
                    LinkedHashMap<String,MethodInfo> methods = currentClass.getMethods(); 
                    method = methods.get(methodsKeys);
                    found = true;
                    break;
                }
            }

            ClassInfo parent = currentClass.getParentClass();
            if( (found == false) && (parent != null) )
            {
                method = parent.checkParentClassesForMethod(wantedOffset);
                if (method !=null)
                    found = true;
            }

            if (found == true)
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
                    String type = me.getKey().toString();
                    System.out.print(","+ getAssemblyType(methodType));
                }

                String classId = method.getCurrentClassId();
                System.out.print(")* @"+classId+"."+methodId +" to i8*)");
                firstMethod = false;

                wantedOffset = wantedOffset + 8;
                
            }
            else
                wantedOffset = -1;
        }   
    }

    public void startOfGenerate(HashMap<String,ClassInfo> classST){
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

    public String storeLiteral(String s){
        System.out.println("\n\t;storeLiteral Start\n");

        String reg = newReg();
        //System.out.println("\t%"+reg+"= alloca i32");
        //System.out.println("\tstore i32 "+ s +",i32* %"+reg);
        System.out.println("\t%"+reg+" = add i32 0, "+s);
        System.out.println("\n\t;storeLiteral End\n");

        return reg;
    }

    public void MainClassEnds(){
        System.out.println("\tret i32 0\n}\n");
    }

    public void VarDeclaration(String type , String id){
        System.out.println("\n\t;VarDeclaration Start\n");

        System.out.println("\t%"+id+" = "+"alloca "+ getAssemblyType(type));

        System.out.println("\t;VarDeclaration End\n");
    }

    public void IfStatement(String reg,String labelIf,String labelElse){
        System.out.println("\tbr i1 %"+reg+", label %"+labelIf+", label %"+labelElse);
    }

    public void PrintStatement(String reg){
        System.out.println("\t;PrintStatement Start\n");


        System.out.println("\tcall void (i32) @print_int(i32 %"+reg+")");

        System.out.println("\t;PrintStatement End\n");

    }

    public void ArrayAssignmentStatement(String idType, String idreg,String reg1 ,String reg2){
        System.out.println("\n\t;ArrayAssignmentStatement Start\n");

        if (idType.equals("int[]")){
            String arrayaddr = newReg();
            System.out.println("\t%"+arrayaddr+" = load i32*, i32** %"+idreg);
            String size = newReg();
            System.out.println("\t %"+size+" = load i32, i32* %"+arrayaddr);

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
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
            String indexPtr = newReg();
            System.out.println( "\t%"+index+" = add i32 1, %"+reg1+"\n"+
                                "\t%"+indexPtr+" = getelementptr i32, i32* %"+arrayaddr+", i32 %"+index);

            System.out.println("\tstore i32 %"+ reg2 +",i32* %"+indexPtr);

        }
        else{

        }
        System.out.println("\n\t;ArrayAssignmentStatement End\n");

    }

    public void AssignmentStatement(String idType , String id , String exprType , String exprReg){
        //String midReg;
        //midReg = newReg();
        //System.out.println("\t%"+ midReg+" = load "+ getAssemblyType(idType) +", "+getAssemblyType(exprType)+"* %"+exprReg);
        System.out.println("\n\t;AssignmentStatement Start\n");

        System.out.println("\tstore "+getAssemblyType(idType)+ " %"+ exprReg +","+getAssemblyType(idType)+"* %"+id);

        System.out.println("\t;AssignmentStatement End\n");

    }

    public String CompareExpression(String reg1 ,String reg2 ,String type){
        System.out.println("\n\t;CompareExpression Start\n");

        String temp = newReg();
        String reg = newReg();
        System.out.println("\t%"+temp+" = load i32, i32* %"+reg1);
        System.out.println("\t%"+reg+" = icmp slt "+getAssemblyType(type)+" %"+temp+", %"+reg2);

        System.out.println("\n\t;CompareExpression End\n");

        return reg;
    }

    public String PlusExpression(String reg1 ,String reg2 ,String type){
        System.out.println("\n\t;PlusExpression Start\n");

        String reg = newReg();
        System.out.println("\t%"+reg+" = add "+getAssemblyType(type)+" %"+reg1+", %"+reg2);

        System.out.println("\n\t;PlusExpression End\n");

        return reg;
    }

    public String MinusExpression(String reg1 ,String reg2 ,String type){
        System.out.println("\n\t;MinusExpression Start\n");

        String reg = newReg();
        System.out.println("\t%"+reg+" = sub "+getAssemblyType(type)+" %"+reg1+", %"+reg2);

        System.out.println("\n\t;MinusExpression End\n");

        return reg;
    }

    public String TimesExpression(String reg1 ,String reg2 ,String type){
        System.out.println("\n\t;TimesExpression Start\n");

        String reg = newReg();
        System.out.println("\t%"+reg+" = mul "+getAssemblyType(type)+" %"+reg1+", %"+reg2);

        System.out.println("\n\t;TimesExpression End\n");

        return reg;
    }

    public String ArrayLookup( String exprType1 , String reg1 ,String exprType2 ,String reg2 ){

        System.out.println("\n\t;ArrayLookUp Start\n");
        if (exprType1.equals("int[]")){

            String arrayaddr = newReg();
            System.out.println("\t%"+arrayaddr+" = load i32*, i32** %"+reg1);
            String size = newReg();
            System.out.println("\t %"+size+" = load i32, i32* %"+arrayaddr);

            String temp3 , temp4 , temp5;
            temp3 = newReg();
            temp4 = newReg();
            temp5 = newReg();
            String errorLabel = newLabel("if_error");
            String label = newLabel("if");
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
            String indexPtr = newReg();
            System.out.println( "\t%"+index+" = add i32 1, %"+reg2+"\n"+
                                "\t%"+indexPtr+" = getelementptr i32, i32* %"+arrayaddr+", i32 %"+index);

            String returnreg  = newReg();
            System.out.println("\t%"+returnreg+" = load i32, i32* %"+indexPtr);

            System.out.println("\t;ArrayLookUp End\n");

            return returnreg;
        }
        else{
            System.out.println("\t;ArrayLookUp End\n");

            return "ok";
        }

    }

    public String BooleanArrayAllocationExpression(String numCellsReg)
    {
        System.out.println("\n\t;BooleanArrayAllocationExpression Start\n");

        String arraySizeReg = newReg();
        System.out.println("\t%"+arraySizeReg+" = add i32 4, %"+numCellsReg);

        String reg1 = newReg();
        System.out.println("\t%"+reg1+" = icmp sge i32 %"+arraySizeReg+", 4");

        String label = newLabel("if");
        String error_label = newLabel("if_error");
        System.out.println("\tbr i1 %"+reg1+", label %"+label+", label %"+error_label);

        String allocReg = newReg();
        //String bitcastReg = newReg();
        System.out.println("\t"+error_label+":\n"+
                "\tcall void @throw_nsz()\n" +
                "\tbr label %"+label+"\n\n"+
                "\t"+label+":\n" +
                "\t%"+allocReg+" = call i8* @calloc(i32 %"+arraySizeReg+", i32 1)\n");

        System.out.println("\tstore i32 %"+numCellsReg +", i32* %"+allocReg);

        System.out.println("\t;BooleanArrayAllocationExpression End\n");

        return allocReg;
    }

    public String IntegerArrayAllocationExpression(String numCellsReg){
        System.out.println("\n\t;IntegerArrayAllocationExpression Start\n");

        String arraySizeReg = newReg();
        System.out.println("\t%"+arraySizeReg+" = add i32 1, %"+numCellsReg);

        String reg1 = newReg();
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

        System.out.println("\tstore i32 %"+numCellsReg +", i32* %"+bitcastReg);


        System.out.println("\t;IntegerArrayAllocationExpression End\n");

        return bitcastReg;
    }
}