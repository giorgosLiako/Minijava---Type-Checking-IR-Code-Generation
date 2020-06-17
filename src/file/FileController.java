package file;
import java.util.*;
import java.io.*;


public class FileController {

    String path;
    PrintStream console = System.out; 
    PrintStream out ;

    public FileController(String path)
    {
        StringBuilder builder = new StringBuilder();

        for(int i=0 ; i < path.length() ; i++)
        {   
            char c =  path.charAt(i);

            if ( ( c == '.') && (i+4 < path.length()) && (path.charAt(i+1) == 'j') && (path.charAt(i+2) == 'a') && (path.charAt(i+3) == 'v') && (path.charAt(i+4) == 'a')  ){
                break;
            }
            builder.append(c) ;
        }
        builder.append(".ll");
        this.path = builder.toString();


    }

    public void createFileAndSetStream(){
        
        try{
            PrintStream out = new PrintStream(new File(path));
            System.setOut(out);  
    
        }
        catch(FileNotFoundException ex){
			System.err.println(ex.getMessage());
        }

    }

    public void recoverStreams()
    {
        try{
			System.setOut(console);  

		}
        catch(NullPointerException ex){
  			System.err.println(ex.getMessage());          
        }
    }
}