/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import org.kisst.cordys.caas.PackageDefinition.EPackageType;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class holds the information for a loaded package.
 */
public class Package extends LdapObjectBase
{
    /** Holds the roles that are part of this package. */
    public final ChildList<Role> roles = new ChildList<Role>(this, Role.class);
    /** Holds an alias for the roles. */
    public final ChildList<Role> role = roles;
    /** Holds an alias for the roles. */
    public final ChildList<Role> r = roles;
    /** Holds the web service interfaces. */
    public final ChildList<WebServiceInterface> webServiceInterfaces = new ChildList<WebServiceInterface>(this,
            WebServiceInterface.class);
    /** Holds the wsi. */
    public final ChildList<WebServiceInterface> wsi = webServiceInterfaces;
    /** Holds the filename. */
    public final StringProperty member = new StringProperty("member", 3);
    /** Holds the owner. */
    public final StringProperty owner = new StringProperty("owner", 3);
    /** Holds the definition. */
    private XmlNode definition = null;
    /** Holds the corresponding package definition step */
    private PackageDefinition pd = null;

    /**
     * Instantiates a new package.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected Package(LdapObject parent, String dn)
    {
        super(parent, dn);
        
        //From the system we need to find the definition object and set it here.
        CordysSystem sys = getSystem();
        pd = sys.packages.getByName(getCn());
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "package";
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#myclear()
     */
    @Override
    public void myclear()
    {
        super.myclear();
        definition = null;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#preDeleteHook()
     */
    @Override
    protected void preDeleteHook()
    {
        throw new RuntimeException("It is not allowed to delete an Isvp, please use unload instead");
    }

    /**
     * This method gets the Package DN for this package.
     * 
     * @return The Package DN for this package.
     */
    public String getPackageDN()
    {
        String retVal = member.get();

        if (retVal.startsWith("cn="))
        {
            retVal = retVal.substring(3);
        }

        return retVal;
    }

    /**
     * This method gets the basename.
     * 
     * @return The basename
     */
    public String getBasename()
    {
        String result = member.get();
        if (result.endsWith(".isvp"))
            result = result.substring(0, result.length() - 5);
        return result;
    }

    /**
     * This method gets the type of this package.
     * 
     * @return The type of this package.
     */
    public PackageDefinition getPackageDefinition()
    {
        return pd;
    }
    
    /**
     * Unload.
     * 
     * @param deleteReferences The delete references
     */
    public void unload(boolean deleteReferences)
    {
        if (getPackageDefinition().getType() == EPackageType.isvp)
        {
            for (Machine machine : getSystem().machines)
            {
                // TODO: check if machine has the ISVP loaded
                machine.unloadIsvp(this, deleteReferences);
            }
            getSystem().removeLdap(getDn());
            
            //Force a reload of the packages
            getSystem().packages.clear();
        }
    }

    /**
     * This method gets the definition.
     * 
     * @return The definition
     */
    public XmlNode getDefinition()
    {
        if (definition != null)
            return definition;
        XmlNode request = new XmlNode(Constants.GET_ISVP_DEFINITION, Constants.XMLNS_ISV);
        XmlNode file = request.add("file");
        file.setText(getBasename());
        file.setAttribute("type", "isvpackage");
        file.setAttribute("onlyxml", "true");
        definition = call(request).getChild("ISVPackage").detach();
        return definition;
    }

    /**
     * This method gets the description.
     * 
     * @return The description
     */
    public XmlNode getDescription()
    {
        return getDefinition().getChild("description");
    }

    /**
     * This method gets the content.
     * 
     * @return The content
     */
    public XmlNode getContent()
    {
        return getDefinition().getChild("content");
    }

    /**
     * This method gets the owner2.
     * 
     * @return The owner2
     */
    public String getOwner2()
    {
        return getDescription().getChildText("owner");
    }

    /**
     * This method gets the name2.
     * 
     * @return The name2
     */
    public String getName2()
    {
        return getDescription().getChildText("name");
    }

    /**
     * This method gets the version.
     * 
     * @return The version
     */
    public String getVersion()
    {
        return getDescription().getChildText("version");
    }

    /**
     * This method gets the wcpversion.
     * 
     * @return The wcpversion
     */
    public String getWcpversion()
    {
        return getDescription().getChildText("wcpversion");
    }

    /**
     * This method gets the eula.
     * 
     * @return The eula
     */
    public String getEula()
    {
        return getDescription().getChildText("eula");
    }

    /**
     * This method gets the sidebar.
     * 
     * @return The sidebar
     */
    public String getSidebar()
    {
        return getDescription().getChildText("sidebar");
    }

    /**
     * This method gets the buildnumber.
     * 
     * @return The buildnumber
     */
    public String getBuildnumber()
    {
        return getDescription().getChildText("build");
    }

    /**
     * Class that wraps the nodes on which this package was loaded.
     */
    public static class Node
    {
        public String name;
        public String id;
        public String status;
        public String version;
        public String buildNumber;
    }
}
