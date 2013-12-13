package org.kisst.cordys.caas.cm;

import java.io.File;
import java.util.ArrayList;
import java.util.Map;

import org.kisst.cordys.caas.Configuration;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.XMLStoreObject;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class is used to create the template of a given organization.
 */
public class Template
{
    /** The environment that is used. */
    private static final Environment env = Environment.get();
    /** Holds the created template */
    private final String template;
    /** Holds whether or not the template is empty. */
    private boolean empty = true;

    /**
     * Instantiates a new template.
     * 
     * @param template The template XML.
     */
    public Template(String template)
    {
        this.template = template;
    }

    /**
     * Creates a new Template object.
     * 
     * @param org The organization for which the template should be created / applied
     * @param targetPackageName The name of the target package.
     */
    public Template(Organization org, String targetPackageName)
    {
        this(org, targetPackageName, null, null);
    }

    /**
     * Extracts a template of the given organization
     * 
     * @param org Organization which template needs to be created
     * @param targetPackageName The name of the target package.
     * @param isvp The package for which the template should be generated.
     * @param userDn The user to generate the template for.
     */
    public Template(Organization org, String targetPackageName, Package pkg, User u)
    {
        env.info("Exporting template for " + org.getName() + " organization");
        XmlNode result = new XmlNode("org", Constants.XMLNS_TEMPLATE);
        if (targetPackageName != null)
        {
            result.setAttribute("package", targetPackageName);
        }

        result.setAttribute("org", org.getName());

        // First we export all the non-Cordys packages.
        env.info("Exporting non-Cordys packages... ");
        CordysSystem system = org.getSystem();
        for (Package p : system.packages)
        {
            IDeployedPackageInfo pi = p.getInfo();

            if (pi != null && !"Cordys".equals(pi.getVendor()))
            {
                env.debug("Found non-standard package: " + pi.getPackageName());

                XmlNode node = result.add("package");
                node.setAttribute("name", pi.getPackageName());

                XmlNode version = node.add("version");
                version.setAttribute("version", pi.getFullVersion());
                version.setAttribute("tested", "OK");

                version.add("warning").setAttribute("message", "Package " + pi.getPackageName() + " should be loaded");
            }
        }

        // Export the DSOs in the given organization
        env.info("Exporting " + org.dsos.getSize() + " dso objects ... ");
        for (DsoType dsotype : org.dsotypes)
        {
            for (Dso dso : dsotype.dsos)
            {
                XmlNode node = result.add("dso");
                node.setAttribute("name", dso.getName());
                node.setAttribute("desc", dso.getProp("desc").toString());
                node.setAttribute("type", dsotype.getName());
                XmlNode configNode = dso.config.getXml().clone();
                node.add("datasourceconfiguration").add(configNode);
            }
        }

        // Export XML Store objects
        env.info("Exporting " + org.xmlStoreObjects.getSize() + " xmlstore objects ... ");
        for (XMLStoreObject xso : org.xmlStoreObjects)
        {
            XmlNode node = result.add("xmlstoreobject");
            node.setAttribute("key", xso.getKey());
            node.setAttribute("version", xso.getVersion());
            node.setAttribute("name", xso.getName());
            node.add(xso.getXML().clone());
        }

        // Exporting local roles
        env.info("Exporting " + org.roles.getSize() + " roles ... ");
        for (Role role : org.roles)
        {
            XmlNode node = result.add("role");
            node.setAttribute("name", role.getName());
            if (role.type.get() != null)
            {
                node.setAttribute("type", role.type.get());
            }
            for (Role subRole : role.roles)
            {
                String isvpName = null;
                if (subRole.getParent() instanceof Organization)
                {
                    if (subRole.getName().equals("everyoneIn" + org.getName()))
                        continue;
                    isvpName = targetPackageName;
                }
                else
                    isvpName = subRole.getParent().getName();
                XmlNode child = node.add("role");
                child.setAttribute("name", subRole.getName());
                if (subRole.type.get() != null)
                {
                    child.setAttribute("type", subRole.type.get());
                }
                if (isvpName != null)
                    child.setAttribute("package", isvpName);
            }
        }

        // Exporting users in the organization
        env.info("Exporting " + org.users.getSize() + " users ... ");
        for (User user : org.users)
        {
            if ("SYSTEM".equals(user.getName().toUpperCase()))
                continue; // SYSTEM user should not be part of the template
            XmlNode node = result.add("user");
            node.setAttribute("name", user.getName());
            node.setAttribute("au", user.au.getRef().getName());
            for (Role role : user.roles)
            {
                String isvpName = null;
                if (role.getParent() instanceof Organization)
                {
                    if (role.getName().equals("everyoneIn" + org.getName()))
                        continue;
                    isvpName = targetPackageName;
                }
                else
                    isvpName = role.getParent().getName();
                XmlNode child = node.add("role");
                child.setAttribute("name", role.getName());
                if (role.type.get() != null)
                {
                    child.setAttribute("type", role.type.get());
                }
                if (isvpName != null)
                    child.setAttribute("package", isvpName);
            }
        }

        // Exporting service groups.
        env.info("Exporting " + org.serviceGroups.getSize() + " service groups ... ");
        for (ServiceGroup serviceGroup : org.serviceGroups)
        {
            XmlNode node = result.add("servicegroup");
            node.setAttribute("name", serviceGroup.getName());
            XmlNode configNode = serviceGroup.config.getXml().clone();
            XmlNode keyStoreNode = configNode.getChild("soapnode_keystore");
            if (keyStoreNode != null)
                configNode.remove(keyStoreNode);
            
            //If no namespace is defined on the given XML node we need to explicitly set it to ""
            if (configNode.getNamespace() == null)
            {
                configNode.setNamespace("");
            }
            
            node.add("bussoapnodeconfiguration").add(configNode);
            for (WebServiceInterface wsi : serviceGroup.webServiceInterfaces)
            {
                XmlNode child = node.add("wsi");
                child.setAttribute("name", wsi.getName());
                String isvpName = null;
                if (wsi.getParent() instanceof Organization)
                    isvpName = targetPackageName;
                else
                    isvpName = wsi.getParent().getName();
                if (isvpName != null)
                    child.setAttribute("package", isvpName);
            }
            for (ServiceContainer serviceContainer : serviceGroup.serviceContainers)
            {
                XmlNode child = node.add("sc");
                child.setAttribute("name", serviceContainer.getName());
                child.setAttribute("automatic", "" + serviceContainer.automatic.getBool());
                child.add("bussoapprocessorconfiguration").add(serviceContainer.config.getXml().clone());
                for (ConnectionPoint cp : serviceContainer.connectionPoints)
                {
                    XmlNode cpNode = child.add("cp");
                    cpNode.setAttribute("name", cp.getName());

                    // Check the type
                    String labeledURI = cp.uri.get();
                    String description = cp.desc.get();

                    if (labeledURI.startsWith("socket"))
                    {
                        cpNode.setAttribute("type", "socket");
                        // No need to add the child nodes for the socket
                    }
                    else if (labeledURI.startsWith("msmq"))
                    {
                        cpNode.setAttribute("type", "msmq://");

                        cpNode.add("labeleduri").setText(labeledURI);
                        cpNode.add("description").setText(description);
                    }
                    else if (labeledURI.startsWith("jms://"))
                    {
                        cpNode.setAttribute("type", "jms");

                        cpNode.add("labeleduri").setText(labeledURI);
                        cpNode.add("description").setText(description);
                    }
                    else
                    {
                        cpNode.setAttribute("type", "other");

                        cpNode.add("labeleduri").setText(labeledURI);
                        cpNode.add("description").setText(description);
                    }
                }
            }
        }

        String str = result.getPretty();
        this.template = str.replace("$", "${dollar}");
    }

