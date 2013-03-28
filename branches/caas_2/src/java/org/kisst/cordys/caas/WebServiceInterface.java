/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;

/**
 * This class wraps the web service interface definition in LDAP.
 */
public class WebServiceInterface extends LdapObjectBase
{
    /** Holds the web services. */
    public final ChildList<WebService> webServices = new ChildList<WebService>(this, WebService.class);
    /** Holds the web service. */
    public final ChildList<WebService> webService = webServices;
    /** Holds the ws. */
    public final ChildList<WebService> ws = webServices;
    /** Holds the xsds. */
    public final ChildList<Xsd> xsds = new ChildList<Xsd>(this, Xsd.class);
    /** Holds the xsd. */
    public final ChildList<Xsd> xsd = xsds;
    /** Holds the namespaces. */
    public final StringList namespaces = new StringList("labeleduri");
    /** Holds the ns. */
    public final StringList ns = namespaces;
    /** Holds the implementationclass. */
    public final StringProperty implementationclass = new StringProperty("implementationclass");

    /**
     * Instantiates a new web service interface.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected WebServiceInterface(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "wsi";
    }

    /**
     * This method creates the web service.
     * 
     * @param name The name
     */
    public void createWebService(String name)
    {
        createInLdap(newEntryXml("", name, "busmethod"));
        webServices.clear();
    }
}
