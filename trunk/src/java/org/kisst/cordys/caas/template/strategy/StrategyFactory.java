package org.kisst.cordys.caas.template.strategy;

import org.kisst.caas._2_0.template.ObjectFactory;
import org.kisst.caas._2_0.template.Parameters;
import org.kisst.caas._2_0.template.ServiceContainerStrategy;
import org.kisst.caas._2_0.template.ServiceGroup;
import org.kisst.caas._2_0.template.Strategy;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.template.Template;

/**
 * A factory for creating Strategy objects.
 */
public class StrategyFactory
{
    
    /**
     * This method creates the strategy handler class based on the the given type.
     * 
     * @param sg The sg
     * @param org The org
     * @param template The template that is being created / applied.
     * @return The strategy handler to use.
     */
    public static ICustomStrategy create(ServiceGroup sg, Organization org, Template template)
    {
        ICustomStrategy retVal = null;
        
        
        //Initialize the default strategy and get the parameters
        ServiceContainerStrategy scs = ServiceContainerStrategy.SINGLE;
        Strategy s = sg.getStrategy();
        Parameters p = null;
        if (s != null)
        {
            scs = s.getType();
            p = s.getParameters();
        }
        else
        {
            p = new ObjectFactory().createParameters();
        }
        
        switch (scs)
        {
            case SINGLE:
                retVal = new SingleStrategy();
                break;
            case CLONING:
                retVal = new CloningStrategy();
                break;
            case CUSTOM:
                retVal = createCustom(s);
                break;
            default:
                break;
        }
        
        //Initialize the strategy
        retVal.initialize(sg, org, p, template);
        
        return retVal;
    }

    /**
     * This method creates the instance of the custom strategy based on the definition.
     * 
     * @param s The strategy definition.
     * @return The created strategy.
     */
    private static ICustomStrategy createCustom(Strategy s)
    {
        ICustomStrategy retVal = null;
        try
        {
            Class<?> clazz = Class.forName(s.getClazz());
            if (!ICustomStrategy.class.isAssignableFrom(clazz))
            {
                throw new CaasRuntimeException("Class " + clazz.getName() + " does not implement interface " + ICustomStrategy.class.getName());
            }
            
            retVal = (ICustomStrategy) clazz.newInstance();
        }
        catch(Exception e)
        {
            throw new CaasRuntimeException(e);
        }
        
        return retVal;
    }
}
