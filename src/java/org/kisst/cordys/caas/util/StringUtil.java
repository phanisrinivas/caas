package org.kisst.cordys.caas.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * DOCUMENTME.
 * 
 * @author $author$
 */
public class StringUtil
{
    /**
     * DOCUMENTME.
     * 
     * @param name DOCUMENTME
     * @return DOCUMENTME
     */
    public static String quotedName(String name)
    {
        if ((name.indexOf(' ') >= 0) || (name.indexOf('.') >= 0) || (name.indexOf('-') >= 0))
        {
            return '"' + name + '"';
        }
        else
        {
            return name;
        }
    }

    /**
     * Substitutes the Map values found in the given string with their corresponding keys from the Map.
     * 
     * @param str
     * @param vars
     * @return
     */
    public static String reverseSubstitute(String str, Map<String, String> vars)
    {
        StringBuffer buff = new StringBuffer();

        for (Entry<String, String> entry : vars.entrySet())
        {
            String value = entry.getValue();
            String key = getKeyByValue(vars, value);
            int prevpos = 0;
            int pos = 0;
            buff.setLength(0);

            while ((pos = str.indexOf(value, prevpos)) >= 0)
            {
                buff.append(str.substring(prevpos, pos));
                buff.append("${" + key + "}");
                prevpos = pos + value.length();
            }
            buff.append(str.substring(prevpos));
            str = buff.toString();
        }

        if (vars == null || vars.size() == 0)
        {
            return str;
        }
        else
        {
            return buff.toString();
        }
    }

