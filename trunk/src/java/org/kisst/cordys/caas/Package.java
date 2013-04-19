/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class holds the information for a loaded package.
 */
public class Package extends CordysObject
{
    /** Holds the package types. */
    public enum EPackageType
    {
        cap, isvp
    };

    /** Holds the package types. */
    public enum EPackageStatus
    {
        loaded, incomplete, not_loaded
    };

    /** Holds the reference to the Cordys system that is being used. */
    private final CordysSystem system;
    /** Holds the name of the package. This is the Package DN that would be used in case the package is loaded. */
    private String name;
    /** Holds the type of package. It defaults to CAP */
    public EPackageType type = EPackageType.cap;
    /** Holds the definition XML for this package */
    public XmlNode definition;
    /** Holds the source filename for this package */
    public String filename;
    /** Holds the owner of the package */
    public String owner;
    /** Holds the build number of the package */
    public String buildnumber;
    /** Holds the version information */
    public String version;
    /** Holds the CN (package DN) of the package */
    public String cn;
    /** Holds the description of the package */
    public String description;
    /** Holds the status of the package (whether it is loaded or not) */
    public EPackageStatus status = EPackageStatus.not_loaded;
    /** Holds the roles that are part of this package. */
    public ChildList<Role> roles;
    /** Holds an alias for the roles. */
    public ChildList<Role> role;
    /** Holds an alias for the roles. */
    public ChildList<Role> r;
    /** Holds the web service interfaces. */
    public ChildList<WebServiceInterface> webServiceInterfaces;
    /** Alias for the web service interfaces. */
    public ChildList<WebServiceInterface> wsi;
    /** Holds the deployed package information for this package */
    private IDeployedPackageInfo m_info;
    /** Holds the runtime package DN object in LDAP. */
    private RuntimePackage runtime;

    /**
     * Instantiates a new package definition.
     * 
     * @param system The parent Cordys system
     * @param definition The definition of this package. This can be either an ISV package definition or an CAP definition.
     */
    protected Package(CordysSystem system, XmlNode definition)
    {
        this.system = system;

        // Based on the XML definition we need to determine whether it's an ISV package or that it is a CAP package. The XML of
        // the ISV package looks like this:

        // @formatter:off
        // <ISVPackage xmlns="http://schemas.cordys.com/1.0/isvpackage" file="Cordys_CommandConnector_1.1.3" cn="Cordys CommandConnector 1.1">
        //  <description>
        //          <owner>Cordys</owner>
        //          <name>CommandConnector</name>
        //          <version>1.1 Build 1.1.3</version>
        //          <cn>Cordys CommandConnector 1.1</cn>
        //          <wcpversion>C3.001</wcpversion>
        //          <build>3</build>
        //          <eula source="" />
        //          <sidebar source="" />
        //  </description>
        //  <content />
        //  <promptset />
        // </ISVPackage>
        // @formatter:on

        if ("ISVPackage".equals(definition.getName()))
        {
            type = EPackageType.isvp;
            name = definition.getAttribute("cn");
            filename = definition.getAttribute("file");

            owner = (String) definition.get("description/owner/text()");
            buildnumber = (String) definition.get("description/build/text()");
            version = (String) definition.get("description/version/text()");
            cn = (String) definition.get("description/cn/text()");

            // ISV packages are always loaded.
            status = EPackageStatus.loaded;
        }
        else if ("ApplicationPackage".equals(definition.getName()))
        {
            // @formatter:off
            // <ApplicationPackage>
            //   <ApplicationName>Cordys XMLStore</ApplicationName>
            //   <ApplicationVersion>D1.002</ApplicationVersion>
            //   <ApplicationBuild>26</ApplicationBuild>
            //   <owner>Cordys</owner>
            //   <node>CNL1523</node>
            // </ApplicationPackage>
            // @formatter:on

            // It is a CAP file
            type = EPackageType.cap;
            name = definition.getChildText("ApplicationName");
            owner = definition.getChildText("owner");

            buildnumber = definition.getChildText("ApplicationBuild");
            version = definition.getChildText("ApplicationVersion");

            if (owner == null || owner.isEmpty())
            {
                status = EPackageStatus.not_loaded;
            }
            else
            {
                status = EPackageStatus.loaded;
            }
        }

        // Build up the description
        description = name + " version " + getFullVersion();

        this.definition = definition.detach();

        if (status == EPackageStatus.loaded)
        {
            // The package is loaded, so we can read the main entry from the LDAP.
            runtime = new RuntimePackage(system, "cn=" + cn + "," + system.getDn());

            // Now that we have the LDAP entry we can also create the role list and the wsi list.
            webServiceInterfaces = runtime.webServiceInterfaces;
            wsi = runtime.webServiceInterfaces;

            roles = runtime.roles;
            role = runtime.roles;
            r = runtime.roles;
        }
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return system;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return system.getOrganization();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return StringUtil.quotedName(name);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return getVarName();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return system.getVarName() + ".packages." + StringUtil.quotedName(name);
    }

