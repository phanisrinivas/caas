package org.kisst.cordys.caas.template;

import static org.kisst.cordys.caas.main.Environment.debug;
import static org.kisst.cordys.caas.main.Environment.error;
import static org.kisst.cordys.caas.main.Environment.info;
import static org.kisst.cordys.caas.main.Environment.warn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXB;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Marshaller;

import org.kisst.caas._2_0.template.DSO;
import org.kisst.caas._2_0.template.DSOType;
import org.kisst.caas._2_0.template.DataSourceConfiguration;
import org.kisst.caas._2_0.template.ObjectFactory;
import org.kisst.caas._2_0.template.RoleType;
import org.kisst.caas._2_0.template.Tested;
import org.kisst.caas._2_0.template.Version;
import org.kisst.caas._2_0.template.Warning;
import org.kisst.caas._2_0.template.XMLStoreObjectOperation;
import org.kisst.caas._2_0.template.XMLStoreVersion;
import org.kisst.cordys.caas.Assignment;
import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.Configuration;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.XMLStoreObject;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.template.strategy.ICustomStrategy;
import org.kisst.cordys.caas.template.strategy.StrategyFactory;
import org.kisst.cordys.caas.util.DOMUtil;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XMLSubstitution;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class is used to create the template of a given organization.
 */
public class Template
{
    /** Holds the created template */
    private final org.kisst.caas._2_0.template.Organization organizationTemplate;
    /** Holds whether or not the template is empty. */
    private boolean empty = true;
    /** Holds the options that should be applied */
    private List<ETemplateOption> options;
    /** Holds the organization from which the template was created. */
    private Organization organization;

    /**
     * Instantiates a new template.
     * 
     * @param template The template XML.
     */
    public Template(String template)
    {
        this(template, null);
    }

    /**
     * Instantiates a new template.
     * 
     * @param template The template XML.
     * @param templateOptions The template options
     */
    public Template(String template, List<ETemplateOption> templateOptions)
    {
        organizationTemplate = parseTemplate(template);

        processTemplateOptions(templateOptions);
    }

    /**
     * Creates a new Template object.
     * 
     * @param org The organization for which the template should be created / applied
     * @param targetPackageName The name of the target package.
     */
    public Template(Organization org, String targetPackageName, List<ETemplateOption> templateOptions)
    {
        this(org, targetPackageName, null, null, templateOptions);
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
        this(org, targetPackageName, pkg, u, null);
    }