    /**
     * Returns a key matching the given value from the givem Map.
     * 
     * @param map
     * @param value
     * @return
     */
    public static String getKeyByValue(Map<String, String> map, String value)
    {
        for (Entry<String, String> entry : map.entrySet())
        {
            if (value.equals(entry.getValue()))
            {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * Removes XML comments from the given string.
     * 
     * @param xmlString
     * @return
     */
    public static String removeXmlComments(String xmlString)
    {
        return xmlString.replaceAll("(?s)<!--.*?-->", "");
    }

    /**
     * DOCUMENTME.
     * 
     * @param iterator DOCUMENTME
     * @param separator DOCUMENTME
     * @return DOCUMENTME
     */
    public static String join(Iterator<?> iterator, String separator)
    {
        // handle null, zero and one elements before building a buffer
        if (iterator == null)
        {
            return null;
        }

        if (!iterator.hasNext())
        {
            return "";
        }

        Object first = iterator.next();

        if (!iterator.hasNext())
        {
            return null;
        }

        // two or more elements
        StringBuffer buf = new StringBuffer(256); // Java default is 16, probably too small

        if (first != null)
        {
            buf.append(first);
        }

        while (iterator.hasNext())
        {
            if (separator != null)
            {
                buf.append(separator);
            }

            Object obj = iterator.next();

            if (obj != null)
            {
                buf.append(obj);
            }
        }
        return buf.toString();
    }

    /**
     * Substitutes the Map keys found in the given string with their corresponding values from the Map.
     * 
     * @param str
     * @param vars
     * @return
     */
    public static String substitute(String str, Map<String, String> vars)
    {
        StringBuilder result = new StringBuilder();

        if (str == null)
        {
            str = "";
        }

        try
        {
            int prevpos = 0;
            int pos = str.indexOf("${");

            while (pos >= 0)
            {
                int pos2 = str.indexOf("}", pos);

                if (pos < 0)
                {
                    throw new RuntimeException("Unbounded ${");
                }

                String key = str.substring(pos + 2, pos2);
                result.append(str.substring(prevpos, pos));

                String value = vars.get(key);

                if ((value == null) && key.equals("dollar"))
                {
                    value = "$";
                }

                // This will be replaced later in resolveVariables() webService of Template.java, as we don't know its
                // value at this point of time
                /*
                 * if(value==null && key.equals("CORDYS_INSTALL_DIR")) value="CORDYS_INSTALL_DIR";
                 */
                if (value == null)
                {
                    throw new RuntimeException("Unknown variable ${" + key + "}");
                }
                result.append(value);
                prevpos = pos2 + 1;
                pos = str.indexOf("${", prevpos);
            }
            result.append(str.substring(prevpos));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return result.toString();
    }

    /**
     * Generates a random UUID used as a RequestID for SAML request.
     * 
     * @return
     */
    public static String generateUUID()
    {
        UUID uuid = UUID.randomUUID();
        String strUUID = "a" + uuid.toString(); // XML validation requires that the request ID does not start with a
                                                // number
        return strUUID;
    }

    /**
     * Converts a Map to Query String. This webService forms a query string from the map
     * 
     * @param map A Map object containing <key, value> which need to be converted to query string
     * @return String Query string formed from the map
     */
    public static String mapToString(Map<String, String> map)
    {
        StringBuilder stringBuilder = new StringBuilder();

        for (String key : map.keySet())
        {
            if (stringBuilder.length() > 0)
            {
                stringBuilder.append("&");
            }

            String value = map.get(key);
            stringBuilder.append(((key != null) ? key : ""));

            if (value != null)
            {
                stringBuilder.append("=");
                stringBuilder.append((value != null) ? value : "");
            }
        }
        return stringBuilder.toString();
    }

    /**
     * Converts a Query String to Map.
     * 
     * @param input Input query string which need to be converted to a map object
     * @param map DOCUMENTME
     * @return map HashMap object loaded with <key,value> pairs
     */
    public static HashMap<String, String> stringToMap(String input, HashMap<String, String> map)
    {
        String[] nameValuePairs = input.split("&");

        for (String nameValuePair : nameValuePairs)
        {
            int pos = nameValuePair.indexOf("=");

            if (pos > 0)
            {
                map.put(nameValuePair.substring(0, pos), nameValuePair.substring(pos + 1));
            }
        }
        return map;
    }

    /**
     * Converts a file path to Unix style file path NOTE: This webService doesn't prefix the path with / if it is not present
     * 
     * @param path - Path to be converted into Unix style file path
     * @return path - Converted file path
     */
    public static String getUnixStyleFilePath(String path)
    {
        if (path == null)
        {
            path = "";
        }
        else
        {
            path = path.trim();

            if (path.length() > 0)
            {
                path = path.replace('\\', '/');
                path = path.replaceAll("/{2,}", "/");

                if (path.endsWith("/"))
                {
                    path = path.substring(0, path.length() - 1);
                }
            }
        }
        return path;
    }

    /**
     * Checks if a stirng is empty or null.
     * 
     * @param str
     * @return true if the string is empty or null false otherwise
     */
    public static boolean isEmptyOrNull(String str)
    {
        if ((str == null) || str.matches("^\\s*$"))
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * <p>
     * This method compares the 2 strings to eachother. If they are equal it returns true. It can handle null values for both
     * source and target.
     * </p>
     * <p>
     * This method will return true if:
     * </p>
     * <ul>
     * <li>source == null and target == null</li>
     * <li>source.equals(target)</li>
     * </ul>
     * 
     * @param source The source string to compare.
     * @param target The target string to compare.
     * @return true if source and target are equal.
     */
    public static boolean equals(String source, String target)
    {
        boolean retVal = false;

        if ((source == null) && (target == null))
        {
            retVal = true;
        }
        else if ((source != null) && source.equals(target))
        {
            retVal = true;
        }

        return retVal;
    }

    /**
     * This method gets the cn from a LDAP DN.
     * 
     * @param dn The dn
     * @return The cn
     */
    public static String getCN(String dn)
    {
        int pos = dn.indexOf("=");
        int pos2 = dn.indexOf(",", pos);

        return dn.substring(pos + 1, pos2);
    }
}