    /**
     * Saves the template to the given file. Substitutes the values found in the template with their corresponding keys from the
     * Map
     * 
     * @param filename absolute path of the template file
     * @param vars Map containing the properties
     */
    public void save(String filename, Map<String, String> vars)
    {
        FileUtil.saveString(new File(filename), StringUtil.reverseSubstitute(template, vars));
        env.info("Template successfully exported to " + filename);
    }

    /**
     * This method returns whether or not the template is empty.
     * 
     * @return Whether or not the template is empty.
     */
    public boolean isEmpty()
    {
        return empty;
    }

    /**
     * Resolves the variables in the template with the values from the Map
     * 
     * @param vars Map containing the properties
     * @return XmlNode of the template after resolving the variables in it
     */
    public XmlNode xml(Map<String, String> vars)
    {
        String str = template;
        if (vars != null)
            str = StringUtil.substitute(str, vars);
        str = str.replace("${dollar}", "$");
        return new XmlNode(str);
    }

    /**
     * Applies the template to the given organization
     * 
     * @param org Organization object to which the template needs to be applied
     * @param conf Configuration object containing the properties
     */
    public void apply(Organization org, Configuration conf)
    {
        apply(org, conf.getProps());
    }

    /**
     * Applies the template to the given organization
     * 
     * @param org Organization to which the template needs to be applied
     * @param vars Map containing the properties
     */
    public void apply(Organization org, Map<String, String> vars)
    {
        env.info("Importing template to " + org.getName() + " organization");
        XmlNode template = xml(vars);
        for (XmlNode node : template.getChildren())
        {
            if (node.getName().equals("dso"))
                processDso(org, node);
            else if (node.getName().equals("xmlstoreobject"))
                processXMLStoreObject(org, node);
            else if (node.getName().equals("role"))
                processRole(org, node);
            else if (node.getName().equals("user"))
                processUser(org, node);
            else if (node.getName().equals("servicegroup"))
                processServiceGroup(org, node);
            else if (node.getName().equals("package"))
                processPackage(org, node);
            else
                env.warn("Unknown organization element " + node.getPretty());
        }
        env.info("Template successfully imported to " + org.getName() + " organization");
    }

