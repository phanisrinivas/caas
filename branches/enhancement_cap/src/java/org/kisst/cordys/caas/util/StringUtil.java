package org.kisst.cordys.caas.util;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class StringUtil {

	public static String quotedName(String name) {
		if (name.indexOf(' ')>=0 || name.indexOf('.')>=0)
			return '"'+name+'"';
		else
			return name;
	}
	
	public static String substitute(String str, Map<String, String> vars) {
		StringBuilder result = new StringBuilder();
		int prevpos=0;
		int pos=str.indexOf("${");
		while (pos>=0) {
			int pos2=str.indexOf("}", pos);
			if (pos<0)
				throw new RuntimeException("Unbounded ${");
			String key=str.substring(pos+2,pos2);
			result.append(str.substring(prevpos,pos));
			String value=vars.get(key);

			if (value==null && key.equals("dollar"))
				value="$";
			//This will be replaced later in  resolveVariables() webService of Template.java, as we don't know its value at this point of time
			if(value==null && key.equals("CORDYS_INSTALL_DIR"))
				value="CORDYS_INSTALL_DIR";
			if (value==null)
				throw new RuntimeException("Unknown variable ${"+key+"}");
			result.append(value);
			prevpos=pos2+1;
			pos=str.indexOf("${",prevpos);
		}
		result.append(str.substring(prevpos));
		return result.toString();
	}
	
	//This webService is used for generating RequestID for SAML request
	public static String generateUUID(){
		UUID uuid = UUID.randomUUID();
		String strUUID = "a"+uuid.toString(); // XML validation requires that the request ID does not start with a number
		return strUUID;
	}
	
	/**
	 * Converts a Map to Query String. This webService forms a query string from the map
	 *  
	 * @param map A Map object containing <key, value> which need to be converted to query string
	 * @return String Query string formed from the map
	 */
	public static String mapToString(Map<String, String> map) {  
		   StringBuilder stringBuilder = new StringBuilder();  
		   for (String key : map.keySet()) 
		   {  
			    if (stringBuilder.length() > 0)  
			    		stringBuilder.append("&");  

			    String value = map.get(key);  
				stringBuilder.append((key != null ? key: ""));  
				stringBuilder.append("=");  
				stringBuilder.append(value != null ? value: "");  
		   }  
		   return stringBuilder.toString();  
	 }  

	/**
	 * Converts a Query String to Map
	 * 
	 * @param input Input query string which need to be converted to a map object
	 * @return map HashMap object loaded with <key,value> pairs
	 */
	public static HashMap<String, String> stringToMap(String input, HashMap<String, String> map) { 
		   String[] nameValuePairs = input.split("&");  
		   for (String nameValuePair : nameValuePairs) 
		   {
			int pos = nameValuePair.indexOf("=");
			if(pos>0)
			{
				map.put(nameValuePair.substring(0,pos), nameValuePair.substring(pos+1));
			}
		   }  
		   return map;  
	}
	/**
	 * Converts a file path to Unix style file path
	 * NOTE: This webService doesn't prefix the path with / if it is not present  
	 * 
	 * @param path - Path to be converted into Unix style file path
	 * @return path - Converted file path
	 */
	public static String getUnixStyleFilePath(String path)
	{
			if (path == null){
		      path = "";
		    }else{
		      path = path.trim();
		      if (path.length() > 0){
			        path = path.replace('\\', '/');
			        path = path.replaceAll("/{2,}", "/");
			        if (path.endsWith("/"))
			          path = path.substring(0, path.length() - 1);
		      }
		    }
		    return path;
	  }
	public static boolean isEmptyOrNull(String str) 
	{
		if ((str == null) || str.matches("^\\s*$"))
			return true;
		else
			return false;
	}
}
