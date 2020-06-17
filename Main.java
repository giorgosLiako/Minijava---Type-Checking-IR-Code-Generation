import syntaxtree.*;
import java.io.*;
import visitors.*;
import types.*;
import file.*;
class Main {

    public static void main (String [] args){
		if(args.length < 1)
		{
			System.err.println("Usage: java Driver <inputFile>");
			System.exit(1);
		}

		FileInputStream fis = null;
		
		try
		{
			for(int i=0 ; i < args.length; i++)
			{	System.out.println("\nFILE: " + args[i] +"\n");
				fis = new FileInputStream(args[i]);
				MiniJavaParser parser = new MiniJavaParser(fis);
				System.err.println("Program parsed successfully.");

				DeclVisitor decl = new DeclVisitor();

				Goal root = parser.Goal();
				root.accept(decl, null);

				boolean errorFound = decl.getErrorFound();
				if (errorFound == true) {
					continue;
				}
				TypeCheckVisitor expr = new TypeCheckVisitor(decl.getClassST());
				root.accept(expr,null);
				errorFound = expr.getErrorFound();
				if (errorFound == true) {
					continue;
				}
				System.err.println("Program passed type checking.");


				FileController file = new FileController(args[i]);
				file.createFileAndSetStream();

				GenCodeVisitor code = new GenCodeVisitor(decl.getClassST());
				root.accept(code,null);


				System.err.println("Intermediate code generated successfuly.");

				file.recoverStreams();
			}
		}
		catch(ParseException ex)
		{
			System.out.println(ex.getMessage());
		}
		catch(FileNotFoundException ex)
		{
			System.err.println(ex.getMessage());
		}
		finally
		{
			try
			{
				if(fis != null)
					fis.close();
			}
			catch(IOException ex)
			{
				System.err.println(ex.getMessage());
			}
		}
    }
}