    /**
     * This method processes the given package in the template. For now it will do nothing until automatic loading is supported.
     * 
     * @param org The organization to apply it to.
     * @param node The node defining the package.
     */
    private void processPackage(Organization org, XmlNode node)
    {
        env.warn("Automatic deployment of package " + node.getAttribute("name") + " not supported.");
    }

    /**
     * Appends/overwrites the XMLStore
     * 
     * @param org Organization where the XMLStore object needs to be processed
     * @param xmlStoreObjNode XmlNode of the XMLStore object from the template
     */
    private void processXMLStoreObject(Organization org, XmlNode xmlStoreObjNode)
    {
        // Remove xml comments
        String uncommented = StringUtil.removeXmlComments(xmlStoreObjNode.compact());
        xmlStoreObjNode = new XmlNode(uncommented);
        String operationFlag = xmlStoreObjNode.getAttribute("operation");
        String key = xmlStoreObjNode.getAttribute("key");
        String version = xmlStoreObjNode.getAttribute("version");
        String name = xmlStoreObjNode.getAttribute("name");
        XmlNode newXml = xmlStoreObjNode.getChildren().get(0);
        XMLStoreObject obj = new XMLStoreObject(key, version, org);
        if (operationFlag != null && operationFlag.equals("overwrite"))
        {
            env.info("Overwriting " + name + " xmlstore object ... ");
            obj.overwriteXML(newXml.clone());
        }
        else if (operationFlag != null && operationFlag.equals("append"))
        {
            env.info("Appending " + name + " xmlstore object ... ");
            obj.appendXML(newXml.clone());
        }
        else
        // By default overwrite the XMStore object
        {
            env.info("Overwriting " + name + " xmlstore object ... ");
            obj.overwriteXML(newXml.clone());
        }
        env.info("OK");
    }

