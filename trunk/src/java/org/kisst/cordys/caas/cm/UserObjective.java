/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.cm;

import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.support.EntryObjectList;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class UserObjective.
 */
public class UserObjective extends ObjectiveBase
{
    /** Holds the name of the authenticated user. */
    private final String auName;
    /** Holds the type of the authenticated user. */
    private final String type;
    /** Holds the osidentity for the user */
    private final String osidentity;
    /** Holds the password for the user */
    private final String password;

    /**
     * Instantiates a new user objective.
     * 
     * @param org The org
     * @param node The node
     */
    public UserObjective(Organization org, XmlNode node)
    {
        super(org, "roles", node, false);
        this.auName = node.getAttribute("au", name);
        this.type = node.getAttribute("type", null);
        this.osidentity = node.getAttribute("osidentity", null);
        this.password = node.getAttribute("password", null);
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#exists()
     */
    @Override
    boolean exists()
    {
        return org.users.getByName(name) != null;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#getList()
     */
    @Override
    EntryObjectList<?> getList()
    {
        return org.users.getByName(name).roles;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#getVarName()
     */
    @Override
    public String getVarName()
    {
        return org.getVarName() + ".user." + name;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#create()
     */
    @Override
    public void create()
    {
        if (exists())
        {
            throw new RuntimeException("User allready exists");
            // User user = org.users.getByName(name);
            // if (! auName.equals(user.authenticatedUser.get()))
            // user.au.set(auName);
        }
        org.createUser(name, auName, type, osidentity, password);
    }
}
