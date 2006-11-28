package test;

import junit.framework.*;
import java.net.*;
import java.util.*;
import java.io.*;

public class CommonTSTTest extends TestCase {

  protected void setUp() {
  }

  public static Test suite() {
    return new TestSuite(CommonTSTTest.class);
  }

  public void testForVerification1() throws Exception {
	    boolean r = verifyRootFolder(); //1
	    assertTrue("Root folder Exists!", r); 
	    int iCount = verifyRootFolderFiles(); //2
	    assertEquals(9, iCount);
	    int iCommonCount = verifyCommonFolderFiles(); //3
	    assertEquals(1, iCommonCount);	    
	    int  iCommonLibCount = verifyCommonLibFolderFiles(); //4
	    assertEquals(39, iCommonLibCount);
	    int iCommonLibDsoCount = verifyCommonLibDsobootFolderFiles(); //5
	    assertEquals(2, iCommonLibDsoCount);
	    int iCommonLibTomcatCount = verifyCommonLibTomcatFolderFiles(); //6
	    assertEquals(1, iCommonLibTomcatCount);
	    int iCommonLibWeblogicCount = verifyCommonLibWeblogicFolderFiles(); //7
	    assertEquals(1, iCommonLibWeblogicCount);
  }  
  
  public static void main(String[] args) {
    junit.textui.TestRunner.run(suite());
  }

  //1. Verify the root folder terracotta-2.1.0 exists
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
  //2. Verify that the 4 folders and 5 files exists in the root folder terracotta-2.1.0
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
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iRootFiles;		 	  
  } 
  //3. Verify that the 1 folder exists in the terracotta-2.1.0/common
  public int verifyCommonFolderFiles() throws Exception {
		 int iCommonFiles = 0;		 
		 String sCommonFolder = null;		 	
		 System.out.println("************************************");
		 sCommonFolder = System.getProperty("common.dir");		 
		 System.out.println("files location="+sCommonFolder);
		 File f = new File(sCommonFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCommonFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iCommonFiles;		 	  
  } 
 
  //4. Verify that the 3 folder and 36 files exists in the terracotta-2.1.0/common/lib
  public int verifyCommonLibFolderFiles() throws Exception {
		 int iCommonLibFiles = 0;		 
		 String sCommonLibFolder = null;		 	
		 System.out.println("************************************");
		 sCommonLibFolder = System.getProperty("libdir.value");		 
		 System.out.println("files location="+sCommonLibFolder);
		 File f = new File(sCommonLibFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCommonLibFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iCommonLibFiles;		 	  
  }
  //5. Verify that the 2 files exists in the terracotta-2.1.0/common/lib/dso-boot
  public int verifyCommonLibDsobootFolderFiles() throws Exception {
		 int iCommonLibDsoFiles = 0;		 
		 String sCommonLibDsoFolder = null;		 	
		 System.out.println("************************************");
		 sCommonLibDsoFolder = System.getProperty("libdsodir.value");		 
		 System.out.println("files location="+sCommonLibDsoFolder);
		 File f = new File(sCommonLibDsoFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCommonLibDsoFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iCommonLibDsoFiles;		 	  
  }   
  //6. Verify that the 1 file exists in the terracotta-2.1.0/common/lib/tomcat
  public int verifyCommonLibTomcatFolderFiles() throws Exception {
		 int iCommonLibTomcatFiles = 0;		 
		 String sCommonLibTomcatFolder = null;		 	
		 System.out.println("************************************");
		 sCommonLibTomcatFolder = System.getProperty("libtomcatdir.value");		 
		 System.out.println("files location="+sCommonLibTomcatFolder);
		 File f = new File(sCommonLibTomcatFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCommonLibTomcatFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iCommonLibTomcatFiles;		 	  
  }
 //7. Verify that the 1 file exists in the terracotta-2.1.0/common/lib/weblogic
  public int verifyCommonLibWeblogicFolderFiles() throws Exception {
		 int iCommonLibWeblogicFiles = 0;		 
		 String sCommonLibWeblogicFolder = null;		 	
		 System.out.println("************************************");
		 sCommonLibWeblogicFolder = System.getProperty("libweblogicdir.value");		 
		 System.out.println("files location="+sCommonLibWeblogicFolder);
		 File f = new File(sCommonLibWeblogicFolder);
		 System.out.println("get absolute path="+f.getAbsolutePath());
		 File f1 = new File(f.getAbsolutePath());		 
		 
		 File fArray[] = new File[0];
		 fArray = f1.listFiles();			 
		 System.out.println("file array size="+fArray.length);
		 
		 if(fArray.length != 0){
			 iCommonLibWeblogicFiles = fArray.length;
			 for(int i=0; i<fArray.length;i++){
				 if(fArray[i].isDirectory() == true){
					 System.out.println("folder name="+fArray[i].getName());
					 //System.out.println("folder name="+fArray[i].getAbsolutePath());
				 }else {
					 System.out.println("file name="+fArray[i].getName());
				 }
			 }
		 }
		 System.out.println("************************************");
		 return iCommonLibWeblogicFiles;		 	  
  }
}