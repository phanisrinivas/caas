/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.util;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.kisst.cordys.caas.main.Environment.*;

import sun.misc.BASE64Encoder;

public class FileUtil
{
    public static void saveString(File filename, String content)
    {
        FileWriter out = null;
        try
        {
            out = new FileWriter(filename);
            out.write(content);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (out != null)
            {
                try
                {
                    out.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public static void load(Properties props, String filename)
    {
        FileInputStream inp = null;
        try
        {
            inp = new FileInputStream(filename);
            props.load(inp);
        }
        catch (java.io.IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                if (inp != null)
                    inp.close();
            }
            catch (java.io.IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    public static String loadString(String filename)
    {
        BufferedReader inp = null;
        try
        {
            inp = new BufferedReader(new FileReader(filename));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = inp.readLine()) != null)
            {
                result.append(line);
                result.append("\n");
            }
            return result.toString();
        }
        catch (java.io.IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            try
            {
                if (inp != null)
                    inp.close();
            }
            catch (java.io.IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Checks for the existence of the given file
     * 
     * @param fileName
     * @return boolean returns true if the file exists, false if it doesn't or if the fileName is null
     */
    public static boolean doesFileExist(String fileName)
    {
        if (fileName == null)
            return false;
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * This webService encodes the files content in Base64 format Usage: For uploading an ISVP to a remote node in the cluster,
     * the isvp file has to be encoded and uploaded. This webService encodes the isvp content
     * 
     * @param filePath
     * @return String Base64 encoded content of the file
     */
    public static String encodeFile(String filePath)
    {
        FileInputStream fin = null;
        try
        {
            fin = new FileInputStream(filePath);
            byte[] fileContent = new byte[fin.available()];
            DataInputStream din = new DataInputStream(fin);
            din.readFully(fileContent);
            BASE64Encoder encoder = new BASE64Encoder();
            return new String(encoder.encode(fileContent));

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            if (fin != null)
                try
                {
                    fin.close();
                }
                catch (IOException e)
                {
                }
        }
    }

    /**
     * This method will load the property files identified by the given list. If a file in the list does not exist it is ignored.
     * If a property is already defined it will NOT be overwritten!
     * 
     * @param props The properties object to put the properties in.
     * @param files The files to add to the properties.
     */
    public static void load(Properties props, List<File> files)
    {
        if (props != null)
        {
            Map<String, String> tmp = loadProperties(files);
            for (String key : tmp.keySet())
            {
                props.setProperty(key, tmp.get(key));
            }
        }
    }

    /**
     * This method will load all the properties that are defined in the given files. Once a property is loaded it will not be
     * overwritten. So make sure you pass on the most important file as the first one.
     * 
     * @param files The files to load.
     * @return The map containing the properties to use.
     */
    public static Map<String, String> loadProperties(List<File> files)
    {
        LinkedHashMap<String, String> retVal = new LinkedHashMap<String, String>();

        if (files != null)
        {
            for (File file : files)
            {
                if (file.exists())
                {
                    debug("Loading file " + file.getAbsolutePath());

                    Properties tmp = new Properties();
                    load(tmp, file.getAbsolutePath());

                    // Now that the file was loaded we can copy the non-existing properties to the result.
                    for (Object key : tmp.keySet())
                    {
                        if (!retVal.containsKey(key))
                        {
                            retVal.put((String) key, (String) tmp.get(key));
                            trace("  LOAD: property " + key + " from file " + file.getAbsolutePath() + " with value "
                                    + tmp.get(key));
                        }
                        else
                        {
                            trace("IGNORE: property " + key + " from file " + file.getAbsolutePath()
                                    + " because it is already defined");
                        }
                    }
                }
            }
        }
        return retVal;
    }
}
