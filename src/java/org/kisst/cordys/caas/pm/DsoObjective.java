package org.kisst.cordys.caas.pm;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class will check if the DSO is configured correctly.
 */
public class DsoObjective implements Objective
{
    /** Holds the environment to use */
    private static final Environment env = Environment.get();
    /** Holds the XML of the datasource itself. */
    private XmlNode m_component;
    /** Holds the name of the data source */
    private String m_name;
    /** Holds the type of the data source */
    private String m_type;
    /** Holds the description of the data source */
    private String m_description;

    /**
     * Instantiates a new dso object.
     * 
     * @param child the configuration XML for the datasource.
     */
    public DsoObjective(XmlNode child)
    {
        m_name = child.getAttribute("name");
        m_type = child.getAttribute("type");
        m_description = child.getAttribute("desc");

        XmlNode component = child.getChild("datasourceconfiguration/component");

        // Store the configuration of the component for comparison
        m_component = component.clone();
    }

    /**
     * @see org.kisst.cordys.caas.pm.Objective#check(org.kisst.cordys.caas.Organization)
     */
    @Override
    public boolean check(Organization org)
    {
        //All the values that we've read from the caaspm file still have the variable names. So in order to properly check them we need to substitube all PM-values based on the template
        Properties props = org.getSystem().getProperties();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String,String> variables = new HashMap<String, String>((Map) props);
        
        String type = StringUtil.substitute(m_type, variables);
        String name = StringUtil.substitute(m_name, variables);
        String description = StringUtil.substitute(m_description, variables);
        String username = StringUtil.substitute(m_component.getChildText("userName"), variables);
        String password = StringUtil.substitute(m_component.getChildText("password"), variables);
        String connectionString = StringUtil.substitute(m_component.getChildText("connectionString"), variables);
        String defaultDatabase = StringUtil.substitute(m_component.getChildText("defaultDatabase"), variables);
        
        
        boolean retVal = true;

        DsoType types = org.dsotypes.getByName(type);
        if (types != null)
        {
            Dso dso = types.dsos.getByName(name);
            if (dso != null)
            {
                if (!StringUtil.equals(dso.desc.get(), description))
                {
                    env.error("DSO " + name + " description does not match. Current: " + dso.desc.get() + ". Should be: "
                            + description);
                    retVal = false;
                }

                // Compare the actual DB configuration.
                if (!StringUtil.equals(username, dso.username.get()))
                {
                    env.error("DSO " + name + " username does not match. Current: " + dso.username.get() + ". Should be: "
                            + username);
                    retVal = false;
                }
                
                if (!StringUtil.equals(password, dso.password.get()))
                {
                    env.error("DSO " + name + " password does not match. Current: " + dso.password.get() + ". Should be: "
                            + password);
                    retVal = false;
                }
                
                if (!StringUtil.equals(connectionString, dso.connectionstring.get()))
                {
                    env.error("DSO " + name + " connectionString does not match. Current: " + dso.connectionstring.get() + ". Should be: "
                            + connectionString);
                    retVal = false;
                }
                
                if (!StringUtil.equals(defaultDatabase, dso.defaultdb.get()))
                {
                    env.error("DSO " + name + " defaultdb does not match. Current: " + dso.defaultdb.get() + ". Should be: "
                            + defaultDatabase);
                    retVal = false;
                }
            }
            else
            {
                env.error("DSO " + name + " not found");
                retVal = false;
            }
        }
        else
        {
            env.error("DSO " + name + " not found");
            retVal = false;
        }

        return retVal;
    }

    /**
     * @see org.kisst.cordys.caas.pm.Objective#configure(org.kisst.cordys.caas.Organization)
     */
    @Override
    public void configure(Organization org)
    {
    }

    /**
     * @see org.kisst.cordys.caas.pm.Objective#remove(org.kisst.cordys.caas.Organization)
     */
    @Override
    public void remove(Organization org)
    {
    }

}
