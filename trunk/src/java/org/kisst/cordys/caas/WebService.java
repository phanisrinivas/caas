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
 * This class wraps the details of a web service operation.
 */
public class WebService extends LdapObjectBase
{
    /** Holds the implementation. */
    public final XmlProperty implementation = new XmlProperty("busmethodimplementation");
    /** Holds the impl. */
    public final XmlProperty impl = implementation;
    /** Holds the signature. */
    public final XmlProperty signature = new XmlProperty("busmethodsignature");
    /** Holds the sig. */
    public final XmlProperty sig = signature;
    /** Holds the wsdl. */
    public final XmlProperty wsdl = new XmlProperty("busmethodwsdl");
    /** Holds the iface. */
    public final XmlProperty iface = new XmlProperty("busmethodinterface");

    /**
     * Instantiates a new web service.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected WebService(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "webservice";
    }
}
