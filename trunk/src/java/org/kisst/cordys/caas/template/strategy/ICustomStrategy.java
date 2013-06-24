package org.kisst.cordys.caas.template.strategy;

import org.kisst.caas._2_0.template.Parameters;
import org.kisst.caas._2_0.template.ServiceGroup;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.template.Template;

/**
 * Holds the Interface ICustomStrategy. This interface describes the strategy that is responsible for creating the service
 * containers in Cordys.
 * 
 * @author pgussow
 */
public interface ICustomStrategy
{
    /**
     * This method initializes the strategy with the context.
     * 
     * @param sg The definition of the service group that should be created.
     * @param org The organization in which the service group should be created.
     * @param p The strategy-specific parameter values.
     * @param template The template that is being created/applied.
     */
    void initialize(ServiceGroup sg, Organization org, Parameters p, Template template);

    /**
     * This method creates the template based on the given servicegroup.
     * 
     * @param template The service group object to hold the configuration.
     * @param serviceGroup The actual service group that should be written to the template.
     */
    void create(ServiceGroup template, org.kisst.cordys.caas.ServiceGroup serviceGroup);

    /**
     * This method is called when a template is applied to the organization. This method is responsible for creating the service
     * group, 1 or more service containers and connection points in Cordys.
     */
    void apply();

}
