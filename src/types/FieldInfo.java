package types;
import java.util.*;

public class FieldInfo {


    private String fieldId;
    private String type;

    private int offset;

    private ClassInfo currentClass;


    public FieldInfo(ClassInfo cl , String id , String type , int offset){
        
        this.fieldId = id;
        this.type = type;
        this.offset = offset;

        this.currentClass  = cl;
    }

    //get 

    public String getFieldId() {
        return fieldId;
    }

    public String getFieldType() {
        return type;
    }

    public int getFieldOffset(){
        return offset;
    }    

    //set 

    public void setFieldOffset(int offset){
        this.offset = offset;
    }

    public void setFieldId(String fieldId) {
        this.fieldId = fieldId;
    }

    public void setFieldType(String type) {
        this.type = type;

    }

    //operations

    public void print(){
        System.out.println( type +"  "+ fieldId + "( " + offset + " )");
    }
}