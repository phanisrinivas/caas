/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kisst.cordys.caas.comp.CompatibilityManagerFactory;
import org.kisst.cordys.caas.comp.ICompatibilityManager;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.support.XmlObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.PasswordHasher;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This is the main class for a Cordys system. From this class you can access organizations, manage CAP packages, create
 * authenticated users, etc.
 */
public class CordysSystem extends LdapObject
{
    /** Holds the object to use for executing SOAP requests. */
    private final SoapCaller caller;
    /** Holds the ldapcache. */
    private final HashMap<String, LdapObject> ldapcache = new HashMap<String, LdapObject>();
    /** Holds the name of this system. */
    private final String name;
    /** Holds the base DN of this instance. */
    private final String dn;
    /** Holds the environment details. */
    private final Environment env;
    /** Holds the properties. */
    private Properties properties = new Properties();
    /** Holds the version of the Cordys instance. */
    public final String version;
    /** Holds the build number of the Cordys instance. */
    public final String build;
    /** Holds the os of the Cordys instance. */
    public final String os;
    /** Holds whether or not the cache should be used. */
    public boolean useCache = true;
    /** Holds all the organizations in this Cordys instance. */
    public final Organization.OrganizationList organizations = new Organization.OrganizationList(this);
    /** Holds an alias for the organizations. */
    public final Organization.OrganizationList org = organizations;
    /** Holds an alias for the organizations. */
    public final Organization.OrganizationList o = organizations;
    /** Holds the list of packages for this system. */
    public final PackageList packages = new PackageList(this);
    /** Holds an alias for the loaded packages. */
    public final PackageList p = packages;
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final PackageList isvp = packages;
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final PackageList i = packages;
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final PackageList isvps = packages;
    /** Holds the all the authenticated users in this Cordys instance. */
    public final ChildList<AuthenticatedUser> authenticatedUsers = new ChildList<AuthenticatedUser>(this,
            "cn=authenticated users,", AuthenticatedUser.class);
    /** Holds an alias for the authenticated users. */
    public final ChildList<AuthenticatedUser> auser = authenticatedUsers;
    /** Holds an alias for the authenticated users. */
    public final ChildList<AuthenticatedUser> au = authenticatedUsers;
    /** Holds the definitions of the application connectors that are available on the Cordys system. */
    public final XmlObjectList<Connector> connectors = new XmlObjectList<Connector>(this, "/Cordys/WCP/Application Connector",
            null);
    /** Holds an alias for the connectors. */
    public final XmlObjectList<Connector> connector = connectors;
    /** Holds an alias for the connectors. */
    public final XmlObjectList<Connector> conn = connectors;
    /** Holds all the service containers that are available on the system. */
    public final CordysObjectList<ServiceContainer> serviceContainers = new ServiceContainerList(this);
    /** Holds an alias for the service containers. */
    public final CordysObjectList<ServiceContainer> sc = serviceContainers;
    /** Holds the nodes that are available in the cluster. */
    public final CordysObjectList<Machine> machines = new MachineList(this);
    /** Holds an alias for the machines. */
    public final CordysObjectList<Machine> machine = machines;
    /** Holds an alias for the machines. */
    public final CordysObjectList<Machine> nodes = machines;
    /** Holds the mapped machines based on the 'nodes' property. */
    public final Map<String, Machine> mapped = new LinkedHashMap<String, Machine>();
    /** Holds the compatibility manager to use for this Cordys installation. */
    private ICompatibilityManager m_cm;

