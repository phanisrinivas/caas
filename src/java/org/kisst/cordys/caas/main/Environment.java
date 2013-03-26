/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;

public class Environment
{
    private final static Environment singleton = new Environment();

    public static Environment get()
    {
        return singleton;
    }

    private Environment()
    {
        initEnvironment();
    }

    public boolean debug = false;
    public boolean quiet = false;
    public boolean verbose = false;
    private Properties props = new Properties();

    private void log(String type, String msg)
    {
        System.out.println(type + " " + msg);
    }

    public void debug(String msg)
    {
        if (debug && !quiet)
            log("DEBUG", msg);
    }

    public void info(String msg)
    {
        if (verbose && !quiet)
            log("INFO ", msg);
    }

    public void warn(String msg)
    {
        if (!quiet)
            log("WARN ", msg);
    }

    public void error(String msg)
    {
        log("ERROR", msg);
    }

    public String getProp(String key, String defaultValue)
    {
        return props.getProperty(key, defaultValue);
    }

    public void loadProperties(String filename)
    {
        props.clear();
        FileUtil.load(props, filename);
    }
    
    /**
     * This method initailizes the environment. It will read the caas.conf file to use. The location where it reads the caas.conf
     * in order of it's priority:
     * <ul>
     * <li>The location specified in via the -Dcaas.conf.location=c:/temp/caas.conf</li>
     * <li>caas.conf in the current working directory</li>
     * <li>caas.conf in the user.home/config/caas folder</li>
     * </ul>
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
            System.out.println("checking " + aFileName);
            if (FileUtil.doesFileExist(aFileName))
            {
                System.out.println("using " + aFileName);
                fileName = aFileName;
                break;
            }
        }
        // Load the caas.conf file
        if (fileName != null)
        {
            loadProperties(fileName);
        }
        else
        {
            throw new CaasRuntimeException("caas.conf file not present in any of the considered locations: " + fileLocations);
        }
    }
}
