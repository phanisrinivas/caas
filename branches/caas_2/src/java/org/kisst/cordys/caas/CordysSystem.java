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
import java.util.Map;
import java.util.Properties;

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
 * Holds the Class CordysSystem.
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
    public final ChildList<Organization> organizations = new ChildList<Organization>(this, Organization.class);
    /** Holds an alias for the organizations. */
    public final ChildList<Organization> org = organizations;
    /** Holds an alias for the organizations. */
    public final ChildList<Organization> o = organizations;
    /** Holds the packages that are installed on the system. */
    public final CordysObjectList<PackageDefinition> packages = new PackageDefinitionList(this);
    /** Holds an alias for the packages. */
    public final CordysObjectList<PackageDefinition> p = packages;
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final ChildList<Package> isvp = new ChildList<Package>(this, Package.class);
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final ChildList<Package> i = isvp;
    /** Holds an alias for the packages. This is for backwards compatibility. */
    public final ChildList<Package> isvps = isvp;
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

        loadProperties();
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
     * Creates an authenticated user.
     * 
     * @param name Name of the authenticated user
     * @param defaultOrgContext Name of the default organization
     */
    public void createAuthenticatedUser(String name, String defaultOrgContext)
    {
        XmlNode newEntry = newAuthenticatedUserEntryXml("cn=authenticated users,", name, "busauthenticationuser");
        newEntry.add("defaultcontext").add("string").setText(defaultOrgContext);
        newEntry.add("description").add("string").setText(name);
        newEntry.add("osidentity").add("string").setText(name);
        newEntry.add("cn").add("string").setText(name);
        // Set the userPassword same as the osidentity
        newEntry.add("userPassword").add("string").setText(PasswordHasher.encryptPassword(name));
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
     * This method gets the env.
     * 
     * @return The env
     */
    public Environment getEnv()
    {
        return env;
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
        LdapObject result = ldapcache.get(dn);

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
     * Remember ldap.
     * 
     * @param obj The obj
     */
    private void rememberLdap(LdapObject obj)
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
            env.info("Uploading application " + isvpName + " to " + machine.getName() + " ... ", false);
            machine.uploadIsvp(isvpFilePath);
            env.log("", "OK", true);

            // TODO: check if dependent isvps are installed
            // Install the ISVP
            env.info("Installing application " + isvpName + " on " + machine.getName() + " ... ", false);

            String status = machine.loadIsvp(isvpName, prompSetsXMLNode, timeOutInMillis);
            env.log("", "OK", true);
            env.info("STATUS:: " + status);
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
            env.info("Uploading application " + isvpName + " to " + machine.getName() + " ... ", false);
            machine.uploadIsvp(isvpFilePath);
            env.log("", "OK", true);
            // Upgrade the ISVP
            env.info("Upgrading application " + isvpName + " on " + machine.getName() + " ... ", false);

            String status = machine.upgradeIsvp(isvpName, prompSetsXMLNode, deleteReferences, timeOutInMillis);
            env.log("", "OK", true);
            env.info("STATUS:: " + status);
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
            env.info("Unloading " + isvp.getName() + " on " + machine.getName() + " with deleteReference=" + deleteReferences
                    + " ... ", false);
            machine.unloadIsvp(isvp, deleteReferences);
            env.log("", "OK", true);
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
        return response.getChild("tuple/old");
    }

    /**
     * This method seeks all users and roles that have the given role attached throughout the entire organization
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
        };
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
            for (ServiceContainer sc : serviceContainers)
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
            for (Organization o : organizations)
            {
                for (ServiceContainer sc : o.serviceContainers)
                {
                    grow(sc);
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
    }
}