    /**
     * Extracts a template of the given organization
     * 
     * @param org Organization which template needs to be created
     * @param targetPackageName The name of the target package.
     * @param isvp The package for which the template should be generated.
     * @param userDn The user to generate the template for.
     */
    public Template(Organization org, String targetPackageName, Package pkg, User u, List<ETemplateOption> templateOptions)
    {
        this.organization = org;
        this.organizationTemplate = new org.kisst.caas._2_0.template.Organization();

        processTemplateOptions(templateOptions);

        long overallStartTime = System.currentTimeMillis();

        info("Exporting template for " + org.getName() + " organization");

        organizationTemplate.setOrg(org.getName());

        // First we export all the non-Cordys packages.
        if (options.contains(ETemplateOption.NON_CORDYS_PACKAGES))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + ETemplateOption.NON_CORDYS_PACKAGES.description() + "...");
            CordysSystem system = org.getSystem();
            for (Package p : system.packages)
            {
                IDeployedPackageInfo pi = p.getInfo();

                if (pi != null && !"Cordys".equals(pi.getVendor()))
                {
                    debug("Found non-standard package: " + pi.getPackageName());

                    org.kisst.caas._2_0.template.Package templatePackage = new org.kisst.caas._2_0.template.Package();
                    templatePackage.setName(pi.getPackageName());
                    Version templateVersion = new Version();
                    templateVersion.setVersion(pi.getFullVersion());
                    templateVersion.setTested(Tested.OK);

                    Warning w = new Warning();
                    w.setMessage("Package " + pi.getPackageName() + " should be loaded");
                    templateVersion.getWarning().add(w);

                    templatePackage.getVersion().add(templateVersion);
                }
            }
            info("Finished exporting " + ETemplateOption.NON_CORDYS_PACKAGES.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.NON_CORDYS_PACKAGES.description() + "...");
        }

        // Export the DSOs in the given organization
        if (options.contains(ETemplateOption.DSO))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + org.dsos.getSize() + " " + ETemplateOption.DSO.description() + "...");
            for (DsoType dsotype : org.dsotypes)
            {
                for (Dso dso : dsotype.dsos)
                {
                    DSO td = new DSO();
                    td.setName(dso.getName());
                    td.setDesc(dso.getProp("desc").toString());
                    td.setType(DSOType.valueOf(dsotype.getName().toUpperCase()));

                    DataSourceConfiguration dsc = new DataSourceConfiguration();
                    XmlNode configNode = dso.config.getXml();
                    dsc.setAny(DOMUtil.convert(configNode));
                    td.setDatasourceconfiguration(dsc);
                }
            }
            info("Finished exporting " + ETemplateOption.DSO.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.DSO.description() + "...");
        }

        // Export XML Store objects
        if (options.contains(ETemplateOption.XML_STORE_OBJECTS))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + org.xmlStoreObjects.getSize() + " " + ETemplateOption.XML_STORE_OBJECTS.description() + "...");
            for (XMLStoreObject xso : org.xmlStoreObjects)
            {
                org.kisst.caas._2_0.template.XMLStoreObject txso = new org.kisst.caas._2_0.template.XMLStoreObject();

                txso.setKey(xso.getKey());
                txso.setVersion(XMLStoreVersion.valueOf(xso.getVersion().toUpperCase()));
                txso.setName(xso.getName());
                txso.setAny(DOMUtil.convert(xso.getXML()));

                organizationTemplate.getXmlstoreobject().add(txso);
            }
            info("Finished exporting " + ETemplateOption.XML_STORE_OBJECTS.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.XML_STORE_OBJECTS.description() + "...");
        }

        // Exporting local roles
        if (options.contains(ETemplateOption.ROLES))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + org.roles.getSize() + " " + ETemplateOption.ROLES.description() + "...");

            exportRoles(org, targetPackageName, organizationTemplate);

            info("Finished exporting " + ETemplateOption.ROLES.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.ROLES.description() + "...");
        }

        // Exporting users in the organization
        if (options.contains(ETemplateOption.USERS))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + org.users.getSize() + " " + ETemplateOption.USERS.description() + "...");

            exportUsers(org, targetPackageName, organizationTemplate);

            info("Finished exporting " + ETemplateOption.USERS.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.USERS.description() + "...");
        }

        // Exporting service groups.
        if (options.contains(ETemplateOption.SERVICE_GROUPS))
        {
            long startTime = System.currentTimeMillis();
            info("Exporting " + org.serviceGroups.getSize() + " service groups ... ");

            exportServiceGroups(org, targetPackageName, organizationTemplate);

            info("Finished exporting " + ETemplateOption.SERVICE_GROUPS.description() + " in "
                    + ((System.currentTimeMillis() - startTime) / 1000) + " seconds ... ");
        }
        else
        {
            info("Skipping exporting " + ETemplateOption.SERVICE_GROUPS.description() + "...");
        }

        info("Finished exporting entire template in " + ((System.currentTimeMillis() - overallStartTime) / 1000)
                + " seconds ... ");
    }

    /**
     * Process template options.
     * 
     * @param templateOptions The template options
     */
    private void processTemplateOptions(List<ETemplateOption> templateOptions)
    {
        options = templateOptions;
        if (options == null)
        {
            options = new ArrayList<ETemplateOption>();
            options.add(ETemplateOption.ALL);
        }

        // Make the option checks easier: if ALL is added, then we add all template options so that our checks can be easier
        if (options.contains(ETemplateOption.ALL))
        {
            for (ETemplateOption o : ETemplateOption.values())
            {
                options.add(o);
            }
        }
    }

    /**
     * Export roles.
     * 
     * @param org The org
     * @param targetPackageName The target package name
     * @param result The result
     */
    private void exportRoles(Organization org, String targetPackageName,
            org.kisst.caas._2_0.template.Organization templateOrganization)
    {
        for (Role role : org.roles)
        {
            org.kisst.caas._2_0.template.Role tr = new org.kisst.caas._2_0.template.Role();
            tr.setName(role.getName());
            organizationTemplate.getRole().add(tr);

            if (role.type.get() != null)
            {
                tr.setType(RoleType.valueOf(role.type.get().toUpperCase()));
            }

            for (Role subRole : role.roles)
            {
                String packageName = null;
                if (subRole.getParent() instanceof Organization)
                {
                    if (subRole.getName().equals("everyoneIn" + org.getName()))
                        continue;
                    packageName = targetPackageName;
                }
                else
                {
                    packageName = subRole.getParent().getName();
                }

                org.kisst.caas._2_0.template.Role child = new org.kisst.caas._2_0.template.Role();
                tr.getRole().add(child);

                child.setName(subRole.getName());
                if (subRole.type.get() != null)
                {
                    child.setType(RoleType.valueOf(subRole.type.get().toUpperCase()));
                }

                if (packageName != null)
                {
                    child.setPackage(packageName);
                }
            }
        }
    }

    /**
     * This method parses the template XML to the object structure.
     * 
     * @param template The template XML string.
     */
    private org.kisst.caas._2_0.template.Organization parseTemplate(String template)
    {
        return JAXB.unmarshal(new ByteArrayInputStream(template.getBytes()), org.kisst.caas._2_0.template.Organization.class);
    }

    /**
     * This method gets the template xml from the current object.
     * 
     * @return The template xml
     */
    private String getTemplateXml()
    {
        StringWriter writer = new StringWriter();

        try
        {
            JAXBContext jc = JAXBContext.newInstance(organizationTemplate.getClass());
            Marshaller marshaller = jc.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            JAXBElement<org.kisst.caas._2_0.template.Organization> tmp = new ObjectFactory().createOrg(organizationTemplate);
            marshaller.marshal(tmp, writer);
        }
        catch (Exception e)
        {
            throw new CaasRuntimeException(e);
        }

        return writer.toString();
    }

    /**
     * Export service groups.
     * 
     * @param org The org
     * @param targetPackageName The target package name
     * @param result The result
     */
    private void exportServiceGroups(Organization org, String targetPackageName,
            org.kisst.caas._2_0.template.Organization templateOrganization)
    {
        for (ServiceGroup serviceGroup : org.serviceGroups)
        {
            org.kisst.caas._2_0.template.ServiceGroup sg = new org.kisst.caas._2_0.template.ServiceGroup();
            templateOrganization.getServicegroup().add(sg);

            ICustomStrategy strategy = StrategyFactory.create(sg, org, this);

            strategy.create(sg, serviceGroup);
        }
    }

    /**
     * Export users.
     * 
     * @param org The org
     * @param targetPackageName The target package name
     * @param result The result
     */
    private void exportUsers(Organization org, String targetPackageName,
            org.kisst.caas._2_0.template.Organization templateOrganization)
    {
        for (User user : org.users)
        {
            if ("SYSTEM".equals(user.getName().toUpperCase()))
                continue; // SYSTEM user should not be part of the template

            org.kisst.caas._2_0.template.User tu = new org.kisst.caas._2_0.template.User();
            templateOrganization.getUser().add(tu);

            tu.setName(user.getName());
            AuthenticatedUser authUser = user.au.getRef();
            tu.setAu(authUser.getName());

            if (authUser.authenticationtype != null && !StringUtil.isEmptyOrNull(authUser.authenticationtype.get()))
            {
                tu.setType(authUser.authenticationtype.get());
            }

            if (authUser.userPassword != null && !StringUtil.isEmptyOrNull(authUser.userPassword.get()))
            {
                tu.setPassword(authUser.userPassword.get());
            }

            // For the osIdentity we only support the first one.
            tu.setOsidentity(authUser.osidentity.getAt(0));

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
                {
                    isvpName = role.getParent().getName();
                }

                org.kisst.caas._2_0.template.Role child = new org.kisst.caas._2_0.template.Role();
                tu.getRole().add(child);

                child.setName(role.getName());
                if (role.type.get() != null)
                {
                    child.setType(RoleType.valueOf(role.type.get().toUpperCase()));
                }

                if (isvpName != null)
                {
                    child.setPackage(isvpName);
                }
            }

            // Export the assignments. The difficulty is to figure out which role the user plays in the assignment.
            for (Assignment<User> a : user.assignments)
            {
                org.kisst.caas._2_0.template.Assignment assignment = new org.kisst.caas._2_0.template.Assignment();
                tu.getAssignment().add(assignment);

                assignment.setTeam(a.team.getName());
                if (a.effectiveDate.get() != null)
                {
                    assignment.setEffectivedate(String.valueOf(a.effectiveDate.get().getTime()));
                }

                if (a.isPrincipal.get() == true)
                {
                    assignment.setPrincipal(true);
                }

                if (a.isLead.get() == true)
                {
                    assignment.setLead(true);
                }

                if (a.role != null)
                {
                    String packageName = null;

                    if (a.role.getParent() instanceof Organization)
                    {
                        if (a.role.getName().equals("everyoneIn" + org.getName()))
                        {
                            continue;
                        }
                        packageName = targetPackageName;
                    }
                    else
                    {
                        packageName = a.role.getParent().getName();
                    }

                    assignment.setRolename(a.role.getName());
                    if (a.role.type.get() != null)
                    {
                        assignment.setRoletype(a.role.type.get());
                    }
                    if (packageName != null)
                    {
                        assignment.setRolepackage(packageName);
                    }
                }
            }

        }
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
        // Add the organization name, system name and LDAP root to the map
        if (organization != null)
        {
            addDefaultVariables(organization, vars);
        }

        // Now we need to substitue values for variable names. But we cannot do simple character substitution, because it could
        // corrupt the XML. So what we're going to do is a more intelligent way.
        XMLSubstitution xs = new XMLSubstitution(getTemplateXml(), vars);
        String actualTemplate = xs.execute().getPretty();

        FileUtil.saveString(new File(filename), actualTemplate);

        info("Template successfully exported to " + filename);
    }

    /**
     * This method adds the default variables based on the given organization. There are 3 default parameters that are set:
     * <ul>
     * <li>sys.org.name - The Cordys organization name</li>
     * <li>sys.ldap.root- The root LDAP DN of the system that this template is connecting to</li>
     * <li>sys.name - The name of the system in the caas.conf</li>
     * <li>sys.user.name - The name of the user that was used to connect to this system</li>
     * 
     * @param org The organization to get the information from.
     * @param vars The variables list to add the default values to.
     */
    private void addDefaultVariables(Organization org, Map<String, String> vars)
    {
        vars.put("sys.org.name", org.getName());
        vars.put("sys.ldap.root", org.getSystem().getDn());
        vars.put("sys.name", org.getSystem().getName());
        vars.put("sys.user.name", org.getSystem().getConnectionUser());
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
        String str = getFinalTemplateXml(vars);
        return new XmlNode(str);
    }

    /**
     * This method returns the actual template XML based on the given variables.
     * 
     * @param vars The varible substitution for the variables.
     * @return The string containing the XML that should serve as a template.
     */
    public String getFinalTemplateXml(Map<String, String> vars)
    {
        String str = getTemplateXml();
        if (vars != null)
        {
            str = StringUtil.substitute(str, vars);
        }
        str = str.replace("${dollar}", "$");

        return str;
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
        // Add the default system properties used for mapping.
        addDefaultVariables(org, vars);

        info("Importing template to " + org.getName() + " organization");

        if (Environment.debug)
        {
            StringBuilder sb = new StringBuilder(1024);
            sb.append("Variables:\n");
            for (String name : vars.keySet())
            {
                sb.append(name).append(": ").append(vars.get(name)).append("\n");
            }
            debug(sb.toString());
        }

        // Parse the final template into the object structure to apply the template.
        String tmp = getFinalTemplateXml(vars);
        org.kisst.caas._2_0.template.Organization template = JAXB.unmarshal(new StringReader(tmp),
                org.kisst.caas._2_0.template.Organization.class);

        for (DSO dso : template.getDso())
        {
            processDso(org, dso);
        }

        for (org.kisst.caas._2_0.template.XMLStoreObject xso : template.getXmlstoreobject())
        {
            processXMLStoreObject(org, xso);
        }

        for (org.kisst.caas._2_0.template.Role r : template.getRole())
        {
            processRole(org, r);
        }

        for (org.kisst.caas._2_0.template.User u : template.getUser())
        {
            processUser(org, u);
        }

        for (org.kisst.caas._2_0.template.ServiceGroup sg : template.getServicegroup())
        {
            processServiceGroup(org, sg);
        }

        for (org.kisst.caas._2_0.template.Package p : template.getPackage())
        {
            processPackage(org, p);
        }

        info("Template successfully imported to " + org.getName() + " organization");
    }

    /**
     * This method processes the given package in the template. For now it will do nothing until automatic loading is supported.
     * 
     * @param org The organization to apply it to.
     * @param pkg The node defining the package.
     */
    private void processPackage(Organization org, org.kisst.caas._2_0.template.Package pkg)
    {
        if (options.contains(ETemplateOption.NON_CORDYS_PACKAGES))
        {
            warn("Automatic deployment of package " + pkg.getName() + " not supported.");
        }
        else
        {
            info("Skipping applying " + ETemplateOption.NON_CORDYS_PACKAGES.description());
        }
    }

    /**
     * Appends/overwrites the XMLStore
     * 
     * @param org Organization where the XMLStore object needs to be processed
     * @param xso XmlNode of the XMLStore object from the template
     */
    private void processXMLStoreObject(Organization org, org.kisst.caas._2_0.template.XMLStoreObject xso)
    {
        if (options.contains(ETemplateOption.XML_STORE_OBJECTS))
        {
            XMLStoreObjectOperation operationFlag = xso.getOperation();
            String key = xso.getKey();
            XMLStoreVersion version = xso.getVersion();
            String name = xso.getName();

            XmlNode newXml = DOMUtil.convert(xso.getAny());
            XMLStoreObject obj = new XMLStoreObject(key, version.value(), org);
            if (operationFlag == XMLStoreObjectOperation.OVERWRITE)
            {
                info("Overwriting " + name + " xmlstore object ... ");
                obj.overwriteXML(newXml.clone());
            }
            else if (operationFlag == XMLStoreObjectOperation.APPEND)
            {
                info("Appending " + name + " xmlstore object ... ");
                obj.appendXML(newXml.clone());
            }
            else
            {
                // By default overwrite the XMStore object
                info("Overwriting " + name + " xmlstore object ... ");
                obj.overwriteXML(newXml.clone());
            }
            info("OK");
        }
        else
        {
            info("Skipping applying " + ETemplateOption.XML_STORE_OBJECTS.description());
        }
    }

    /**
     * Creates/updates DSO. Also creates the DSO type if it does not exist.
     * 
     * @param org Organization where the DSO needs to be created/updated
     * @param tdso XmlNode representing the DSO as per the template
     */
    private void processDso(Organization org, DSO tdso)
    {
        if (options.contains(ETemplateOption.DSO))
        {
            String name = tdso.getName();
            String desc = tdso.getDesc();
            DSOType type = tdso.getType();
            XmlNode config = DOMUtil.convert(tdso.getDatasourceconfiguration().getAny());
            DsoType dsotype = org.dsotypes.getByName(type.value());
            if (dsotype == null) // Holds true only once per dsotype of org
            {
                info("creating dsotype " + type + " ... ");
                org.createDsoType(type.value());
                info("OK");
                dsotype = org.dsotypes.getByName(type.value());
            }
            Dso dso = dsotype.dsos.getByName(name);
            if (dso == null) // Create DSO
            {
                info("creating dso " + name + " ... ");
                dsotype = org.dsotypes.getByName(type.value());
                dsotype.createDso(name, desc, config);
                info("OK");
            }
            else
            // Update DSO
            {
                info("updating dso " + name + " ... ");
                dsotype.updateDso(dso, config);
                info("OK");
            }
        }
        else
        {
            info("Skipping applying " + ETemplateOption.DSO.description());
        }
    }

    /**
     * Creates/updates service group. Also configures the web service interfaces and creates/updates corresponding service
     * containers
     * 
     * @param org Organization where the service group needs to be created/updated
     * @param sg XmlNode representing the service group as per the template
     */
    private void processServiceGroup(Organization org, org.kisst.caas._2_0.template.ServiceGroup sg)
    {
        if (options.contains(ETemplateOption.SERVICE_GROUPS))
        {
            ICustomStrategy cs = StrategyFactory.create(sg, org, this);

            cs.apply();
        }
        else
        {
            info("Skipping applying " + ETemplateOption.SERVICE_GROUPS.description());
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
                    Package pkg = org.getSystem().packages.getByName(packageName);
                    if (pkg != null)
                    {
                        newWSI = pkg.webServiceInterfaces.getByName(wsiName);
                    }
                }
                if (newWSI != null)
                {
                    result.add(newWSI);
                }
                else
                {
                    error("Skipping unknown web service interface " + wsiName);
                }
            }
        }
        return result.toArray(new WebServiceInterface[result.size()]);
    }

    /**
     * Creates/updates user Also configures the user with the given roles
     * 
     * @param org Organization where the user needs to be created/updated
     * @param tu XmlNode representing the user as per the template
     */
    private void processUser(Organization org, org.kisst.caas._2_0.template.User tu)
    {
        if (options.contains(ETemplateOption.USERS))
        {
            String name = tu.getName();
            if ("SYSTEM".equals(name.toUpperCase()))
            {
                // Whenever I had a SYSTEM user in my template, Cordys would crash pretty hard. It would not be possible to start
                // the
                // monitor anymore. I had to use the CMC to remove the organization before the Monitor would start again.
                error("Ignoring user " + name + " because the SYSTEM user should not be modified from a template");
                return;
            }
            if (org.users.getByName(name) == null) // Create User
            {
                info("creating user " + name + " ... ");
                org.createUser(name, tu.getAu(), tu.getType(), tu.getOsidentity(), tu.getPassword()); // Create Org User
                info("OK");
            }

            info("configuring user " + name + " with roles ... ");
            // Assigning roles
            User user = org.users.getByName(name);
            ArrayList<String> newRoles = new ArrayList<String>();
            for (org.kisst.caas._2_0.template.Role child : tu.getRole())
            {
                Role role = null;
                String isvpName = child.getPackage();
                String roleName = child.getName();
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

            // TODO: Process assignments

            // Assign all the roles to the user at once
            if (newRoles != null && newRoles.size() > 0)
                user.roles.add(newRoles.toArray(new String[newRoles.size()]));
            info("OK");
        }
        else
        {
            info("Skipping applying " + ETemplateOption.USERS.description());
        }
    }

    /**
     * Creates/updates role. Configures its sub-roles as well
     * 
     * @param org Organization where the role needs to be created/updated
     * @param tr XmlNode representing the role as per the template
     */
    private void processRole(Organization org, org.kisst.caas._2_0.template.Role tr)
    {
        if (options.contains(ETemplateOption.ROLES))
        {
            String name = tr.getName();
            RoleType type = tr.getType();
            if (org.roles.getByName(name) == null)
            {
                info("creating role " + name + " ... ");
                org.createRole(name, type.value());
                info("OK");
            }
            info("configuring role " + name + " ... ");
            Role role = org.roles.getByName(name);
            for (org.kisst.caas._2_0.template.Role child : tr.getRole())
            {
                Role subRole = null;
                String isvpName = child.getPackage();
                String roleName = child.getName();
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
            info("OK");
        }
        else
        {
            info("Skipping applying " + ETemplateOption.ROLES.description());
        }
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
                Environment.warn("Unknown organization element " + node.getPretty());
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
            error("Missing ServiceGroup " + name);
            return;
        }
        info("Checking configuration of ServiceGroup " + name);
        WebServiceInterface[] target = getWebServiceInterfaces(org, node);
        for (WebServiceInterface wsi : target)
        {
            if (!serviceGroup.webServiceInterfaces.contains(wsi))
                error("ServiceGroup " + serviceGroup + " does not contain WebServiceInterface " + wsi);
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
                error("ServiceGroup " + serviceGroup + " contains WebServiceInterface " + wsi + " that is not in template");
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
                    error("Missing ServiceContainer " + scName);
                    continue;
                }
                boolean automatic = "true".equals(child.getAttribute("automatic"));
                if (serviceContainer.automatic.getBool() != automatic)
                    error("  " + serviceContainer + " property automatic, template says " + automatic
                            + " while current value is " + serviceContainer.automatic.get());
                XmlNode config = child.getChild("bussoapprocessorconfiguration").getChildren().get(0);
                XmlNode configsc = serviceContainer.config.getXml();
                for (String msg : config.diff(configsc))
                    error(msg);
            }
            else if (child.getName().equals("bussoapnodeconfiguration"))
            {
                // do nothing
            }
            else
                error("Unknown servicegroup subelement " + child.getPretty());
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
            info("Unknown user " + name);
            return;
        }
        info("Checking roles of user " + name);
        for (XmlNode child : node.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role role = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                info("Checking role " + roleName);
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
                    error("User " + user + " should have unknown role " + dnRole);
                else if (!user.roles.contains(role))
                    error("User " + user + " does not have role " + role);
            }
            else
                error("Unknown user subelement " + child.getPretty());
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
            info("Unknowm role " + name);
            return;
        }
        info("Checking roles of role " + name);
        for (XmlNode child : node.getChildren())
        {
            if (child.getName().equals("role"))
            {
                Role subRole = null;
                String isvpName = child.getAttribute("package");
                String roleName = child.getAttribute("name");
                info("  adding role " + roleName);

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
                    error("Role " + role + " should have unknown role " + dnRole);
                else if (!role.roles.contains(subRole))
                    error("Role " + role + " does not have role " + subRole);
            }
            else
                error("Unknown role subelement " + child.getPretty());
        }
    }

}