    /**
     * Creates a cordys system and initializes it.
     * 
     * @param name Name of the cordys system as per the caas.conf file
     * @param caller SoapCaller instance configured in caas.conf depending on the cordys authentication
     */
    public CordysSystem(String name, SoapCaller caller)
    {
        super();
        this.env = Environment.get();
        this.name = name;
        this.caller = caller;

        XmlNode response = call(new XmlNode(Constants.GET_INSTALLATION_INFO, Constants.XMLNS_MONITOR));
        String tmp = response.getChildText("tuple/old/soapprocessorsinfo/processor/dn");
        String key = "cn=soap nodes,o=system,";
        this.dn = tmp.substring(tmp.indexOf(key) + key.length());
        this.version = response.getChildText("tuple/old/buildinfo/version");
        this.build = response.getChildText("tuple/old/buildinfo/build");
        this.os = response.getChildText("tuple/old/osinfo/version");
        rememberLdap(this);

        // Based on the version we need to create the Compatibility Manager
        m_cm = CompatibilityManagerFactory.create(this.version);

        // Parse the nodes defined for the cluster (if applicable).
        tmp = env.getProp("system." + name + ".nodes", null);
        if (!StringUtil.isEmptyOrNull(tmp))
        {
            String[] ns = tmp.split(";");
            for (String node : ns)
            {
                String[] tmp2 = node.split(":");
                if (tmp2.length == 2)
                {
                    String logicalName = tmp2[0];
                    String actual = tmp2[1];

                    Machine m = machines.getByName(actual);
                    if (m != null)
                    {
                        mapped.put(logicalName, m);
                    }
                }
            }
        }

        // If no mapped systems are defined, we'll create the mapped list with all the nodes and use their node name
        for (Machine m : machines)
        {
            if (!mapped.containsValue(m))
            {
                mapped.put(m.getName(), m);
            }
        }

        loadProperties();
    }

    /**
     * This method gets the compatibility manager to use for this Cordys installation.
     * 
     * @return The compatibility manager to use for this Cordys installation.
     */
    public ICompatibilityManager getCompatibilityManager()
    {
        return m_cm;
    }

    /**
     * This method sets the compatibility manager to use for this Cordys installation.
     * 
     * @param cm The compatibility manager to use for this Cordys installation.
     */
    public void setCompatibilityManager(ICompatibilityManager cm)
    {
        m_cm = cm;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return name;
    }

    /**
     * This method gets the SOAP caller that is to be used.
     * 
     * @return The SOAP caller that is to be used.
     */
    public SoapCaller getSoapCaller()
    {
        return caller;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return organizations.getByName("system");
    }

    /**
     * Creates an authenticated user.
     * 
     * @param name Name of the authenticated user
     * @param defaultOrgContext Name of the default organization
     */
    public void createAuthenticatedUser(String name, String defaultOrgContext)
    {
        createAuthenticatedUser(name, defaultOrgContext, null, null, null);
    }

    /**
     * Creates an authenticated user.
     * 
     * @param name Name of the authenticated user
     * @param defaultOrgContext Name of the default organization
     * @param type The type for the
     * @param osIdentity The os identity to use.
     * @param password The password for the user. If the password starts with {SHA1} then the password is added as is. Otherwise
     *            it is assumed to be plain text and thus a SHA1 hash will be calculated.
     */
    public void createAuthenticatedUser(String name, String defaultOrgContext, String type, String osIdentity, String password)
    {
        if (StringUtil.isEmptyOrNull(type))
        {
            type = "custom";
        }

        if (StringUtil.isEmptyOrNull(osIdentity))
        {
            osIdentity = name;
        }

        if (StringUtil.isEmptyOrNull(password))
        {
            password = name;
        }

        if (!password.startsWith("{SHA1}"))
        {
            password = PasswordHasher.encryptPassword(password);
        }

        XmlNode newEntry = newAuthenticatedUserEntryXml("cn=authenticated users,", name, "busauthenticationuser");
        newEntry.add("defaultcontext").add("string").setText(defaultOrgContext);
        newEntry.add("description").add("string").setText(name);
        newEntry.add("osidentity").add("string").setText(osIdentity);
        newEntry.add("authenticationtype").add("string").setText(type);
        newEntry.add("cn").add("string").setText(name);
        // Set the userPassword same as the osidentity
        newEntry.add("userPassword").add("string").setText(password);
        createInLdap(newEntry);
        authenticatedUsers.clear();
    }

    /**
     * Getter method for cordys system properties.
     * 
     * @return properties of the cordys system
     */
    public Properties getProperties()
    {
        return this.properties;
    }

