/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.support;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class holds a list of Ldap based Cordys objects.
 * 
 * @param <T> The Ldap based object.
 */
public class ChildList<T extends LdapObject> extends CordysObjectList<T>
{
    /** Holds the parent LDAP object. */
    private final LdapObject parent;
    /** Holds the prefix. */
    private final String prefix;
    /** Holds the type of objects . */
    private final Class<? extends LdapObject> clz;

    /**
     * Instantiates a new child list.
     * 
     * @param parent The parent
     * @param clz The clz
     */
    public ChildList(LdapObject parent, Class<T> clz)
    {
        this(parent, "", clz);
    }

    /**
     * Instantiates a new child list.
     * 
     * @param parent The parent
     * @param prefix The prefix
     * @param clz The clz
     */
    public ChildList(LdapObject parent, String prefix, Class<? extends LdapObject> clz)
    {
        super(parent.getSystem());
        this.parent = parent;
        this.prefix = prefix;
        this.clz = clz;
        // We have to delay the use of the dn, because the dn is not known in CordysSystem
        // at construction time
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return parent.getOrganization();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return parent.getKey() + ":" + clz.getSimpleName() + "s";
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObjectList#getName()
     */
    @Override
    public String getName()
    {
        String name = clz.getSimpleName();
        return name.substring(0, 1).toLowerCase() + name.substring(1) + "s";
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObjectList#getVarName()
     */
    @Override
    public String getVarName()
    {
        return parent.getVarName() + "." + getName();
    }
    
    /**
     * This method gets the parent object.
     * 
     * @return The parent object.
     */
    public LdapObject getParent()
    {
        return parent;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
     */
    @Override
    @SuppressWarnings("unchecked")
    protected void retrieveList()
    {
        XmlNode method = new XmlNode(Constants.GET_CHILDREN, Constants.XMLNS_LDAP);
        // method.add("dn").setText(prefix+((CordysLdapObject) parent).getDn());
        String dn;

        if (parent instanceof CordysSystem)
        {
            dn = ((CordysSystem) parent).getDn();
        }
        else
        {
            dn = parent.getDn();
        }
        method.add("dn").setText(prefix + dn);

        XmlNode response = system.call(method);

        if (response.getName().equals("Envelope"))
        {
            response = response.getChild("Body").getChildren().get(0);
        }

        for (XmlNode tuple : response.getChildren("tuple"))
        {
            XmlNode elm = tuple.getChild("old/entry");
            CordysObject obj = system.getLdap(elm);

            if (obj == null)
            {
                continue;
            }

            if ((clz == null) || (obj.getClass() == clz))
            {
                this.grow((T) obj);
            }
        }
    }
}
