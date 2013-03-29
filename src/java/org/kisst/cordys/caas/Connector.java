/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysXmlObject;

/**
 * This class wraps the runtime definition of an application connector.
 */
public class Connector extends CordysXmlObject
{
    /**
     * Instantiates a new connector.
     * 
     * @param parent The parent
     * @param key The key
     */
    public Connector(CordysObject parent, String key)
    {
        super(parent, key);
    }

    /**
     * This method gets the name.
     * 
     * @return The name
     * @see org.kisst.cordys.caas.support.CordysXmlObject#getName()
     */
    @Override
    public String getName()
    {
        String retVal = getData().getChildText("description");
        if (retVal == null)
        {
            //For backwards compatibility
            retVal = getData().getChildText("step/description");
        }
        
        return retVal;
    }

    /**
     * This method gets the implementation.
     * 
     * @return The implementation
     */
    public String getImplementation()
    {
        String retVal = getData().getChildText("implementation");
        if (retVal == null)
        {
            //For backwards compatibility
            retVal = getData().getChildText("step/implementation");
        }
        
        return retVal;
    }

    /**
     * Prefix.
     * 
     * @return The string
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "conn";
    }

    /**
     * To string.
     * 
     * @return The string
     * @see org.kisst.cordys.caas.support.CordysXmlObject#toString()
     */
    @Override
    public String toString()
    {
        return getVarName();
    }

    /**
     * This method gets the var name.
     * 
     * @return The var name
     * @see org.kisst.cordys.caas.support.CordysXmlObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        String name = getName();
        if (name.indexOf(" ") >= 0 || name.indexOf('.') >= 0)
            return getSystem().getVarName() + ".conn.\"" + getName() + "\"";
        else
            return getSystem().getVarName() + ".conn." + getName();
    }
}
