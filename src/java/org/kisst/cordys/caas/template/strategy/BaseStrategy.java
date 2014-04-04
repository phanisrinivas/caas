package org.kisst.cordys.caas.template.strategy;

import static org.kisst.cordys.caas.main.Environment.error;
import static org.kisst.cordys.caas.main.Environment.info;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.kisst.caas._2_0.template.ConnectionPoint;
import org.kisst.caas._2_0.template.ConnectionPointType;
import org.kisst.caas._2_0.template.Parameter;
import org.kisst.caas._2_0.template.Parameters;
import org.kisst.caas._2_0.template.ServiceGroup;
import org.kisst.caas._2_0.template.WSI;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.template.Template;
import org.kisst.cordys.caas.util.DOMUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class is the baseclass for all strategies. It implements the {@link ICustomStrategy} interface and contains some generic
 * methods to parse the parameters and store the main information.
 * 
 * @author pgussow
 */
public abstract class BaseStrategy implements ICustomStrategy
{
    /** Holds the organization to which this strategy should be applied. */
    private Organization m_organization;
    /** Holds the template from which the service group comes. */
    private Template m_template;
    /** Holds the service group that should be created. */
    private ServiceGroup m_serviceGroup;
    /** Holds the strategy specific parameters and the values */
    private Map<String, String> m_parameters = new LinkedHashMap<String, String>();

    /**
     * Instantiates a new base strategy.
     */
    public BaseStrategy()
    {

    }

    /**
     * This method gets the service group that should be created.
     * 
     * @return The service group that should be created.
     */
    public ServiceGroup getServiceGroup()
    {
        return m_serviceGroup;
    }

    /**
     * This method sets the service group that should be created.
     * 
     * @param serviceGroup The service group that should be created.
     */
    public void setServiceGroup(ServiceGroup serviceGroup)
    {
        m_serviceGroup = serviceGroup;
    }

    /**
     * This method gets the template from which the service group comes.
     * 
     * @return The template from which the service group comes.
     */
    public Template getTemplate()
    {
        return m_template;
    }

    /**
     * This method sets the template from which the service group comes.
     * 
     * @param template The template from which the service group comes.
     */
    public void setTemplate(Template template)
    {
        m_template = template;
    }

    /**
     * This method gets the organization to which this strategy should be applied.
     * 
     * @return The organization to which this strategy should be applied.
     */
    public Organization getOrganization()
    {
        return m_organization;
    }

    /**
     * This method sets the organization to which this strategy should be applied.
     * 
     * @param organization The organization to which this strategy should be applied.
     */
    public void setOrganization(Organization organization)
    {
        m_organization = organization;
    }

    /**
     * @see org.kisst.cordys.caas.template.strategy.ICustomStrategy#initialize(org.kisst.caas._2_0.template.ServiceGroup,
     *      org.kisst.cordys.caas.Organization, org.kisst.caas._2_0.template.Parameters, org.kisst.cordys.caas.template.Template)
     */
    @Override
    public void initialize(ServiceGroup sg, Organization org, Parameters p, Template template)
    {
        m_organization = org;
        m_serviceGroup = sg;
        m_template = template;

        // Parse the parameter values into a map.
        if (p != null)
        {
            for (Parameter param : p.getParameter())
            {
                m_parameters.put(param.getName(), param.getValue());
            }
        }

        onInitialize();
    }

    /**
     * Adapter method
     */
    protected void onInitialize()
    {
    }

    /**
     * This method gets the parameter value.
     * 
     * @param name The name of the parameter
     * @return The parameter value.
     */
    protected String getParameter(String name)
    {
        return m_parameters.get(name);
    }

    /**
     * This method gets the integer parameter value.
     * 
     * @param name The name of the parameter
     * @return The parameter value.
     */
    protected int getIntParameter(String name)
    {
        String tmp = m_parameters.get(name);
        if (StringUtil.isEmptyOrNull(tmp))
        {
            return -1;
        }
        else
        {
            return Integer.parseInt(tmp);
        }
    }

    /**
     * This method creates the service group based on the template definition.
     * 
     * @param sg The definition of the service group.
     * @return The created service group in Cordys.
     */
    protected org.kisst.cordys.caas.ServiceGroup createServiceGroup(org.kisst.caas._2_0.template.ServiceGroup sg)
    {
        String name = sg.getName();
        org.kisst.cordys.caas.ServiceGroup serviceGroup = getOrganization().serviceGroups.getByName(name);
        XmlNode config = DOMUtil.convert(sg.getBussoapnodeconfiguration().getAny());

        WebServiceInterface[] wsis = getWebServiceInterfaces(getOrganization(), sg);

        if (serviceGroup == null) // Create SG
        {
            info("creating servicegroup " + name + " ... ");

            getOrganization().createServiceGroup(name, config, wsis);
            serviceGroup = getOrganization().serviceGroups.getByName(name);

            info("OK");
        }
        else
        {
            info("updating servicegroup " + name + " ... ");

            // Update the configuration of the service group
            serviceGroup.updateConfiguration(config);

            if ((wsis != null) && (wsis.length > 0))
            {
                serviceGroup.webServiceInterfaces.update(wsis);
                ArrayList<String> namespaces = new ArrayList<String>();
                for (WebServiceInterface webServiceInterface : wsis)
                {
                    for (String namespace : webServiceInterface.namespaces.get())
                    {
                        namespaces.add(namespace);
                    }
                }
                serviceGroup.namespaces.update(namespaces);
                info("OK");
            }
        }
        return serviceGroup;
    }