    /**
     * Creates/updates DSO. Also creates the DSO type if it does not exist.
     * 
     * @param org Organization where the DSO needs to be created/updated
     * @param dsoNode XmlNode representing the DSO as per the template
     */
    private void processDso(Organization org, XmlNode dsoNode)
    {
        String name = dsoNode.getAttribute("name");
        String desc = dsoNode.getAttribute("desc");
        String type = dsoNode.getAttribute("type");
        XmlNode config = dsoNode.getChild("datasourceconfiguration").getChildren().get(0).clone();
        DsoType dsotype = org.dsotypes.getByName(type);
        if (dsotype == null) // Holds true only once per dsotype of org
        {
            env.info("creating dsotype " + type + " ... ");
            org.createDsoType(type);
            env.info("OK");
            dsotype = org.dsotypes.getByName(type);
        }
        Dso dso = dsotype.dsos.getByName(name);
        if (dso == null) // Create DSO
        {
            env.info("creating dso " + name + " ... ");
            dsotype = org.dsotypes.getByName(type);
            dsotype.createDso(name, desc, config);
            env.info("OK");
        }
        else
        // Update DSO
        {
            env.info("updating dso " + name + " ... ");
            dsotype.updateDso(dso, config);
            env.info("OK");
        }
    }

    /**
     * Creates/updates service group. Also configures the web service interfaces and creates/updates corresponding service
     * containers
     * 
     * @param org Organization where the service group needs to be created/updated
     * @param serviceGroupNode XmlNode representing the service group as per the template
     */
    private void processServiceGroup(Organization org, XmlNode serviceGroupNode)
    {
        // Process SG
        String name = serviceGroupNode.getAttribute("name");
        ServiceGroup serviceGroup = org.serviceGroups.getByName(name);
        if (serviceGroup == null) // Create SG
        {
            env.info("creating servicegroup " + name + " ... ");
            XmlNode config = serviceGroupNode.getChild("bussoapnodeconfiguration").getChildren().get(0).clone();
            org.createServiceGroup(name, config, getWebServiceInterfaces(org, serviceGroupNode));
            serviceGroup = org.serviceGroups.getByName(name);
            env.info("OK");
        }
        else
        // Update SG
        {
            env.info("updating servicegroup " + name + " ... ");
            WebServiceInterface[] newWebServiceInterfaces = getWebServiceInterfaces(org, serviceGroupNode);
            if ((newWebServiceInterfaces != null) && (newWebServiceInterfaces.length > 0))
            {
                serviceGroup.webServiceInterfaces.update(newWebServiceInterfaces);
                ArrayList<String> namepsaces = new ArrayList<String>();
                for (WebServiceInterface webServiceInterface : newWebServiceInterfaces)
                {
                    for (String namespace : webServiceInterface.namespaces.get())
                    {
                        namepsaces.add(namespace);
                    }
                }
                serviceGroup.namespaces.update(namepsaces);
                env.info("OK");
            }
        }
        // Process SCs
        CordysObjectList<Machine> machines = org.getSystem().machines;
        int i = 0;
        if (serviceGroupNode.getChildren("sc").size() != machines.getSize())
        {
            env.warn("Template says " + serviceGroupNode.getChildren("sc").size() + " servicecontainers for " + name
                    + " but the no of machines are " + org.getSystem().machines.getSize());
        }
        for (XmlNode serviceContainerNode : serviceGroupNode.getChildren("sc"))
        {
            boolean processContainer = true;
            String scName = serviceContainerNode.getAttribute("name");

            // Determine the machine on which the container should run
            String machineName = serviceContainerNode.getAttribute("machine");
            Machine machine = null;
            if (!StringUtil.isEmptyOrNull(machineName))
            {
                // A name was set. Try to look it up.
                machine = machines.getByName(machineName);
                if (machine == null)
                {
                    // Could not find the machine by its name. So maybe its a number
                    try
                    {
                        int index = Integer.parseInt(machineName);
                        if (index > machines.getSize())
                        {
                            env.warn("Service container " + scName
                                    + " is configured to run on a node which is not present. Skipping the creation of this node");
                            processContainer = false;
                        }
                        else
                        {
                            machine = machines.get(index);
                        }
                    }
                    catch (Exception e)
                    {
                        env.warn("Could not find machine with identification '" + machineName + "' in the environment");
                    }
                }
            }

            // If no specific machine was found we'll use the iteration
            if (machine == null)
            {
                machine = machines.get(i++);
            }

            // Get the real machine name to create the container on.
            machineName = machine.getName();

            if (processContainer)
            {
                XmlNode configsNode = serviceContainerNode.getChild("bussoapprocessorconfiguration/configurations");
                ServiceContainer serviceContainer = serviceGroup.serviceContainers.getByName(scName);
                if (serviceContainer == null) // Create SC
                {
                    env.info("creating servicecontainer " + scName + " for machine " + machineName + " ... ");
                    boolean automatic = "true".equals(serviceContainerNode.getAttribute("automatic"));
                    serviceGroup.createServiceContainer(scName, machineName, automatic, configsNode.clone());
                    for (XmlNode subchild : serviceContainerNode.getChildren())
                    {
                        if (subchild.getName().equals("cp"))
                        {
                            ServiceContainer newSC = serviceGroup.serviceContainers.getByName(scName);

                            // Read the data from the XML
                            String type = subchild.getAttribute("type");
                            if (StringUtil.isEmptyOrNull(type))
                            {
                                type = "socket";
                            }
                            String cpName = subchild.getAttribute("name");
                            String description = subchild.getChildText("description");
                            String labeledURI = subchild.getChildText("labeleduri");

                            newSC.createConnectionPoint(cpName, type, machineName, description, labeledURI);
                        }
                    }
                    env.info("OK");
                }
                else
                // Update SC
                {
                    env.info("updating servicecontainer " + scName + " for machine " + machineName + " ... ");
                    boolean automatic = "true".equals(serviceContainerNode.getAttribute("automatic"));
                    serviceGroup.updateServiceContainer(scName, machineName, automatic, configsNode.clone(), serviceContainer);
                    env.info("OK");
                }
            }
        }
    }