    /**
     * This method gets the status of this package.
     * 
     * @return The status of this package.
     */
    public EPackageStatus getStatus()
    {
        return status;
    }

    /**
     * This method sets the status of this package.
     * 
     * @param status The status of this package.
     */
    public void setStatus(EPackageStatus status)
    {
        this.status = status;
    }

    /**
     * This method returns whether or not the package is loaded.
     * 
     * @return Whether or not the package is loaded.
     */
    public boolean isLoaded()
    {
        return status == EPackageStatus.loaded;
    }

    /**
     * This method returns whether or not the package is incomplete.
     * 
     * @return Whether or not the package is incomplete.
     */
    public boolean isIncomplete()
    {
        return status == EPackageStatus.incomplete;
    }

    /**
     * This method gets the full version for this package.
     * 
     * @return The full version for this package.
     */
    public String getFullVersion()
    {
        return version + "." + buildnumber;
    }

    /**
     * This method gets the type of this package (either CAP or ISVP).
     * 
     * @return The type of this package (either CAP or ISVP).
     */
    public EPackageType getType()
    {
        return type;
    }

    /**
     * This method gets the Package DN for this package.
     * 
     * @return The Package DN for this package.
     */
    public String getPackageDN()
    {
        return name;
    }

    /**
     * This method gets the filename for the current package. This is only applicable for an ISV package. This is always tha
     * latest filename that is available.
     * 
     * @return The filename of the package.
     */
    public String getFilename()
    {
        return filename;
    }
    
    /**
     * This method returns the DN of the runtime location of the package.
     * 
     * @return The DN of the runtime location of the package.
     */
    public String getDn()
    {
        if (!isLoaded())
        {
            throw new CaasRuntimeException("The package is not yet loaded");
        }

        return runtime.getDn();
    }

    /**
     * This method returns the runtime version of the package.
     * 
     * @return The runtime package
     */
    public RuntimePackage getRuntime()
    {
        return runtime;
    }

    /**
     * This method unloads the package if it is currently loaded.
     * 
     * @param deleteReferences The delete references
     */
    public void unload(boolean deleteReferences)
    {
        if (status == EPackageStatus.loaded)
        {
            if (type == EPackageType.isvp)
            {
                for (Machine machine : system.machines)
                {
                    machine.unloadIsvp(this, deleteReferences);
                }

                system.removeLdap(runtime.getDn());

                // Force a reload of the packages
                system.packages.clear();
            }
            else if (type == EPackageType.cap)
            {
                system.undeployCap(cn, deleteReferences);
            }

            status = EPackageStatus.not_loaded;
        }
        else
        {
            throw new CaasRuntimeException("Package " + cn + " is not loaded");
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

        // The loading details are not loaded yet
        switch (type)
        {
            case isvp:
                loadISVPInfo();
                break;
            case cap:
                loadCAPInfo();
                break;
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

        XmlNode response = system.call(request);

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
        file.setText(getFilename());
        file.setAttribute("type", "isvpackage");
        file.setAttribute("onlyxml", "true");
        file.setAttribute("detail", "false");

        XmlNode isvPackage = system.call(request).getChild("ISVPackage");

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

    /**
     * This class is a wrapper around the DN entry for the package. It is needed for the creation of the role and wsi list.
     */
    public class RuntimePackage extends LdapObjectBase
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
        /** Alias for the web service interfaces. */
        public final ChildList<WebServiceInterface> wsi = webServiceInterfaces;

        /**
         * Instantiates a new runtime package.
         * 
         * @param system The system
         * @param dn The dn
         */
        protected RuntimePackage(CordysSystem system, String dn)
        {
            super(system, dn);
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
         * @see org.kisst.cordys.caas.support.LdapObject#getVarName()
         */
        @Override
        public String getVarName()
        {
            return Package.this.getVarName() + ".runtime";
        }
    }
}
