/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import java.io.IOException;
import java.io.InputStream;

import java.net.ConnectException;

import java.util.LinkedHashMap;
import java.util.Properties;

import org.kisst.cordys.caas.cm.Template;
import static org.kisst.cordys.caas.main.Environment.*;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.soap.DummyCaller;
import org.kisst.cordys.caas.soap.HttpClientCaller;
import org.kisst.cordys.caas.soap.NativeCaller;
import org.kisst.cordys.caas.soap.SamlClientCaller;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * DOCUMENTME.
 * 
 * @author $author$
 */
public class Caas
{
    /** DOCUMENTME. */
    private static LinkedHashMap<String, CordysSystem> systemCache = new LinkedHashMap<String, CordysSystem>();
    /** Holds the default system that is used. */
    public static String defaultSystem = null;

    /**
     * This method creates a new Configuration object based on the given filename.
     * 
     * @param filename The name of the file to load. The content must be a caas.conf like file.
     * @return The configuration object wrapping the given file.
     */
    public static Configuration config(String filename)
    {
        return new Configuration(filename);
    }

    /**
     * Creates a template from the file.
     * 
     * @param filename Template file name
     * @return Template object
     */
    public static Template template(String filename)
    {
        return new Template(FileUtil.loadString(filename));
    }

    /**
     * DOCUMENTME.
     * 
     * @param filename
     * @return
     */
    public static CordysSystem connect(String filename)
    {
        String name = filename.substring(0, filename.indexOf("."));
        int pos = name.lastIndexOf("/");

        if (pos >= 0)
        {
            name = name.substring(pos + 1);
        }
        return connect(filename, name);
    }

    /**
     * DOCUMENTME.
     * 
     * @param filename
     * @param name
     * @return
     */
    public static CordysSystem connect(String filename, String name)
    {
        try
        {
            Environment.info("Connecting to system " + name + " (" + filename + ") ... ");

            HttpClientCaller caller = new HttpClientCaller(filename);
            CordysSystem result = new CordysSystem(name, caller);
            Environment.info("OK");
            return result;
        }
        catch (Exception e)
        {
            // Catch any exceptions so it won't be a problem if anything fails in the Startup script
            if (!(e.getCause() instanceof ConnectException))
            {
                e.printStackTrace();
            }
            Environment.error("Failed to connect to system " + name, e);
            
            return null;
        }
    }

    /**
     * DOCUMENTME.
     * 
     * @param filename
     * @return
     */
    public static CordysSystem loadFromDump(String filename)
    {
        String name = filename.substring(0, filename.indexOf("."));
        int pos = name.lastIndexOf("/");

        if (pos >= 0)
        {
            name = name.substring(pos + 1);
        }
        return loadFromDump(filename, name);
    }

    /**
     * DOCUMENTME.
     * 
     * @param filename
     * @param name
     * @return
     */
    public static CordysSystem loadFromDump(String filename, String name)
    {
        XmlNode xml = new XmlNode(FileUtil.loadString(filename));
        DummyCaller caller = new DummyCaller(xml);

        if (name == null)
        {
            name = caller.getName();
        }
        return new CordysSystem(name, caller);
    }

    /**
     * DOCUMENTME.
     * 
     * @return Caas version
     */
    public static String getVersion()
    {
        InputStream in = Caas.class.getResourceAsStream("/version.properties");

        if (in == null)
        {
            return "unknown-version";
        }

        Properties props = new Properties();

        try
        {
            props.load(in);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return props.getProperty("base.version") + "." + props.getProperty("build.number");
    }

    /**
     * Constructs a Cordys system and caches it. Depending up on the authentication mechanism mentioned in the caas.conf file it
     * connects to the Cordys server and returns an instance of CordysSystem It also loads the properties file of the system if
     * configured
     * 
     * @param name
     * @return
     */
    public static CordysSystem getSystem(String name)
    {
        if (StringUtil.isEmptyOrNull(name))
        {
            return getDefaultSystem();
        }

        CordysSystem result = systemCache.get(name);

        if (result != null)
        {
            return result;
        }

        String classname = get().getProp("system." + name + ".gateway.class", null);

        try
        {
            info("Connecting to system " + name + " ... ");

            SoapCaller caller;

            if ((classname == null) || classname.equals("HttpClientCaller"))
            {
                caller = new HttpClientCaller(name);
            }
            else if (classname.equals("NativeCaller"))
            {
                caller = new NativeCaller(name);
            }
            else if (classname.equals("SamlClientCaller"))
            {
                caller = new SamlClientCaller(name);
            }
            else
            {
                throw new RuntimeException("Unknown SoapCaller class " + classname);
            }
            result = new CordysSystem(name, caller);

            info("Connected to system " + name);

            if (result.getPropsFile() != null)
            {
                info("Using " + result.getPropsFile() + " as property file");
            }
            // Put it in cache
            systemCache.put(name, result);
            return result;
        }
        catch (Exception e)
        {
            error("Failed: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * DOCUMENTME.
     * 
     * @return
     */
    public static CordysSystem getDefaultSystem()
    {
        if (defaultSystem == null)
        {
            defaultSystem = get().getProp("caas.defaultSystem", "default");
        }
        return getSystem(defaultSystem);
    }
    
    /**
     * This method enables trace logging for CAAS.
     */
    public static void enableTrace()
    {
        Environment.trace = true;
    }

    /**
     * This method disables trace logging for CAAS.
     */
    public static void disableTrace()
    {
        Environment.trace = false;
    }

    /**
     * This method enables debug logging for CAAS.
     */
    public static void enableDebug()
    {
        Environment.debug = true;
    }

    /**
     * This method disables debug logging for CAAS.
     */
    public static void disableDebug()
    {
        Environment.debug = false;
    }

    /**
     * This method enables verbose logging for CAAS.
     */
    public static void enableVerbose()
    {
        Environment.verbose = true;
    }

    /**
     * This method disables verbose logging for CAAS.
     */
    public static void disableVerbose()
    {
        Environment.verbose = false;
    }

    /**
     * This method enables quiet logging for CAAS.
     */
    public static void enableQuiet()
    {
        Environment.quiet = true;
    }

    /**
     * This method disables quiet logging for CAAS.
     */
    public static void disableQuiet()
    {
        Environment.quiet = false;
    }
}