    /**
     * @param org
     * @param serviceGroupNode
     * @return
     */
    private WebServiceInterface[] getWebServiceInterfaces(Organization org, XmlNode serviceGroupNode)
    {
        ArrayList<WebServiceInterface> result = new ArrayList<WebServiceInterface>();
        for (XmlNode child : serviceGroupNode.getChildren())
        {
            if ((child.getName().equals("wsi")))
            {
                WebServiceInterface newWSI = null;
                String packageName = child.getAttribute("package");
                if (StringUtil.isEmptyOrNull(packageName))
                {
                    // For backward compatibility
                    packageName = child.getAttribute("isvp");
                }

                String wsiName = child.getAttribute("name");
                if (packageName == null)
                {
                    newWSI = org.webServiceInterfaces.getByName(wsiName);
                }
                else
                {
                    Package isvp = org.getSystem().isvp.getByName(packageName);
                    if (isvp != null)
                        newWSI = isvp.webServiceInterfaces.getByName(wsiName);
                }
                if (newWSI != null)
                {
                    result.add(newWSI);
                }
                else
                {
                    env.error("Skipping unknown web service interface " + wsiName);
                }
            }
        }
        return result.toArray(new WebServiceInterface[result.size()]);
    }

    /**
     * Creates/updates user Also configures the user with the given roles
     * 
     * @param org Organization where the user needs to be created/updated
     * @param userNode XmlNode representing the user as per the template
     */
    private void processUser(Organization org, XmlNode userNode)
    {
        String name = userNode.getAttribute("name");
        if ("SYSTEM".equals(name.toUpperCase()))
        {
            /*
             * Whenever I had a SYSTEM user in my template, Cordys would crash pretty hard. It would not be possible to start the
             * monitor anymore. I had to use the CMC to remove the organization before the Monitor would start again.
             */
            env.error("Ignoring user " + name + " because the SYSTEM user should not be modified from a template");
            return;
        }
        if (org.users.getByName(name) == null) // Create User
        {
            env.info("creating user " + name + " ... ");
            org.createUser(name, userNode.getAttribute("au")); // Create Org User
            env.info("OK");
        }

        env.info("configuring user " + name + " with roles ... ");
        // Assigning roles
        User user = org.users.getByName(name);
        ArrayList<String> newRoles = new ArrayList<String>();
        for (XmlNode child : userNode.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role role = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                String dnRole = null;
                if (isvpName == null) // Assign organizational role if the isvp name is not mentioned
                {
                    role = org.roles.getByName(roleName);
                    dnRole = "cn=" + roleName + ",cn=organizational roles," + org.getDn();
                }
                else
                // Assign ISVP role
                {
                    Package isvp = org.getSystem().isvp.getByName(isvpName);
                    if (isvp != null)
                        role = isvp.roles.getByName(roleName);
                    dnRole = "cn=" + roleName + ",cn=" + isvpName + "," + org.getSystem().getDn();
                }
                if (role != null)
                    newRoles.add(role.getDn());
                else
                    newRoles.add(dnRole);
            }
            else
                env.warn("Unknown user subelement " + child.getPretty());
        }
        // Assign all the roles to the user at once
        if (newRoles != null && newRoles.size() > 0)
            user.roles.add(newRoles.toArray(new String[newRoles.size()]));
        env.info("OK");
    }

    /**
     * Creates/updates role. Configures its sub-roles as well
     * 
     * @param org Organization where the role needs to be created/updated
     * @param roleNode XmlNode representing the role as per the template
     */
    private void processRole(Organization org, XmlNode roleNode)
    {
        String name = roleNode.getAttribute("name");
        String type = roleNode.getAttribute("type");
        if (org.roles.getByName(name) == null)
        {
            env.info("creating role " + name + " ... ");
            org.createRole(name, type);
            env.info("OK");
        }
        env.info("configuring role " + name + " ... ");
        Role role = org.roles.getByName(name);
        for (XmlNode child : roleNode.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role subRole = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                String dnRole = null;
                if (isvpName == null)
                {
                    subRole = org.roles.getByName(roleName);
                    dnRole = "cn=" + roleName + ",cn=organizational roles," + org.getDn();
                }
                else
                {
                    Package isvp = org.getSystem().isvp.getByName(isvpName);
                    if (isvp != null)
                        subRole = isvp.roles.getByName(roleName);
                    else
                        dnRole = "cn=" + roleName + ",cn=" + isvpName + "," + org.getSystem().getDn();
                }
                if (subRole != null)
                    role.roles.add(subRole);
                else
                    role.roles.add(dnRole);
            }
            else
                env.warn("Unknown role subelement " + child.getPretty());
        }
        env.info("OK");
    }

    /**
     * @param org
     * @param conf
     */
    public void check(Organization org, Configuration conf)
    {
        check(org, conf.getProps());
    }

    /**
     * @param org
     * @param vars
     */
    public void check(Organization org, Map<String, String> vars)
    {
        XmlNode template = xml(vars);
        for (XmlNode node : template.getChildren())
        {
            if (node.getName().equals("servicegroup"))
                checkServiceGroup(org, node);
            else if (node.getName().equals("user"))
                checkUser(org, node);
            else if (node.getName().equals("role"))
                checkRole(org, node);
            else
                System.out.println("Unknown organization element " + node.getPretty());
        }
    }

    /**
     * @param org
     * @param node
     */
    private void checkServiceGroup(Organization org, XmlNode node)
    {
        String name = node.getAttribute("name");
        ServiceGroup serviceGroup = org.serviceGroups.getByName(name);
        if (serviceGroup == null)
        {
            env.error("Missing ServiceGroup " + name);
            return;
        }
        env.info("Checking configuration of ServiceGroup " + name);
        WebServiceInterface[] target = getWebServiceInterfaces(org, node);
        for (WebServiceInterface wsi : target)
        {
            if (!serviceGroup.webServiceInterfaces.contains(wsi))
                env.error("ServiceGroup " + serviceGroup + " does not contain WebServiceInterface " + wsi);
        }
        for (WebServiceInterface wsi : serviceGroup.webServiceInterfaces)
        {
            boolean found = false;
            for (WebServiceInterface wsi2 : target)
            {
                if (wsi.getDn().equals(wsi2.getDn()))
                    found = true;
            }
            if (!found)
                env.error("ServiceGroup " + serviceGroup + " contains WebServiceInterface " + wsi + " that is not in template");
        }
        for (XmlNode child : node.getChildren())
        {
            if (child.getName().equals("wsi"))
                continue;
            else if (child.getName().equals("sc"))
            {
                String scName = child.getAttribute("name");
                ServiceContainer serviceContainer = serviceGroup.serviceContainers.getByName(scName);
                if (serviceContainer == null)
                {
                    env.error("Missing ServiceContainer " + scName);
                    continue;
                }
                boolean automatic = "true".equals(child.getAttribute("automatic"));
                if (serviceContainer.automatic.getBool() != automatic)
                    env.error("  " + serviceContainer + " property automatic, template says " + automatic
                            + " while current value is " + serviceContainer.automatic.get());
                XmlNode config = child.getChild("bussoapprocessorconfiguration").getChildren().get(0);
                XmlNode configsc = serviceContainer.config.getXml();
                for (String msg : config.diff(configsc))
                    env.error(msg);
            }
            else if (child.getName().equals("bussoapnodeconfiguration"))
            {
                // do nothing
            }
            else
                env.error("Unknown servicegroup subelement " + child.getPretty());
        }
    }

    /**
     * @param org
     * @param node
     */
    private void checkUser(Organization org, XmlNode node)
    {
        String name = node.getAttribute("name");
        User user = org.users.getByName(name);
        if (user == null)
        {
            env.info("Unknown user " + name);
            return;
        }
        env.info("Checking roles of user " + name);
        for (XmlNode child : node.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role role = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                env.info("Checking role " + roleName);
                String dnRole = null;
                if (isvpName == null)
                {
                    role = org.roles.getByName(roleName);
                    dnRole = "cn=" + roleName + ",cn=organizational roles," + org.getDn();
                }
                else
                {
                    Package isvp = org.getSystem().isvp.getByName(isvpName);
                    if (isvp != null)
                        role = isvp.roles.getByName(roleName);
                    else
                        dnRole = "cn=" + roleName + ",cn=" + isvpName + "," + org.getSystem().getDn();
                }
                if (role == null)
                    env.error("User " + user + " should have unknown role " + dnRole);
                else if (!user.roles.contains(role))
                    env.error("User " + user + " does not have role " + role);
            }
            else
                env.error("Unknown user subelement " + child.getPretty());
        }
    }

    /**
     * @param org
     * @param node
     */
    private void checkRole(Organization org, XmlNode node)
    {
        String name = node.getAttribute("name");
        Role role = org.roles.getByName(name);
        if (role == null)
        {
            env.info("Unknowm role " + name);
            return;
        }
        env.info("Checking roles of role " + name);
        for (XmlNode child : node.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role subRole = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                env.info("  adding role " + roleName);

                String dnRole = null;
                if (isvpName == null)
                {
                    subRole = org.roles.getByName(roleName);
                    dnRole = "cn=" + roleName + ",cn=organizational roles," + org.getDn();
                }
                else
                {
                    Package isvp = org.getSystem().isvp.getByName(isvpName);
                    if (isvp != null)
                        subRole = isvp.roles.getByName(roleName);
                    else
                        dnRole = "cn=" + roleName + ",cn=" + isvpName + "," + org.getSystem().getDn();
                }
                if (subRole == null)
                    env.error("Role " + role + " should have unknown role " + dnRole);
                else if (!role.roles.contains(subRole))
                    env.error("Role " + role + " does not have role " + subRole);
            }
            else
                env.error("Unknown role subelement " + child.getPretty());
        }
    }
}