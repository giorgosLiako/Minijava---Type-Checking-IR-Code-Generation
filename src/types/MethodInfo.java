package types;
import java.util.*;


public class MethodInfo {


    private String methodId;

    private String type;

    private int offset;

    private LinkedHashMap<String, String> parameters;
    private HashMap<String, String> locals;

    private ClassInfo currentClass;


    public MethodInfo(ClassInfo cl , String id , String type , int offset ){
        locals   = new HashMap<String, String>();
        parameters   = new  LinkedHashMap<String, String>();


        this.methodId = id ;
        this.type = type;
        this.offset = offset;

        currentClass   = cl;
    }


    //get

    public String getCurrentClassId(){
        return currentClass.getClassId();
    }

    public String getMethodId() {
        return methodId;
    }

    public int getMethodOffset() {
        return offset;
    }

    public String getMethodType() {
        return type;
    }

    public String getParameterType(String id){
        if ( this.parameters.containsKey(id) == true){
            return this.parameters.get(id);
        }
        return null;
    }

    public LinkedHashMap<String, String> getParametersList(){
        return this.parameters;
    }

    public int getParametersNumber(){
        return this.parameters.size();
    }

    public String getLocalType(String id){
        if ( this.locals.containsKey(id) == true){
            return this.locals.get(id);
        }
        return null;
    }    

    //set

    public void setMethodId(String methodId) {
        this.methodId = methodId;
    }

     public void setMethodOffset(int offset) {
        this.offset = offset;
    }


    //operations 

    public void addParameter(String id, String type){
        this.parameters.put(id, type);
    }


    public void addLocal(String id, String type){
        this.locals.put(id, type);
    }

    public boolean parameterExists(String id){
        if ( this.parameters.containsKey(id) == true){
            return true;
        }
        return false;
    }

    public boolean localExists(String id){
        if ( (this.locals.containsKey(id) == true) || (this.parameters.containsKey(id) == true)){
            return true;
        }
        return false;
    }

    public void print(){
        System.out.println( type +"  "+ methodId + "( " + offset + " )");
        System.out.println("Parameters: ");
        // Generating a Set of entries
         Set set = parameters.entrySet();

         // Displaying elements of LinkedHashMap
         Iterator iterator = set.iterator();
         while(iterator.hasNext()) {
            Map.Entry me = (Map.Entry)iterator.next();
            System.out.println(me.getKey() + " " +me.getValue());
         }
        System.out.println("Locals: ");
        for (String keys : locals.keySet())
		{
            System.out.println(locals.get(keys) + "  " + keys);
        }
    }
}