    /**
     * Looks up the properties file for the cordys system Following is the preference order system.${SYSNAME}.properties.file >
     * ${SYSNAME}.properties > ${USER_HOME}/config/caas/${SYSNAME}.properties.
     * 
     * @return properties file name
     */
    public String getPropsFile()
    {
        String propsFileInConf = env.getProp("system." + name + ".properties.file", null);
        String propsFileInPWD = name + ".properties";
        String propsFileInHomeDir = System.getProperty("user.home") + "/config/caas/" + name + ".properties";
        propsFileInConf = StringUtil.getUnixStyleFilePath(propsFileInConf);
        propsFileInHomeDir = StringUtil.getUnixStyleFilePath(propsFileInHomeDir);

        String[] fileNames = new String[] { propsFileInConf, propsFileInPWD, propsFileInHomeDir };

        for (String fileName : fileNames)
        {
            if (FileUtil.doesFileExist(fileName))
            {
                return fileName;
            }
        }
        return null;
    }

    /**
     * Loads the properties.
     */
    private void loadProperties()
    {
        String propertyFile = getPropsFile();
        setDefaultProperties();

        if (propertyFile != null)
        {
            FileUtil.load(properties, propertyFile);
        }
    }

    /**
     * Sets the default properties.
     */
    private void setDefaultProperties()
    {
        properties.setProperty("LDAP_ROOT", this.getDn());
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#toString()
     */
    @Override
    public String toString()
    {
        return "CordysSystem(" + name + ")";
    }

    /**
     * This method gets the
     * 
     * @return The env
     */
    public Environment getEnv()
    {
        return env;
    }

    /**
     * This method gets the username that is used to connect to this system.
     * 
     * @return The username that is used to connect to this system.
     */
    public String getConnectionUser()
    {
        return caller.getUsername();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return this;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#getDn()
     */
    @Override
    public String getDn()
    {
        return dn;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#getCn()
     */
    @Override
    public String getCn()
    {
        return dn;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#getKey()
     */
    @Override
    public String getKey()
    {
        return "ldap:" + dn;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#myclear()
     */
    @Override
    public void myclear()
    {
        // It is not necessary to clear the cache, because that is just an index,
        // and guarantees that objects are never created twice.
        // Instead just the content of the objects is cleared.
        // ldapcache.clear(); rememberLdap(this);

        org.clear();
        machines.clear();
        sc.clear();
        p.clear();
        au.clear();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#useCache()
     */
    @Override
    public boolean useCache()
    {
        return useCache;
    }

    /**
     * Seek ldap.
     * 
     * @param dn The dn
     * @return The ldap object
     */
    public LdapObject seekLdap(String dn)
    {
        return ldapcache.get(dn);
    }

    /**
     * This method gets the ldap.
     * 
     * @param dn The dn
     * @return The ldap
     */
    public synchronized LdapObject getLdap(String dn)
    {
        return getLdap(dn, true);
    }

    /**
     * This method gets the ldap.
     * 
     * @param dn The dn
     * @return The ldap
     */
    public synchronized LdapObject getLdap(String dn, boolean useCache)
    {
        LdapObject result = null;

        if (useCache == true)
        {
            result = ldapcache.get(dn);
        }

        if (result != null)
        {
            return result;
        }
        result = LdapObjectBase.createObject(this, dn);
        rememberLdap(result);
        return result;
    }

    /**
     * This method gets the ldap.
     * 
     * @param entry The entry
     * @return The ldap
     */
    public LdapObject getLdap(XmlNode entry)
    {
        String dn = entry.getAttribute("dn");
        LdapObject result = ldapcache.get(dn);

        if (result != null)
        {
            return result;
        }
        result = LdapObjectBase.createObject(this, entry);
        rememberLdap(result);
        return result;
    }

    /**
     * This method adds the given LdapObject to the internal cache.
     * 
     * @param obj The obj to remember.
     */
    public void rememberLdap(LdapObject obj)
    {
        if (obj == null)
        {
            return;
        }
        ldapcache.put(obj.getDn(), obj);
    }

    /**
     * This method removes the ldap.
     * 
     * @param dn The dn
     */
    public void removeLdap(String dn)
    {
        ldapcache.remove(dn);
    }

    /**
     * This method executes the given SOAP request.
     * 
     * @param request The request to execute.
     * @param queryParams The query params
     * @return The string
     */
    public String call(String request, HashMap<String, String> queryParams)
    {
        return caller.call(request, queryParams);
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#call(org.kisst.cordys.caas.util.XmlNode, java.util.HashMap)
     */
    @Override
    public XmlNode call(XmlNode request, HashMap<String, String> queryParams)
    {
        return caller.call(request, queryParams);
    }

    /**
     * This method executes the given SOAP request.
     * 
     * @param request The request to execute
     * @return The string
     */
    public String call(String request)
    {
        return caller.call(request);
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#call(org.kisst.cordys.caas.util.XmlNode)
     */
    @Override
    public XmlNode call(XmlNode request)
    {
        return caller.call(request);
    }

    /**
     * Refresh service containers.
     */
    public void refreshServiceContainers()
    {
        for (Machine machine : machines)
        {
            machine.refreshServiceContainers();
        }
    }

    /**
     * Installs the given ISVP on all the Cordys server nodes with default timeout.
     * 
     * @param isvpFilePath Path of ISVP file
     */
    public void loadIsvp(String isvpFilePath)
    {
        // TODO: There should be a single webService for both load/upgrade of ISVP
        // By default timeout value is set to 10 minutes
        loadIsvp(isvpFilePath, 10);
    }

    /**
     * Installs the given ISVP on all the Cordys server nodes with the given timeout value.
     * 
     * @param isvpFilePath Path of ISVP file
     * @param timeOutInMinutes Timeout value in minutes
     */
    public void loadIsvp(String isvpFilePath, long timeOutInMinutes)
    {
        loadIsvp(isvpFilePath, null, timeOutInMinutes);
    }

    /**
     * Installs the ISVP on all the nodes with the given prompts set and default timeout.
     * 
     * @param isvpFilePath Path of ISVP file
     * @param isvpPromptsetsFilePath Path promptsets file
     */
    public void loadIsvp(String isvpFilePath, String isvpPromptsetsFilePath)
    {
        // By default timeout value is set to 10 minutes
        loadIsvp(isvpFilePath, isvpPromptsetsFilePath, 10);
    }

    /**
     * Installs the ISVP on all the Cordys nodes with given promptsets and with given timeout.
     * 
     * @param isvpFilePath Path of the ISVP file
     * @param isvpPromptsetsFilePath Path of the promptsets file
     * @param timeOutInMinutes Timeout value in minutes
     */
    public void loadIsvp(String isvpFilePath, String isvpPromptsetsFilePath, long timeOutInMinutes)
    {
        // Validate the input
        String isvpName = validateInput(isvpFilePath);
        // Convert the timeout value to seconds
        long timeOutInMillis = timeOutInMinutes * 60 * 1000;

        XmlNode prompSetsXMLNode = null;

        if (isvpPromptsetsFilePath != null)
        {
            prompSetsXMLNode = validatePromptSetFile(isvpPromptsetsFilePath);
        }

        // Iterate over the machines
        for (Machine machine : machines)
        {
            // Upload the ISVP on to the machine
            info("Uploading application " + isvpName + " to " + machine.getName() + " ... ");
            machine.uploadIsvp(isvpFilePath);
            info("OK");

            // TODO: check if dependent isvps are installed
            // Install the ISVP
            info("Installing application " + isvpName + " on " + machine.getName() + " ... ");

            String status = machine.loadIsvp(isvpName, prompSetsXMLNode, timeOutInMillis);
            info("OK");
            info("STATUS:: " + status);
        }
        isvp.clear();
    }

    /**
     * Upgrades the ISVP on Cordys nodes with default timeout and without the deleting references.
     * 
     * @param isvpFilePath Path of the ISVP file
     */
    public void upgradeIsvp(String isvpFilePath)
    {
        // By default timeout value is set to 10 minutes and deleteReferences flag is set to false
        upgradeIsvp(isvpFilePath, false, 10);
    }

    /**
     * Upgrades the ISVP on Cordys nodes with the given timeout and sets the deletereferences flag as provided during upgrade.
     * 
     * @param isvpFilePath Path of the ISVP file
     * @param deleteReferences Flag that indicates whether or not the references need to be deleted
     * @param timeOutInMinutes Timeout value in minutes
     */
    public void upgradeIsvp(String isvpFilePath, boolean deleteReferences, long timeOutInMinutes)
    {
        // TODO: While upgrading, First upgrade the primary node and after that the secondary nodes.
        // The reason for this is that the isvp's for primary and distributed nodes differ.
        upgradeIsvp(isvpFilePath, null, deleteReferences, timeOutInMinutes);
    }

    /**
     * Upgrades the ISVP on all the Cordys nodes with default timeout and with given promptsets Doesn't delete the references.
     * 
     * @param isvpFilePath The isvp file path
     * @param prompsetsFilePath The prompsets file path
     */
    public void upgradeIsvp(String isvpFilePath, String prompsetsFilePath)
    {
        // By default timeout value is set to 10 minutes and deleteReferences flag is set to false
        upgradeIsvp(isvpFilePath, prompsetsFilePath, false, 10);
    }

    /**
     * Upgrades the ISVP on all the Cordys nodes with the given timeout, promptsets Sets the deleteferences flag as provided
     * during the upgrade.
     * 
     * @param isvpFilePath Path of the ISVP file
     * @param isvpPromptsetsFilePath Path of the ISVP promptsets file
     * @param deleteReferences Delete references flag
     * @param timeOutInMinutes Timeout value in minutes
     */
    public void upgradeIsvp(String isvpFilePath, String isvpPromptsetsFilePath, boolean deleteReferences, long timeOutInMinutes)
    {
        // TODO: While upgrading, First upgrade the primary node and after that the secondary nodes.
        // The reason for this is that the isvp's for primary and distributed nodes differ.

        // Validate the input
        String isvpName = validateInput(isvpFilePath);
        // Convert the timeout value to seconds
        long timeOutInMillis = timeOutInMinutes * 60 * 1000;
        XmlNode prompSetsXMLNode = null;

        if (isvpPromptsetsFilePath != null)
        {
            prompSetsXMLNode = validatePromptSetFile(isvpPromptsetsFilePath);
        }

        // Iterate over the machines
        for (Machine machine : machines)
        {
            // TODO: Upload the ISVP only when it is not present on the machine
            info("Uploading application " + isvpName + " to " + machine.getName() + " ... ");
            machine.uploadIsvp(isvpFilePath);
            info("OK");
            // Upgrade the ISVP
            info("Upgrading application " + isvpName + " on " + machine.getName() + " ... ");

            String status = machine.upgradeIsvp(isvpName, prompSetsXMLNode, deleteReferences, timeOutInMillis);
            info("OK");
            info("STATUS:: " + status);
        }
        isvp.clear();
    }

    /**
     * Un-installs the given ISVP from all Cordys nodes.
     * 
     * @param isvp ISVP to be un-installed
     * @param deleteReferences Flag that indicates whether or not the references to be deleted
     * @return true if the un-installation is successful, false otherwise
     */
    public boolean unloadIsvp(Package isvp, boolean deleteReferences)
    {
        if (isvp == null)
        {
            throw new CaasRuntimeException("Isvp can not be null");
        }

        // TODO: machine class should be a list of its own installed isvps
        for (Machine machine : machines)
        {
            info("Unloading " + isvp.getName() + " on " + machine.getName() + " with deleteReference=" + deleteReferences
                    + " ... ");
            machine.unloadIsvp(isvp, deleteReferences);
            info("OK");
        }
        isvp.clear();
        return true;
    }

    /**
     * Un-installs the ISVP from all the Cordys nodes.
     * 
     * @param isvpName Name of the ISVP file
     * @param deleteReferences Flag that indicates whether or not the references to be deleted
     * @return true if the un-installation is successful, false otherwise
     */
    public boolean unloadIsvp(String isvpName, boolean deleteReferences)
    {
        if (StringUtil.isEmptyOrNull(isvpName))
        {
            throw new CaasRuntimeException("Isvp name can not be null or empty");
        }

        Package isvp = isvps.getByName(isvpName);

        if (isvp != null)
        {
            unloadIsvp(isvp, deleteReferences);
        }
        else
        {
            throw new CaasRuntimeException(isvpName + " is not installed on " + name);
        }
        return true;
    }

    /**
     * Validates the ISVP file path.
     * 
     * @param isvpFilePath Path of the ISVP file
     * @return ISVP name
     */
    private String validateInput(String isvpFilePath)
    {
        // Check if the ISVP file path is empty or null
        if (StringUtil.isEmptyOrNull(isvpFilePath))
        {
            throw new CaasRuntimeException("ISVP file path is empty or null");
        }
        isvpFilePath = isvpFilePath.trim();
        isvpFilePath = StringUtil.getUnixStyleFilePath(isvpFilePath);

        File isvpFile = new File(isvpFilePath);

        // Check if the ISVP file exists at the given location
        if (!isvpFile.exists())
        {
            throw new CaasRuntimeException(isvpFilePath + " doesn't exist");
        }

        // Extract the ISVP name from the complete path of the ISVP
        String isvpName = isvpFile.getName();

        // Check the extension of the file
        if (!isvpName.endsWith(".isvp"))
        {
            throw new CaasRuntimeException("Invalid ISVP file " + isvpName);
        }
        return isvpName;
    }

    /**
     * Validates ISVP promptsets file.
     * 
     * @param isvpPromptSetFilePath Path of the ISVP promptsets file
     * @return promptsets XmlNode
     */
    private XmlNode validatePromptSetFile(String isvpPromptSetFilePath)
    {
        // Check if the ISVP promptset file path is empty or null
        if (StringUtil.isEmptyOrNull(isvpPromptSetFilePath))
        {
            throw new CaasRuntimeException("ISVP promptsets file path is empty or null");
        }
        isvpPromptSetFilePath = isvpPromptSetFilePath.trim();
        isvpPromptSetFilePath = StringUtil.getUnixStyleFilePath(isvpPromptSetFilePath);

        File isvpPromptSetFile = new File(isvpPromptSetFilePath);

        // Check if the ISVP file exists at the given location
        if (!isvpPromptSetFile.exists())
        {
            throw new CaasRuntimeException(isvpPromptSetFile + " doesn't exist");
        }

        String pomptsetsXML = FileUtil.loadString(isvpPromptSetFilePath);
        @SuppressWarnings({ "unchecked", "rawtypes" })
        HashMap<String, String> variables = new HashMap<String, String>((Map) properties);
        pomptsetsXML = StringUtil.substitute(pomptsetsXML, variables);

        XmlNode promposetsXMLNode = new XmlNode(pomptsetsXML);
        return promposetsXMLNode;
    }

    /**
     * @see org.kisst.cordys.caas.support.LdapObject#compareTo(org.kisst.cordys.caas.support.CordysObject)
     */
    @Override
    public int compareTo(CordysObject o)
    {
        return dn.compareTo(o.getKey());
    }

    /**
     * This method gets the xml with the given key from the XML store.
     * 
     * @param key The key of the file in the XML store.
     * @return The xml that was found.
     */
    public XmlNode getXml(String key)
    {
        return getXml(key, "isv", null);
    }

    /**
     * This method gets the xml with the given key from the XML store.
     * 
     * @param key The key of the file in the XML store.
     * @param version The version (isv or organization)
     * @param organization The organization to get it from. If null the default organization is used.
     * @return The xml
     */
    public XmlNode getXml(String key, String version, String organization)
    {
        XmlNode request = new XmlNode(Constants.GET_XML_OBJECT, Constants.XMLNS_XMLSTORE);
        XmlNode keynode = request.add("key");
        keynode.setText(key);
        keynode.setAttribute("version", version);

        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("organization", organization);

        XmlNode response = caller.call(request, queryParams);

        return response.getChild("tuple/old").getChildren().get(0);
    }

    /**
     * This method seeks all users and roles that have the given role attached throughout the entire organization.
     * 
     * @param target The role to find
     * @return The cordys object list containing all users and roles that have the given role attached.
     */
    public CordysObjectList<LdapObject> seek(final Role target)
    {
        return new CordysObjectList<LdapObject>(getSystem()) {
            @Override
            protected void retrieveList()
            {
                for (Organization org : organizations)
                {
                    for (LdapObject obj : org.seek(target))
                    {
                        grow(obj);
                    }
                }
            }

            @Override
            public String getKey()
            {
                return CordysSystem.this.getKey() + ":seek(" + target + ")";
            }

            @Override
            public Organization getOrganization()
            {
                return CordysSystem.this.getOrganization();
            }
        };
    }

    /**
     * This method returns the service groups that have the given web service interface attached to them.
     * 
     * @param target The target The WSI to search for.
     * @return The cordys object list containing the service groups that have the given web service interface attached to it.
     */
    public CordysObjectList<ServiceGroup> seek(final WebServiceInterface target)
    {
        return new CordysObjectList<ServiceGroup>(getSystem()) {
            @Override
            protected void retrieveList()
            {
                for (Organization org : organizations)
                {
                    for (ServiceGroup obj : org.seek(target))
                    {
                        grow(obj);
                    }
                }
            }

            @Override
            public String getKey()
            {
                return CordysSystem.this.getKey() + ":seek(" + target + ")";
            }

            @Override
            public Organization getOrganization()
            {
                return CordysSystem.this.getOrganization();
            }
        };
    }

    /**
     * This method will upload the given CAP file to the Cordys system. CAP works different then ISVP. Cordys takes care that all
     * nodes get the CAP package.
     * 
     * @param capFile The CAP file to load.
     */
    public void uploadCap(String capFile)
    {
        if (packages.supportsCap() == false)
        {
            throw new CaasRuntimeException("The system " + name + " does not support CAP packages");
        }

        File cap = new File(capFile);

        if (!cap.exists())
        {
            throw new CaasRuntimeException("CAP file " + cap.getAbsolutePath() + " does not exist");
        }

        String filename = cap.getName();
        String capEncodedContent = FileUtil.encodeFile(capFile);

        XmlNode request = new XmlNode(Constants.UPLOAD_CAP, Constants.XMLNS_CAP);
        request.add("name").setText(filename);
        request.add("content").setText(capEncodedContent);

        // With ISV packages we needed to upload it to each monitor individually. But with CAP it is not needed anymore.
        call(request);
    }

    /**
     * This method will deploy the latest version of the given CAP package name. It will first check to see if there is a package
     * to deploy and whether it's an upgrade or a fresh load.
     * 
     * @param name The name
     */
    public void deployCap(String name)
    {
        deployCap(name, 10);
    }

    /**
     * This method will deploy the latest version of the given CAP package name. It will first check to see if there is a package
     * to deploy and whether it's an upgrade or a fresh load.
     * 
     * @param name The name
     * @param timeoutInMinutes The timeout in minutes.
     */
    public void deployCap(String name, double timeoutInMinutes)
    {
        if (packages.supportsCap() == false)
        {
            throw new CaasRuntimeException("The system " + name + " does not support CAP packages");
        }

        // First get the status of the package. Is it indeed a new one
        m_cm.deployCap(getSoapCaller(), this, name, timeoutInMinutes);
    }

    /**
     * This method will undeploy the given cap package.
     * 
     * @param name The package DN of the package.
     */
    public void undeployCap(String name)
    {
        undeployCap(name, null, null, 10);
    }

    /**
     * This method will undeploy the given cap package.
     * 
     * @param name The package DN of the package.
     * @param timeoutInMinutes The timeout in minutes
     */
    public void undeployCap(String name, long timeoutInMinutes)
    {
        undeployCap(name, null, null, timeoutInMinutes);
    }

    /**
     * This method will undeploy the given cap package.
     * 
     * @param name The package DN of the package.
     * @param deleteReferences Whether or not to delete the references of the package
     */
    public void undeployCap(String name, Boolean deleteReferences)
    {
        undeployCap(name, null, deleteReferences, 10);
    }

    /**
     * This method will undeploy the given cap package.
     * 
     * @param name The package DN of the package.
     * @param deleteReferences Whether or not to delete the references of the package
     * @param timeoutInMinutes The timeout in minutes
     */
    public void undeployCap(String name, Boolean deleteReferences, long timeoutInMinutes)
    {
        undeployCap(name, null, deleteReferences, timeoutInMinutes);
    }

    /**
     * This method will undeploy the given cap package.
     * 
     * @param name The package DN of the package.
     * @param userInputs The user inputs XML.
     * @param deleteReferences Whether or not to delete the references of the package
     * @param timeoutInMinutes The timeout in minutes
     */
    public void undeployCap(String name, String userInputs, Boolean deleteReferences, long timeoutInMinutes)
    {
        if (packages.supportsCap() == false)
        {
            throw new CaasRuntimeException("The system " + name + " does not support CAP packages");
        }
        
        m_cm.undeployCap(getSoapCaller(), this, name, userInputs, deleteReferences, timeoutInMinutes);
    }

    /**
     * This method will revert the given incomplete cap package. The default timeout is 10 minutes.
     * 
     * @param name The package DN of the package.
     */
    public void revertCap(String name)
    {
        revertCap(name, 10);
    }

    /**
     * This method will revert the given incomplete cap package.
     * 
     * @param name The package DN of the package.
     * @param timeoutInMinutes The timeout in minutes
     */
    public void revertCap(String name, long timeoutInMinutes)
    {
        if (packages.supportsCap() == false)
        {
            throw new CaasRuntimeException("The system " + name + " does not support CAP packages");
        }
        
        m_cm.revertCap(getSoapCaller(), this, name, timeoutInMinutes);
    }

    /**
     * This method will download the currently deployed package with the given name from the Cordys server to the given
     * destination folder.
     * 
     * @param packageDn The DN of the package.
     * @param destination The destination folder to write the package to.
     */
    public void donwloadCap(String packageDn, String destination)
    {
        throw new IllegalAccessError("Currently not supported by Cordys yet.");
    }

    /**
     * This method gets a list of the mapped machines for this system. If will examine the 'nodes' property of the system. That
     * 
     * @return The mapped systems
     */
    public Map<String, Machine> getMappedMachines()
    {
        return mapped;
    }

    /**
     * Holds the list of machines (nodes) in the cluster.
     */
    private final class MachineList extends CordysObjectList<Machine>
    {
        /**
         * Instantiates a new machine list.
         * 
         * @param system The system
         */
        private MachineList(CordysSystem system)
        {
            super(system);
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // We cannot use the generic service containers as it would iterate over all organizations. Monitor service containers
            // are only in the system organization. So we only need to get the ones from the system org.
            Organization system = o.getByName("system");
            for (ServiceContainer sc : system.serviceContainers)
            {
                if (sc.getName().indexOf("monitor") >= 0)
                {
                    grow(new Machine(sc));
                }
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return getSystem().getKey() + ":machines";
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return CordysSystem.this.getOrganization();
        }

    }

    /**
     * Holds the convenience list of all service containers across all organizations.
     */
    private final class ServiceContainerList extends CordysObjectList<ServiceContainer>
    {
        /**
         * Instantiates a new service container list.
         * 
         * @param system The system
         */
        private ServiceContainerList(CordysSystem system)
        {
            super(system);
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // We cannot use the organization here. Because if we have a lot of organizations we will do a LDAP call for each
            // organization. A more elegant way to get all the service containers is by using a SearchLDAP request on the Service
            // containers.
            XmlNode request = new XmlNode(Constants.SEARCH_LDAP, Constants.XMLNS_LDAP);
            request.add("dn").setText(dn);
            request.add("scope").setText("3");
            request.add("filter").setText("(objectclass=bussoapprocessor)");
            request.add("sort").setText("false");
            request.add("returnValues").setText("false");

            XmlNode response = getSystem().call(request);

            Pattern p = Pattern.compile("^cn=([^,]+),cn=([^,]+),cn=soap nodes,o=([^,]+)");

            List<XmlNode> children = response.getChildren("tuple");
            for (XmlNode child : children)
            {
                XmlNode e = (XmlNode) child.get("old/entry");

                String scDN = e.getAttribute("dn");

                // From the DN we can find out the organization name and the service group. Based on that we need to find the
                // parent.
                Matcher m = p.matcher(scDN);
                if (m.find())
                {
                    ServiceContainer sc = (ServiceContainer) LdapObjectBase.createObject(getSystem(), e);
                    grow(sc);
                }
                else
                {
                    info(scDN + " doe snot match pattern");
                }
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return getSystem().getKey() + ":serviceContainers";
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return CordysSystem.this.getOrganization();
        }

    }

}
