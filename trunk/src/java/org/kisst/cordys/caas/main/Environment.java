/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.main;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;

public class Environment
{
    /** Holds the logger to use. */
    private static final Logger m_log;
    /** Holds the singleton environment variable */
    private final static Environment singleton;

    static
    {
        // Initialize the Log4J logging system.
        Layout layout = new PatternLayout("%-5p [%c]: %m%n");
        ConsoleAppender ca = new ConsoleAppender(layout);

        if (!Logger.getRootLogger().getAllAppenders().hasMoreElements())
        {
            BasicConfigurator.configure(ca);
        }

        Logger.getRootLogger().setLevel(Level.ERROR);
        
        m_log = Logger.getLogger("caas");

        m_log.setLevel(Level.ALL);

        singleton = new Environment();
    }

    /** Holds whether or not to display the debug messages */
    public static boolean debug = false;
    /** Holds whether or not to display the trace messages */
    public static boolean trace = false;
    /** Holds whether or not to hold all the messages */
    public static boolean quiet = false;
    /** Holds whether or not to display the verbose messages*/
    public static boolean verbose = false;
    /** Holds the properties loaded for this environment. */
    private Properties props = new Properties();
    /** Holds the location from where the caas.conf file is loaded */
    private File m_caasConf;
    /** Holds the folder in which the caas.conf is located */
    private File m_caasConfFolder;

    /**
     * Instantiates a new environment. It will also initialize the logging system.
     */
    private Environment()
    {
        initEnvironment();
    }

    /**
     * This method returns the singleton instance of the environemnt.
     * 
     * @return The singleton instance of the environemnt.
     */
    public static Environment get()
    {
        return singleton;
    }

    /**
     * This method gets the location of the caas.conf file that is used.
     * 
     * @return The location of the caas.conf file that is used.
     */
    public File getCaasConfFile()
    {
        return m_caasConf;
    }

    /**
     * This method gets the folder in which the caas.conf is located that is currently loaded.
     * 
     * @return The folder in which the caas.conf is located that is currently loaded.
     */
    public File getCaasConfFolder()
    {
        return m_caasConfFolder;
    }
    
    /**
     * Trace.
     * 
     * @param msg The msg
     */
    public static void trace(String msg)
    {
        if (trace && !quiet)
            m_log.trace(msg);
    }

    /**
     * Debug.
     * 
     * @param msg The msg
     */
    public static void debug(String msg)
    {
        if (debug && !quiet)
            m_log.debug(msg);
    }

    /**
     * Info.
     * 
     * @param msg The msg
     */
    public static void info(String msg)
    {
        if (verbose && !quiet)
            m_log.info(msg);
    }

    /**
     * Warn.
     * 
     * @param msg The msg
     */
    public static void warn(String msg)
    {
        if (!quiet)
            m_log.warn(msg);
    }

    /**
     * Error.
     * 
     * @param msg The msg
     */
    public static void error(String msg, Throwable t)
    {
        m_log.error(msg, t);
    }

    /**
     * Error.
     * 
     * @param msg The msg
     */
    public static void error(String msg)
    {
        error(msg, null);
    }

    /**
     * This method gets the property with the given name.
     * 
     * @param key The key
     * @param defaultValue The default value
     * @return The value of the property.
     */
    public String getProp(String key, String defaultValue)
    {
        return props.getProperty(key, defaultValue);
    }

    /**
     * This method gets the properties for the environment.
     * 
     * @return The properties for the environment.
     */
    public Properties getProperties()
    {
        return props;
    }

