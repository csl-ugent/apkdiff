package Util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvValidationException;

public class IO {

	public static boolean directoryExists(String path){
	    File directory = new File(path);
	    return directory.exists();
	}

	public static boolean fileExists(String path) {
		return directoryExists(path);
	}

	public static boolean writeToFile(String path, String content){
		//path including name!
	    File file = new File(path);
	    try{
	        FileWriter fw = new FileWriter(file.getAbsoluteFile());
	        BufferedWriter bw = new BufferedWriter(fw);
	        bw.write(content);
	        bw.close();
	        System.out.println("[IO] Successfully wrote to file: " + path);
	    }
	    catch (IOException e){
	        e.printStackTrace();
	        System.exit(-1);
	    }

		return true;
	}

	public static String readFromFile(String path){
		String output = "";
	    try {
	        File myObj = new File(path);
	        Scanner myReader = new Scanner(myObj);
	        while (myReader.hasNextLine()) {
	        	output += myReader.nextLine();
	        }
	        myReader.close();
	      } catch (FileNotFoundException e) {
	        System.out.println("An error occurred.");
	        e.printStackTrace();
	      }
	    System.out.println("[IO] Successfully read from file: " + path);
	    return output;
	}

	public static int getPercentage(int a, int b){
		return (int) Math.round((double)a/(double)b*100);
	}

	public static String formatPercentage(int a, int b){
		return "" + String.valueOf(a) + "/" + String.valueOf(b) + " (" + String.valueOf(getPercentage(a, b)) + "%)";
	}

	public static void writeRowToCSV(String filePath, List<String> header, List<String> data) {
		//write to csv, if header is provided, write to new file, if header is null, append to existing

	    // first create file object for file placed at location
	    // specified by filepath
	    File file = new File(filePath);
	    boolean alreadyExists = file.exists();
	    boolean writeHeaders = false;
	    if(alreadyExists){
	    	try {
	    		CSVReader reader = new CSVReader(new FileReader(filePath));
	    		// if the first line is the header
				String[] existingHeader = reader.readNext();
		        for (int i = 0; i < header.size(); i++) {
		        	if(i < existingHeader.length){
			            if (!header.get(i).equals(existingHeader[i])){
			            	System.out.println("[WARNING] Headers don't match! Writing new headers.");
			            	writeHeaders = true;
			            }
		        	}
		        	else{
		            	System.out.println("[WARNING] Headers don't match! Writing new headers.");
		            	writeHeaders = true;
		            	break;
		        	}

		        }
	    	}
	    	catch(Exception e){
	    		e.printStackTrace();
	    	}
	    }
	    try {
	        // create FileWriter object with file as parameter
	    	 FileWriter outputfile = new FileWriter(file, true); //true = append boolean


	        // create CSVWriter object filewriter object as parameter
	        CSVWriter writer = new CSVWriter(outputfile);

	        // adding header to csv
	        if(!alreadyExists || writeHeaders){
	        	String[] itemsArray = new String[header.size()];
	            itemsArray = header.toArray(itemsArray);
	        	writer.writeNext(itemsArray);
	        }

	        // add data to csv
        	String[] itemsArray = new String[data.size()];
            itemsArray = data.toArray(itemsArray);
        	writer.writeNext(itemsArray);

	        // closing writer connection
	        writer.close();

	        //log
	        System.out.println("[IO] Wrote csv row to " + filePath);
	    }
	    catch (IOException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	    }
	}


	public static void writeObject(String filePath, Object obj){
	      try {
	          FileOutputStream fileOut =
	          new FileOutputStream(filePath);
	          ObjectOutputStream out = new ObjectOutputStream(fileOut);
	          out.writeObject(obj);
	          out.close();
	          fileOut.close();
	          System.out.println("[IO] Serialized data is saved in " + filePath);
	       } catch (IOException i) {
	          i.printStackTrace();
	       }
	}

	public static Object readObject(String filePath) {
		if(!fileExists(filePath)) return null;
		Object obj = null;
	      try {
	          FileInputStream fileIn = new FileInputStream(filePath);
	          ObjectInputStream in = new ObjectInputStream(fileIn);
	          obj = in.readObject();
	          in.close();
	          fileIn.close();
	          System.out.println("[IO] Serialized data read from " + filePath);
	       } catch (IOException | ClassNotFoundException i) {
	          i.printStackTrace();
	       }
	      return obj;
	}

}
