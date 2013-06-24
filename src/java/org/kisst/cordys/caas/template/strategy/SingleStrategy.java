package org.kisst.cordys.caas.template.strategy;

import org.kisst.caas._2_0.template.BusSoapNodeConfiguration;
import org.kisst.caas._2_0.template.BusSoapProcessorConfiguration;
import org.kisst.caas._2_0.template.ConnectionPointType;
import org.kisst.caas._2_0.template.WSI;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.util.DOMUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class SingleStrategy. There will be only 1 service container created with the given name. If it is installed on a
 * cluster it will look at the machine name to create that service container on the given machine. The machine name is considered
 * to be a logical name (as mapped in the caas.conf). If no mapping is found it tries to see if there is a node that has the given
 * name. This is the default strategy.
 */
public class SingleStrategy extends BaseStrategy
{
    /**
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

            // Determine the machine on which the container should run
            String machineName = container.getMachine();
            Machine machine = null;
            if (!StringUtil.isEmptyOrNull(machineName))
            {
                machine = findMachine(machineName);
            }

            // If no specific machine was found we'll use the first machine
            if (machine == null)
            {
                machine = getOrganization().getSystem().machines.get(0);
            }

            // Now we have the machine to create it on.
            XmlNode configsNode = DOMUtil.convert(container.getBussoapprocessorconfiguration().getAny());

            // Create / update the actual service container.
            createServiceContainer(serviceGroup, scName, configsNode, container, machine);
        }
    }

    /**
     * @see org.kisst.cordys.caas.template.strategy.ICustomStrategy#create(org.kisst.caas._2_0.template.ServiceGroup,
     *      org.kisst.cordys.caas.ServiceGroup)
     */
    @Override
    public void create(org.kisst.caas._2_0.template.ServiceGroup template, ServiceGroup serviceGroup)
    {
        template.setName(serviceGroup.getName());

        XmlNode configNode = serviceGroup.config.getXml().clone();
        XmlNode keyStoreNode = configNode.getChild("soapnode_keystore");
        if (keyStoreNode != null)
            configNode.remove(keyStoreNode);

        // If no namespace is defined on the given XML node we need to explicitly set it to ""
        if (configNode.getNamespace() == null)
        {
            configNode.setNamespace("");
        }

        BusSoapNodeConfiguration bsnc = new BusSoapNodeConfiguration();
        bsnc.setAny(DOMUtil.convert(configNode));
        template.setBussoapnodeconfiguration(bsnc);

        for (WebServiceInterface wsi : serviceGroup.webServiceInterfaces)
        {
            WSI twsi = new WSI();
            template.getWsi().add(twsi);

            twsi.setName(wsi.getName());

            if (!(wsi.getParent() instanceof org.kisst.cordys.caas.Organization))
            {
                twsi.setPackage(wsi.getParent().getName());
            }
        }

        for (ServiceContainer serviceContainer : serviceGroup.serviceContainers)
        {
            org.kisst.caas._2_0.template.ServiceContainer child = new org.kisst.caas._2_0.template.ServiceContainer();
            template.getSc().add(child);

            child.setName(serviceContainer.getName());
            child.setAutomatic(serviceContainer.automatic.getBool());

            BusSoapProcessorConfiguration bspc = new BusSoapProcessorConfiguration();
            bspc.setAny(DOMUtil.convert(serviceContainer.config.getXml()));
            child.setBussoapprocessorconfiguration(bspc);

            for (ConnectionPoint cp : serviceContainer.connectionPoints)
            {
                org.kisst.caas._2_0.template.ConnectionPoint tcp = new org.kisst.caas._2_0.template.ConnectionPoint();
                child.getCp().add(tcp);
                tcp.setName(cp.getName());

                // Check the type
                String labeledURI = cp.uri.get();
                String description = cp.desc.get();

                if (labeledURI.startsWith("socket"))
                {
                    tcp.setType(ConnectionPointType.SOCKET);
                    // No need to add the child nodes for the socket
                }
                else if (labeledURI.startsWith("msmq"))
                {
                    tcp.setType(ConnectionPointType.MSMQ);
                    tcp.setLabeleduri(labeledURI);
                    tcp.setDescription(description);
                }
                else if (labeledURI.startsWith("jms://"))
                {
                    tcp.setType(ConnectionPointType.JMS);

                    tcp.setLabeleduri(labeledURI);
                    tcp.setDescription(description);
                }
                else
                {
                    tcp.setType(ConnectionPointType.OTHER);

                    tcp.setLabeleduri(labeledURI);
                    tcp.setDescription(description);
                }
            }
        }
    }
}
