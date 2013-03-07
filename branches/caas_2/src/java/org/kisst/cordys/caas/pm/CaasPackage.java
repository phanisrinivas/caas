/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.pm;

import java.util.LinkedList;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This is the main class for the package manager. The package manager has a few main operations:
 * <ul>
 * <li>check - Checks if the configuration of the objectives that are in the pm file are currently in place</li>
 * <li>configure - Configures the given organization based on the current pm file.</li>
 * <li>purge - Removes the configuration as described in the pm file from the given organization.</li>
 * </ul>
 */
public class CaasPackage
{
    /** Holds the objectives that are currently configured. */
    private final LinkedList<Objective> objectives = new LinkedList<Objective>();
    /** Holds the organization that needs to be configured. */
    private final String orgName;

    /**
     * Instantiates a new caas package. It will load the given pmFile and parse the objectives from the definition.
     * 
     * @param pmfile The pmfile to parse.
     */
    public CaasPackage(String pmfile)
    {
        XmlNode pm = new XmlNode(FileUtil.loadString(pmfile));
        orgName = pm.getAttribute("org");

        for (XmlNode child : pm.getChildren())
        {
            if ("servicegroup".equals(child.getName()))
            {
                objectives.add(new ServiceGroupObjective(child));
            }
            else if ("user".equals(child.getName()))
            {
                objectives.add(new UserObjective(child));
            }
            else if ("package".equals(child.getName()))
            {
                objectives.add(new PackageObjective(child));
            }
            else if ("dso".equals(child.getName()))
            {
                objectives.add(new DsoObjective(child));
            }
            else
            {
                Environment.get().warn("Unknown objective: " + child.getName());
            }
        }
    }

    /**
     * This method returns the default organization name that is used.
     * 
     * @return The default organization name that is used.
     */
    public String getDefaultOrgName()
    {
        return orgName;
    }

    /**
     * This method checks if the given organization has the given configuration.
     * 
     * @param system The system to use.
     * @return true if the system is configured correctly. Otherwise false.
     */
    public boolean check(CordysSystem system)
    {
        return check(system.org.getByName(orgName));
    }

    /**
     * This method configures the given organization based on the information in the pm file.
     * 
     * @param system The system to configure.
     */
    public void configure(CordysSystem system)
    {
        configure(system.org.getByName(orgName));
    }

    /**
     * This method removes the configuration from the given system.
     * 
     * @param system The system to configure.
     */
    public void purge(CordysSystem system)
    {
        purge(system.org.getByName(orgName));
    }

    /**
     * This method checks if the given organization has the given configuration.
     * 
     * @param org The organization to check.
     * @return true if the system is configured correctly. Otherwise false.
     */
    public boolean check(Organization org)
    {
        boolean result = true;

        for (Objective o : objectives)
        {
            result = o.check(org) && result;
        }
        
        return result;
    }

    /**
     * This method configures the given organization based on the information in the pm file.
     * 
     * @param org The organization to configure.
     */
    public void configure(Organization org)
    {
        for (Objective o : objectives)
        {
            o.configure(org);
        }
    }

    /**
     * This method removes the configuration from the given organization.
     * 
     * @param org he organization to configure.
     */
    public void purge(Organization org)
    {
        for (Objective o : objectives)
        {
            o.remove(org);
        }
    }
}
