/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.pm;

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
     * @param node The node
     */
    public ServiceGroupObjective(XmlNode node)
    {
        super("wsi", new WebServiceInterfaces(node), node);
    }

    /**
     * Holds the Class WebServiceInterfaces.
     */
    public static class WebServiceInterfaces extends Target
    {

        /**
         * Instantiates a new web service interfaces.
         * 
         * @param node The node
         */
        WebServiceInterfaces(XmlNode node)
        {
            super(node);
        }

        /**
         * @see org.kisst.cordys.caas.pm.Target#getVarName(org.kisst.cordys.caas.Organization)
         */
        @Override
        public String getVarName(Organization org)
        {
            return org.getVarName() + ".sg." + name;
        }

        /**
         * @see org.kisst.cordys.caas.pm.Target#exists(org.kisst.cordys.caas.Organization)
         */
        @Override
        boolean exists(Organization org)
        {
            return org.serviceGroups.getByName(name) != null;
        }

        /**
         * @see org.kisst.cordys.caas.pm.Target#getList(org.kisst.cordys.caas.Organization)
         */
        @Override
        EntryObjectList<?> getList(Organization org)
        {
            return org.serviceGroups.getByName(name).webServiceInterfaces;
        }
    }
}
