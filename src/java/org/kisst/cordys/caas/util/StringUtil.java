package org.kisst.cordys.caas.util;

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
			//This will be replaced later in Template.java, as we don't know its value at this point of time
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
	
	//This method is used for generating RequestID for SAML request
	public static String generateUUID(){
		UUID uuid = UUID.randomUUID();
		String strUUID = "a"+uuid.toString(); // XML validation requires that the request ID does not start with a number
		return strUUID;
	}
}
