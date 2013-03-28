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
import org.kisst.cordys.caas.util.StringUtil;
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
    /** Holds the corresponding package definition step */
    private PackageDefinition pd = null;
    /** Holds the deployed package information for this package */
    private IDeployedPackageInfo m_info;

    /**
     * Instantiates a new package.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected Package(LdapObject parent, String dn)
    {
        super(parent, dn);

        // From the system we need to find the definition object and set it here.
        CordysSystem sys = getSystem();

        // There are some old packages in the Cordys LDAP for which there are no CAP packages. So there will be not package
        // definition.
        pd = sys.packageDefinitions.getByName(getCn());
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
        m_info = null;
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
        {
            result = result.substring(0, result.length() - 5);
        }

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

            // Force a reload of the packages
            getSystem().packages.clear();
        }
    }

    /**
     * This method returns the deployed version information.
     * 
     * @return The deployed version information.
     */
    public IDeployedPackageInfo getInfo()
    {
        if (m_info != null)
        {
            return m_info;
        }

        // It's not loaded yet, so lets get the details
        if (pd != null)
        {
            switch (pd.getType())
            {
                case isvp:
                    loadISVPInfo();
                    break;
                case cap:
                    loadCAPInfo();
                    break;
            }
        }
        return m_info;
    }

    /**
     * This method loads the CAP information.
     */
    private void loadCAPInfo()
    {
        XmlNode request = new XmlNode(Constants.GET_CAP_DETAILS, Constants.XMLNS_CAP);
        XmlNode cap = request.add("cap");
        cap.setText(getPackageDN());

        XmlNode response = call(request);

        XmlNode header = (XmlNode) response.get("tuple/old/ApplicationPackage/ApplicationDetails/Header");

        if (header != null)
        {
            String vendor = header.getChildText("Vendor");
            String version = header.getChildText("Version");
            String buildNumber = header.getChildText("BuildNumber");

            m_info = new DeployedPackageInfo(getPackageDN(), null, vendor, version, buildNumber);
        }
    }

    /**
     * This method loads and creates the deployed package information for an ISV package.
     */
    private void loadISVPInfo()
    {
        XmlNode request = new XmlNode(Constants.GET_ISVP_DEFINITION, Constants.XMLNS_ISV);
        XmlNode file = request.add("file");
        file.setText(getBasename());
        file.setAttribute("type", "isvpackage");
        file.setAttribute("onlyxml", "true");
        file.setAttribute("detail", "false");

        XmlNode isvPackage = call(request).getChild("ISVPackage");

        XmlNode desc = isvPackage.getChild("description");

        String vendor = desc.getChildText("owner");
        String fullVersion = desc.getChildText("version");
        String buildNumber = desc.getChildText("build");

        m_info = new DeployedPackageInfo(getPackageDN(), fullVersion, vendor, fullVersion, buildNumber);
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

    /**
     * This class holds the information about a deployed package.
     */
    private static class DeployedPackageInfo implements IDeployedPackageInfo
    {
        /** Holds the full version of the package */
        private String m_fullVersion;
        /** Holds the name of the package. */
        private String m_packageName;
        /** Holds the vendor of this package */
        private String m_vendor;
        /** Holds the version part */
        private String m_version;
        /** Holds the build number */
        private String m_buildNumber;

        /**
         * Instantiates a new deployed package info.
         * 
         * @param packageName The package name
         * @param fullVersion The full version
         * @param vendor The vendor
         * @param version The version
         * @param buildNumber The build number
         */
        DeployedPackageInfo(String packageName, String fullVersion, String vendor, String version, String buildNumber)
        {
            m_fullVersion = fullVersion;
            m_packageName = packageName;
            m_vendor = vendor;
            m_version = version;
            m_buildNumber = buildNumber;

            if (StringUtil.isEmptyOrNull(m_fullVersion))
            {
                m_fullVersion = m_version + "." + m_buildNumber;
            }
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#getFullVersion()
         */
        @Override
        public String getFullVersion()
        {
            return m_fullVersion;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#setFullVersion(java.lang.String)
         */
        @Override
        public void setFullVersion(String fullVersion)
        {
            m_fullVersion = fullVersion;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#getPackageName()
         */
        @Override
        public String getPackageName()
        {
            return m_packageName;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#setPackageName(java.lang.String)
         */
        @Override
        public void setPackageName(String packageName)
        {
            m_packageName = packageName;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#getVendor()
         */
        @Override
        public String getVendor()
        {
            return m_vendor;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#setVendor(java.lang.String)
         */
        @Override
        public void setVendor(String vendor)
        {
            m_vendor = vendor;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#getVersion()
         */
        @Override
        public String getVersion()
        {
            return m_version;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#setVersion(java.lang.String)
         */
        @Override
        public void setVersion(String version)
        {
            m_version = version;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#getBuildNumber()
         */
        @Override
        public String getBuildNumber()
        {
            return m_buildNumber;
        }

        /**
         * @see org.kisst.cordys.caas.IDeployedPackageInfo#setBuildNumber(java.lang.String)
         */
        @Override
        public void setBuildNumber(String buildNumber)
        {
            m_buildNumber = buildNumber;
        }

        /**
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString()
        {
            return getPackageName() + "(" + getVendor() + "; " + getFullVersion() + ")";
        }
    }
}
