package org.kisst.cordys.caas.template.strategy;

import static org.kisst.cordys.caas.main.Environment.info;

import java.util.Map;
import java.util.Map.Entry;

import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.util.DOMUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This strategy will configure a service container on each and every node that is available in the cluster. The value of the
 * 'machine' attribute will be ignored. This strategy can have the following parameters:
 * <ul>
 * <li>containers.per.node: Indicates how many service containers should be created on each node. Note that if you put this value
 * to 2 and you run this on a single node machine then there will be 2 service containers on that single node.</li>
 * <li>naming.pattern: Holds the naming pattern that should be used for the containers. Default is '${NAME} ${SEQUENCE}' which
 * means it takes the name of the container as mentioned in the template file and it adds the sequence number of the node.
 * Example: the container is called BPM and you have 4 nodes they will be called 'BPM 1', 'BPM 2', 'BPM 3' and 'BPM 4'</li>
 * </ul>
 * 
 * @author pgussow
 */
public class CloningStrategy extends BaseStrategy
{
    /** Holds the name of the parameter that indicates how many service containers should be created per node. */
    private static final String CONTAINERS_PER_NODE = "containers.per.node";
    /** Holds the name of the parameter that indicates the naming pattern to use for the service containers. */
    private static final String NAMING_PATTERN = "naming.pattern";
    /** Holds the default naming pattern that is used. */
    private static final String DEFAULT_NAMING_PATTERN = "${NAME} ${SEQUENCE}";

    /**
     * Apply.
     * 
     * @see org.kisst.cordys.caas.template.strategy.ICustomStrategy#apply()
     */
    @Override
    public void apply()
    {
        org.kisst.caas._2_0.template.ServiceGroup sg = getServiceGroup();

        // Create the service group in Cordys.
        ServiceGroup serviceGroup = createServiceGroup(sg);

        // In case of the single strategy we will just create the service containers that are defined in the template. If a
        // machine name is specified it will be created on that specific node.
        for (org.kisst.caas._2_0.template.ServiceContainer container : sg.getSc())
        {
            String scName = container.getName();

            // Get the configuration of the service container.
            XmlNode configsNode = DOMUtil.convert(container.getBussoapprocessorconfiguration().getAny());

            // Determine the machine on which the container should run
            String machineName = container.getMachine();
            if (StringUtil.isEmptyOrNull(machineName))
            {
                int scCount = 1;
                info("No machine defined, so going to do cloning on all mapped systems.");
                Map<String, Machine> machines = getOrganization().getSystem().getMappedMachines();
                for (Entry<String, Machine> e : machines.entrySet())
                {
                    String logicalName = e.getKey();
                    Machine m = e.getValue();

                    info("Creating " + getContainersPerNode() + " containers on machine " + logicalName + "(real:" + m.getName()
                            + ")");
                    for (int count = 0; count < getContainersPerNode(); count++)
                    {
                        // Build up the final name
                        String finalName = getNamingPattern();
                        finalName = finalName.replaceAll("\\$\\{NAME\\}", scName);
                        finalName = finalName.replaceAll("\\$\\{SEQUENCE\\}", String.valueOf(scCount));

                        // Create / update the actual service container.
                        createServiceContainer(serviceGroup, finalName, configsNode, container, m);

                        scCount++;
                    }
                }
            }
            else
            {
                // Going to use the single strategy here, since an explicit system was mentioned.
                Machine machine = findMachine(machineName);
                if (machine == null)
                {
                    machine = getOrganization().getSystem().machines.get(0);
                }

                // Get the real machine name to create the container on.
                machineName = machine.getName();

                // Create / update the actual service container.
                createServiceContainer(serviceGroup, scName, configsNode, container, machine);
            }
        }
    }

    /**
     * @see org.kisst.cordys.caas.template.strategy.ICustomStrategy#create(org.kisst.caas._2_0.template.ServiceGroup,
     *      org.kisst.cordys.caas.ServiceGroup)
     */
    @Override
    public void create(org.kisst.caas._2_0.template.ServiceGroup template, ServiceGroup serviceGroup)
    {

    }

    /**
     * This method gets the number of containers per machine.
     * 
     * @return The number of containers per machine.
     */
    public int getContainersPerNode()
    {
        int retVal = getIntParameter(CONTAINERS_PER_NODE);
        if (retVal < 1)
        {
            retVal = 1;
        }

        return retVal;
    }

    /**
     * This method returns the naming pattern to use for this strategy.
     * 
     * @return The naming pattern to use.
     */
    public String getNamingPattern()
    {
        String retVal = getParameter(NAMING_PATTERN);

        if (StringUtil.isEmptyOrNull(retVal))
        {
            retVal = DEFAULT_NAMING_PATTERN;
        }

        return retVal;
    }
}
