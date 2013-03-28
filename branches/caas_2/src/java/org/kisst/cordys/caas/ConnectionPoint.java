/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.support.LdapObject;

/**
 * This class holds the configuration details for an connection point.
 */
public class ConnectionPoint extends LdapObjectBase
{
    /** Holds the uri of the connection point. This is the actual URI that is recognized by the middleware provider. */
    public final StringProperty uri = new StringProperty("labeleduri");
    /** Holds the private point. */
    public final BooleanProperty privatePoint = new BooleanProperty("busprivatepoint");
    /** Holds whether this belongs to a Client Connection Point Group. */
    public final BooleanProperty clientconnectionpoint = new BooleanProperty("clientconnectionpoint");

    /**
     * Instantiates a new connection point.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected ConnectionPoint(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "cp";
    }
}
