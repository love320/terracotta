package teetest;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class BasicPackTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(BasicPackTest.class);
  }

  public void testForVerification1() throws Exception {
	    boolean r = verifyRootFolder();
	    assertTrue("Root folder Exists!", r);
	    int iCount = verifyRootFolderFiles();
	    assertEquals(11, iCount);	    
	    //boolean d = verifyRootFolderFiles();
	    //assertTrue("Root folder files Exists!", d);
	    boolean b = verifyBinFolder();
		assertTrue("Bin folder exists!", b);
		int iBinCount = verifyBinFolderFiles();
	    assertEquals(7, iBinCount);
	    boolean d = verifyDemoFolder();
		assertTrue("Demo folder exists!", b);
	    int iDemoCount = verifyDemoFolderFiles();
	    assertEquals(10, iDemoCount); 
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  //Verify the root folder tc2.0.x exists
  public boolean verifyRootFolder() throws Exception { 
	     boolean bRootFolderExists = false;
		 String sRootFolder = null;
		 sRootFolder = System.getProperty("homedir.value");	 
		 if(sRootFolder != null) {		 
			 System.out.println("Root folder exists:" + sRootFolder);
			 bRootFolderExists = true;
		 }
		 return bRootFolderExists;	  
  } 
  
  //Verify that the 7 folders and 4 files exists in the root folder tc2.0.x
  public int verifyRootFolderFiles() throws Exception {
		 int iRootFiles = 0;		 
		 String sRootFolder = null;		 	
		 System.out.println("************************************");
		 sRootFolder = System.getProperty("homedir.value");		 
		 System.out.println("files location="+sRootFolder);
		 File f = new File(sRootFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iRootFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iRootFiles;		 	  
  } 

 //Verify the bin folder exists under root folder tc2.0.x
  public boolean verifyBinFolder() throws Exception { 
     boolean bBinFolderExists = false;
	 String sBinFolder = null;
	 sBinFolder = System.getProperty("bindir.value");	 
	 if(sBinFolder != null) {		 
		 System.out.println("Bin folder exists:" + sBinFolder);
		 bBinFolderExists = true;
	 }
	 return bBinFolderExists;	  
  } 
  //Verify that the 8 files exists in the bin folder 
  public int verifyBinFolderFiles() throws Exception {
		 int iBinFiles = 0;		 
		 String sBinFolder = null;		 	
		 System.out.println("************************************");
		 sBinFolder = System.getProperty("bindir.value");		 
		 System.out.println("files location="+sBinFolder);
		 File f = new File(sBinFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iBinFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){				 
				System.out.println("file name="+fArray[i].getName());				
			 }
		 }
		 System.out.println("************************************");
		 return iBinFiles;		 	  
}
  
  //Verify the demo folder exists under root folder tc2.0.x
  public boolean verifyDemoFolder() throws Exception { 
	     boolean bDemoFolderExists = false;
		 String sDemoFolder = null;
		 sDemoFolder = System.getProperty("demodir.value");	 
		 if(sDemoFolder != null) {		 
			 System.out.println("Demo folder exists:" + sDemoFolder);
			 bDemoFolderExists = true;
		 }
		 return bDemoFolderExists;	  
  }  
  //Verify that the 5 folders and 5 files exists in the demo folder 
  public int verifyDemoFolderFiles() throws Exception {
		 int iDemoFiles = 0;		 
		 String sDemoFolder = null;		 	
		 System.out.println("************************************");
		 sDemoFolder = System.getProperty("demodir.value");		 
		 System.out.println("files location="+sDemoFolder);
		 File f = new File(sDemoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iDemoFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iDemoFiles;		 	  
  }
  
}
