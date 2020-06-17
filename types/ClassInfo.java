package types;
import java.util.*;


public class ClassInfo {

    private String classId;

    private HashMap<String, FieldInfo> fields;
    private LinkedHashMap<String, MethodInfo> methods;

    private LinkedHashMap<String,Integer> methodOffsets;
    private LinkedHashMap<String,Integer> fieldOffsets;

    private ClassInfo parent;

    private int fieldNextOffset;
    private int methodNextOffset;


    public ClassInfo(String id){
        fields   = new HashMap<String, FieldInfo>();
        methods  = new LinkedHashMap<String, MethodInfo>();

        methodOffsets = new LinkedHashMap<String,Integer>();
        fieldOffsets  = new LinkedHashMap<String,Integer>();

        classId = id;
        parent   = null;

        fieldNextOffset = 0;
        methodNextOffset = 0 ;
    }

    public ClassInfo(String id , ClassInfo parent , int fieldNextOffset , int methodNextOffset){
        fields   = new HashMap<String, FieldInfo>();
        methods  = new LinkedHashMap<String, MethodInfo>();

        methodOffsets = new LinkedHashMap<String,Integer>();
        fieldOffsets  = new LinkedHashMap<String,Integer>();        

        classId = id;
        this.parent   = parent;

        this.fieldNextOffset = fieldNextOffset;
        this.methodNextOffset = methodNextOffset ;
    }

    public String getClassId() {
        return classId;
    }

    // get 

    public LinkedHashMap<String, MethodInfo> getMethods(){
        return methods;
    }

    public int getFieldNextOffset(){
        return fieldNextOffset;
    }

    public int getMethodNextOffset(){
        return methodNextOffset;
    }

    public LinkedHashMap<String,Integer> getMethodOffsets(){
        return methodOffsets;
    }

    public LinkedHashMap<String,Integer> getFieldOffsets(){
        return fieldOffsets;
    }   

    public ClassInfo getParentClass(){
        return parent;
    }

    public String getFieldType(String id){
        if ( this.fields.containsKey(id) == true){
            return this.fields.get(id).getFieldType();
        }
        return null;
    }

    public int getMethodsSize(){
        int size = 0;

        size = methods.size();

        if (methods.containsKey("main") == true)
            size=size-1;

        if( parent != null){
            for (String keys : methods.keySet())
		    {
                MethodInfo method = methods.get(keys);
                if (parent.methodExists(method.getMethodId()) != null)
                    size = size - 1;
            }

            return size + parent.getMethodsSize();
        }
        return size;
    }

    // set

    public void setFieldNextOffset(int nextOffset){
        this.fieldNextOffset = nextOffset;
    }

    public void setMethodNextOffset(int nextOffset){
        this.methodNextOffset = nextOffset;
    }

    //operations

    

    public void addMethod(String methodName, MethodInfo method){
        this.methodOffsets.put(methodName,method.getMethodOffset());

        this.methods.put(methodName, method);
    }


    public void addField(String fieldName, FieldInfo field){
        this.fieldOffsets.put(fieldName, field.getFieldOffset());

        this.fields.put(fieldName, field);
    }


    public int searchMethod(String methodId  ){

        if ( this.methods.containsKey(methodId) == true ){
            MethodInfo method = methods.get(methodId);
            return method.getMethodOffset();
        }
        else if (parent != null){
            return this.parent.searchMethod(methodId);
        } 
        else
            return -1;
    }
    
    public MethodInfo methodExists(String methodId  ){

        if ( this.methods.containsKey(methodId) == true ){
            MethodInfo method = methods.get(methodId);
            return method;

        } 
        else if (parent != null){
            return this.parent.methodExists(methodId);
        }
        else
            return null;
    }
    
    
    public MethodInfo methodExistsInCurrentClass(String methodId  ){

        if ( this.methods.containsKey(methodId) == true ){
            MethodInfo method = methods.get(methodId);
            return method;

        } 
        else
            return null;
    }

    public boolean fieldExists(String fieldId ){

        if ( this.fields.containsKey(fieldId) == true){
            return true;
        }

        return false;
    }

    public FieldInfo getField(String fieldId){
        if ( this.fields.containsKey(fieldId) == true){
            FieldInfo field = fields.get(fieldId);
            return field;
        }
        if (parent != null)
        {
            return parent.getField(fieldId);
        }

        return null;
    }

    public boolean traverseParentClasses(String classId){
        if ( this.classId.equals(classId) )
        {
            return true;
        }
        if (this.parent != null)
        {
            return this.parent.traverseParentClasses(classId);
        }
        return false;
    }

    public MethodInfo checkParentClassesForMethod(int wantedOffset){

        boolean found = false;
        MethodInfo method = null;
        for (String methodsKeys : methodOffsets.keySet()){

            // method = methods.get(methodsKeys);
            if (methodsKeys.equals("main"))
                continue;

            int offset = methodOffsets.get(methodsKeys);    
            if(offset == wantedOffset)
            {
                method = methods.get(methodsKeys);
                found = true;
                break;
            }
        }

        if (found == true)
            return method;

        if(parent != null)
        {
            return parent.checkParentClassesForMethod(wantedOffset);
        }

        return null;


    }

    public void print(){
        System.out.println("class : "+classId );
        System.out.println("Fields :");
        for (String keys : fields.keySet())
		{
            FieldInfo field = fields.get(keys);
            field.print();
        }
        System.out.println("Method :");
        for (String keys : methods.keySet())
		{
            MethodInfo method = methods.get(keys);
            method.print();
        }

        System.out.println("\n\n");

    }


}