    /**
     * <p>
     * This method looks up for the properties file and loads it after finding it. It first looks up at the location mentioned in
     * 'system.<<systemName>>.properties.file' property in caas.conf If not then looks up for the '<<systemName>>.properties' file
     * in the current directory If not then look up for the '<<systemName>>.properties' in logged in user's home directory
     * </p>
     * There is also a layering option here. The file <systemname>.properties MUST be present. But in that folder you can specify
     * the following files to customize the properties in the default system file. The properties (once defined) will not be
     * overwritten. The sequence of the files is:
     * <ul>
     * <li><systemname>-<orgname>-user.properties</li>
     * <li><systemname>-<orgname>.properties</li>
     * <li><systemname>-user.properties</li>
     * <li><systemname>.properties - This file MUST always be present!</li>
     * </ul>
     * 
     * @param systemName - Cordys system name as mentioned in the caas.conf file
     * @return map - A Map object containing all the properties of the given system
     */
    public Map<String, String> loadSystemProperties(String systemName, String org)
    {
        if (StringUtil.isEmptyOrNull(systemName))
        {
            throw new CaasRuntimeException("Unable to load the properties as the Cordys system name is null");
        }

        // Either the properties file defined in the caas.conf OR the file in the current folder OR the file in the user's home
        // folder MUST be present.
        boolean anyExist = false;

        // This list will hold all the files that should be checked.
        List<File> files = new ArrayList<File>();

        // File name of the properties file mentioned in caas.conf file - Highest Precedence
        String propsFileInConf = Environment.get().getProp("system." + systemName + ".properties.file", null);
        if (!StringUtil.isEmptyOrNull(propsFileInConf))
        {
            // A property file is defined in the caas.conf. So let's add the property file locations to the file list
            File propsFileInConfFile = new File(StringUtil.getUnixStyleFilePath(propsFileInConf));
            File folder = propsFileInConfFile.getParentFile();

            if (!StringUtil.isEmptyOrNull(org))
            {
                files.add(new File(folder, systemName + "-" + org + "-user.properties"));
                files.add(new File(folder, systemName + "-" + org + ".properties"));
            }
            files.add(new File(folder, systemName + "-user.properties"));
            files.add(propsFileInConfFile);
            if (propsFileInConfFile.exists())
            {
                anyExist = true;
            }
        }

        // File name of the properties file in current directory - Second Highest Precedence
        if (!StringUtil.isEmptyOrNull(org))
        {
            files.add(new File(systemName + "-" + org + "-user.properties"));
            files.add(new File(systemName + "-" + org + ".properties"));
        }
        files.add(new File(systemName + "-user.properties"));
        File propsInPWD = new File(systemName + ".properties");
        files.add(propsInPWD);
        if (propsInPWD.exists())
        {
            anyExist = true;
        }

        // File name of the properties file in user's home directory - Lowest Precedence
        File folder = new File(System.getProperty("user.home") + "/config/caas");

        if (!StringUtil.isEmptyOrNull(org))
        {
            files.add(new File(folder, systemName + "-" + org + "-user.properties"));
            files.add(new File(folder, systemName + "-" + org + ".properties"));
        }
        files.add(new File(folder, systemName + "-user.properties"));
        File propsInHome = new File(folder, systemName + ".properties");
        files.add(propsInHome);
        if (propsInHome.exists())
        {
            anyExist = true;
        }

        if (!anyExist)
        {
            warn("No base property file is found for system " + systemName
                    + " in either the caas.conf, current working folder or the users HOME folder");
        }

        // All the possible file locations have been collected. So now we can start to load the properties.
        return FileUtil.loadProperties(files);
    }

    /**
     * <p>
     * This method loads the properties from the given location.
     * </p>
     * <p>
     * To avoid having to commit username and passwords to subversion CAAS supports another file called caas-user.conf file in the
     * same folder as where the caas.conf is read from. Properties in that file are loaded first and can overwrite properties that
     * are in the generic caas.conf file.
     * </p>
     * 
     * @param filename The name of the caas.conf file that should be loaded.
     */
    public Properties loadProperties(String filename)
    {
        if (!FileUtil.doesFileExist(filename))
        {
            throw new CaasRuntimeException("File " + filename + " does not exist");
        }

        props.clear();

        m_caasConf = new File(filename);
        m_caasConfFolder = m_caasConf.getParentFile();

        // Add the files in the sequence that they should be loaded.
        List<File> files = new ArrayList<File>();

        // Add the caas-user.conf
        files.add(new File(m_caasConfFolder, "caas-user.conf"));
        files.add(m_caasConf);

        // Check to see if the caas-user.conf file is present.
        FileUtil.load(props, files);

        return props;
    }

    /**
     * This method initailizes the environment. It will read the caas.conf file to use. The location where it reads the caas.conf
     * in order of it's priority:
     * <ul>
     * <li>The location specified in via the -Dcaas.conf.location=c:/temp/caas.conf</li>
     * <li>caas.conf in the current working directory</li>
     * <li>caas.conf in the user.home/config/caas folder</li>
     * </ul>
     * To avoid having to commit username and passwords to subversion CAAS supports another file called caas-user.conf file in the
     * same folder as where the caas.conf is read from. Properties in that file are loaded first and can overwrite properties that
     * are in the generic caas.conf file.
     */
    private void initEnvironment()
    {
        List<String> fileLocations = new ArrayList<String>();

        // First check if the -D was set
        String filename = System.getProperty(Constants.CAAS_CONF_LOCATION, "");
        if (!StringUtil.isEmptyOrNull(filename))
        {
            fileLocations.add(filename);
        }

        // Add the caas.conf in the current working folder
        fileLocations.add("caas.conf");

        // Add the caas.conf in the
        String fileName = System.getProperty("user.home") + "/config/caas/caas.conf";
        fileLocations.add(fileName);

        // Determine caas.file that need to be considered for loading
        // To do so, Loop over the files as per their precedence and check for their existence
        for (String aFileName : fileLocations)
        {
            debug("Checking if file " + aFileName + " exists");
            if (FileUtil.doesFileExist(aFileName))
            {
                info("Using caas.conf from location " + aFileName);
                fileName = aFileName;
                break;
            }
        }
        // Load the caas.conf file
        if (fileName != null)
        {
            m_caasConf = new File(filename);
            loadProperties(fileName);
        }
        else
        {
            throw new CaasRuntimeException("caas.conf file not present in any of the considered locations: " + fileLocations);
        }
    }
}
