/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Properties;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;

import sun.misc.BASE64Encoder;

public class FileUtil {
	public static void saveString(File filename, String content) {
		FileWriter out=null;
		try {
			out=new FileWriter(filename);
			out.write(content);
		}
		catch (IOException e) { throw new RuntimeException(e); }
		finally {
			if (out!=null) {
				try {
					out.close();
				}
				catch (IOException e) { throw new RuntimeException(e); }
			}
		}
	}

	public static void load(Properties props, String filename) {
		FileInputStream inp = null;
		try {
			inp =new FileInputStream(filename);
			props.load(inp);
		} 
		catch (java.io.IOException e) { throw new RuntimeException(e);  }
		finally {
			try {
				if (inp!=null) 
					inp.close();
			}
			catch (java.io.IOException e) { throw new RuntimeException(e);  }
		}
	}



	public static String loadString(String filename) {
		BufferedReader inp = null;
		try {
			inp =new BufferedReader(new FileReader(filename));
			StringBuilder result=new StringBuilder();
			String line;
			while ((line=inp.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			return result.toString();
		} 
		catch (java.io.IOException e) { throw new RuntimeException(e);  }
		finally {
			try {
				if (inp!=null) 
					inp.close();
			}
			catch (java.io.IOException e) { throw new RuntimeException(e);  }
		}
	}

	/**
	 * Checks for the existence of the given file
	 * 
	 * @param fileName 
	 * @return boolean returns true if the file exists, false if it doesn't or if the fileName is null
	 */
	public static boolean isFileExists(String fileName){
		if(fileName==null)
			return false;
		File file = new File(fileName);
		return file.exists();
	}
	
	/**
	 * This webService encodes the files content in Base64 format
	 * 
	 * Usage: For uploading an ISVP to a remote node in the cluster,
	 * the isvp file has to be encoded and uploaded. This webService
	 * encodes the isvp content
	 * 
	 * @param filePath
	 * @return String Base64 encoded content of the file
	 */
	public static String  encodeFile(String filePath)
	{
		try {			
			FileInputStream fin = new FileInputStream(filePath);
			byte[] fileContent = new byte[fin.available()];
			DataInputStream din = new DataInputStream(fin);
			din.readFully(fileContent);
			BASE64Encoder encoder = new BASE64Encoder();
			return new String(encoder.encode(fileContent));
			
		} catch (Exception  e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * This webService looks up for the properties file and loads it after finding it.
	 * It first looks up at the location mentioned in 'system.<<systemName>>.properties.file' property in caas.conf
	 * If not then looks up for the '<<systemName>>.properties' file in the current directory
	 * If not then look up for the '<<systemName>>.properties' in logged in user's home directory
	 * 
	 * @param systemName - Cordys system name as mentioned in the caas.conf file
	 * @return String - A file path  corresponding to the properties of the given system
	 */
	/*public static String getSystemPropertiesFilePath(String systemName){

		String fileName=null; 
		if(systemName==null)
			throw new CaasRuntimeException("Unable to load the properties as the Cordys system name is null");
		
		//File name of the properties file mentioned in caas.conf file - Highest Precedence
		String propsFileInConf = Environment.get().getProp("system."+systemName+".properties.file", null);
		//File name of the properties file in current directory - Second Highest Precedence
		String propsFileInPWD = systemName+".properties";
		//File name of the properties file in user's home directory - Lowest Precedence
		String propsFileInHomeDir = System.getProperty("user.home")+"/config/caas/"+systemName+".properties";
		//Properties props = new Properties();
		//Convert the file paths to Unix file path format
		propsFileInConf = StringUtil.getUnixStyleFilePath(propsFileInConf);
		propsFileInHomeDir = StringUtil.getUnixStyleFilePath(propsFileInHomeDir);
		
		String[] fileNames = new String[]{propsFileInConf, propsFileInPWD, propsFileInHomeDir};
		//Determine the file that need to be considered for loading
		//To do so, Loop over the files as per their precedence and check for their existence  
		for(String  aFileName:fileNames){ 
			if(FileUtil.isFileExists(aFileName)){ 
				fileName = aFileName;
				break;
			}
		}
		return fileName;
	}*/
	
}
