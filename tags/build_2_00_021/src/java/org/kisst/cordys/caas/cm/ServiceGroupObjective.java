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
 * Holds the Class ServiceGroupObjective.
 */
public class ServiceGroupObjective extends ObjectiveBase
{
    
    /**
     * Instantiates a new service group objective.
     * 
     * @param org The org
     * @param node The node
     */
    public ServiceGroupObjective(Organization org, XmlNode node)
    {
        super(org, "wsi", node, true);
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#getVarName()
     */
    @Override
    public String getVarName()
    {
        return org.getVarName() + ".sn." + name;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#exists()
     */
    @Override
    boolean exists()
    {
        return org.sg.getByName(name) != null;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#getList()
     */
    @Override
    EntryObjectList<?> getList()
    {
        return org.sg.getByName(name).wsi;
    }

    /**
     * @see org.kisst.cordys.caas.cm.ObjectiveBase#create()
     */
    @Override
    public void create()
    {
        throw new RuntimeException("could not create Service Group " + name + " from a ccm file");
    }
}