    /**
     * This method returns the web service interfaces that are to be applied to the service group.
     * 
     * @param org The organization in which the service group should be created.
     * @param sg The definition of the service group in the template.
     * @return The web service interfaces that are to be set.
     */
    protected WebServiceInterface[] getWebServiceInterfaces(org.kisst.cordys.caas.Organization org,
            org.kisst.caas._2_0.template.ServiceGroup sg)
    {
        ArrayList<WebServiceInterface> result = new ArrayList<WebServiceInterface>();

        for (WSI child : sg.getWsi())
        {
            WebServiceInterface newWSI = null;
            String packageName = child.getPackage();

            String wsiName = child.getName();
            if (StringUtil.isEmptyOrNull(packageName))
            {
                newWSI = getOrganization().webServiceInterfaces.getByName(wsiName);
            }
            else
            {
                Package pkg = getOrganization().getSystem().packages.getByName(packageName);
                if (pkg != null && pkg.isLoaded())
                {
                    if (pkg.webServiceInterfaces != null)
                    {
                        newWSI = pkg.webServiceInterfaces.getByName(wsiName);
                    }
                    else
                    {
                        error("Skipping web service interface " + wsiName + " because package " + packageName
                                + " has no web services");
                    }
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
        return result.toArray(new WebServiceInterface[result.size()]);
    }

    /**
     * This method will try to find the machine with the given name. The value can be (in the order of importance):
     * <ul>
     * <li>The logical name of the machine as defined in the property file</li>
     * <li>The actual name of the machine</li>
     * <li>The index of the system</li>
     * </ul>
     * If the name is undefined OR that the system with the given name is not found null is returned.
     * 
     * @param name The name of the machine to find.
     * @return The machine that is identified by the given name. null if no system was found.
     */
    protected Machine findMachine(String name)
    {
        Machine retVal = null;

        if (!StringUtil.isEmptyOrNull(name))
        {
            CordysSystem sys = m_organization.getSystem();

            Map<String, Machine> machines = sys.getMappedMachines();
            if (machines.containsKey(name))
            {
                retVal = machines.get(name);
            }

            if (retVal == null)
            {
                // No machine found yet, maybe the name contains the actual name.
                for (Machine m : machines.values())
                {
                    if (name.equals(m.getName()))
                    {
                        retVal = m;
                        break;
                    }
                }

                // Final option: maybe the name is an index
                if (retVal == null)
                {
                    try
                    {
                        Integer index = Integer.parseInt(name);
                        retVal = sys.machines.get(index);
                    }
                    catch (Exception e)
                    {
                        // Ignore it. The end user filled it with garbage.
                        Environment.warn(name + " does not resolve to a valid machine on system " + sys.getName());
                    }
                }
            }
        }

        return retVal;
    }

    /**
     * This method creates the service container on the given machine. If the service container already exists, the container is
     * overwritten with the given configuration.
     * 
     * @param serviceGroup The service group under which the container is to be created.
     * @param scName The name of the service container to update/create.
     * @param config The configuration of the container.
     * @param container The definition of the container in the template file.
     * @param machine The machine on which the container should run.
     */
    protected void createServiceContainer(org.kisst.cordys.caas.ServiceGroup serviceGroup, String scName, XmlNode config,
            org.kisst.caas._2_0.template.ServiceContainer container, Machine machine)
    {
        ServiceContainer serviceContainer = serviceGroup.serviceContainers.getByName(scName);
        if (serviceContainer == null) // Create SC
        {
            info("creating servicecontainer " + scName + " for machine " + machine.getName() + " ... ");

            serviceGroup.createServiceContainer(scName, machine.getName(), container.isAutomatic(), config.clone());

            for (ConnectionPoint cp : container.getCp())
            {
                ServiceContainer newSC = serviceGroup.serviceContainers.getByName(scName);

                // Read the data from the XML
                ConnectionPointType type = cp.getType();
                if (type == null)
                {
                    type = ConnectionPointType.SOCKET;
                }
                String cpName = cp.getName();
                String description = cp.getDescription();
                String labeledURI = cp.getLabeleduri();

                newSC.createConnectionPoint(cpName, type.value(), machine.getName(), description, labeledURI);
            }

            info("OK");
        }
        else
        // Update SC
        {
            info("updating servicecontainer " + scName + " for machine " + machine.getName() + " ... ");
            serviceGroup.updateServiceContainer(scName, machine.getName(), container.isAutomatic(), config.clone(),
                    serviceContainer);
            info("OK");
        }
    }
}
