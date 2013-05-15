package org.kisst.cordys.caas.cm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class will check if the DSO is configured correctly.
 */
public class DsoObjective extends AbstractObjective
{
    /** Holds the XML of the data source itself. */
    private XmlNode m_component;
    /** Holds the name of the data source */
    private String m_name;
    /** Holds the type of the data source */
    private String m_type;
    /** Holds the description of the data source */
    private String m_description;
    /** Holds the organization that needs to be checked */
    private Organization m_org;

    /**
     * Instantiates a new dso object.
     * 
     * @param child the configuration XML for the datasource.
     */
    public DsoObjective(Organization org, XmlNode child)
    {
        super(org.getSystem());

        m_org = org;
        m_name = child.getAttribute("name");
        m_type = child.getAttribute("type");
        m_description = child.getAttribute("desc");

        XmlNode component = child.getChild("datasourceconfiguration/component");

        // Store the configuration of the component for comparison
        m_component = component.clone();
    }

    /**
     * @see org.kisst.cordys.caas.cm.AbstractObjective#myCheck(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    @Override
    protected void myCheck(Ui ui)
    {
        // All the values that we've read from the caaspm file still have the variable names. So in order to properly check them
        // we need to substitube all PM-values based on the template
        Properties props = m_org.getSystem().getProperties();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, String> variables = new HashMap<String, String>((Map) props);

        String type = StringUtil.substitute(m_type, variables);
        String name = StringUtil.substitute(m_name, variables);
        String description = StringUtil.substitute(m_description, variables);
        String username = StringUtil.substitute(m_component.getChildText("userName"), variables);
        String password = StringUtil.substitute(m_component.getChildText("password"), variables);
        String connectionString = StringUtil.substitute(m_component.getChildText("connectionString"), variables);
        String defaultDatabase = StringUtil.substitute(m_component.getChildText("defaultDatabase"), variables);

        StringBuilder sb = new StringBuilder(1024);
        status = OK;

        DsoType types = m_org.dsotypes.getByName(type);
        if (types != null)
        {
            Dso dso = types.dsos.getByName(name);
            if (dso != null)
            {
                if (!StringUtil.equals(dso.desc.get(), description))
                {
                    sb.append(
                            "DSO " + name + " description does not match. Current: " + dso.desc.get() + ". Should be: "
                                    + description).append("\n");
                    status = ERROR;
                }

                // Compare the actual DB configuration.
                if (!StringUtil.equals(username, dso.username.get()))
                {
                    sb.append(
                            "DSO " + name + " username does not match. Current: " + dso.username.get() + ". Should be: "
                                    + username).append("\n");
                    status = ERROR;
                }

                if (!StringUtil.equals(password, dso.password.get()))
                {
                    sb.append(
                            "DSO " + name + " password does not match. Current: " + dso.password.get() + ". Should be: "
                                    + password).append("\n");
                    status = ERROR;
                }

                if (!StringUtil.equals(connectionString, dso.connectionstring.get()))
                {
                    sb.append(
                            "DSO " + name + " connectionString does not match. Current: " + dso.connectionstring.get()
                                    + ". Should be: " + connectionString).append("\n");
                    status = ERROR;
                }

                if (!StringUtil.equals(defaultDatabase, dso.defaultdb.get()))
                {
                    sb.append(
                            "DSO " + name + " defaultdb does not match. Current: " + dso.defaultdb.get() + ". Should be: "
                                    + defaultDatabase).append("\n");
                    status = ERROR;
                }
            }
            else
            {
                sb.append("DSO " + name + " not found").append("\n");
                status = ERROR;
            }
        }
        else
        {
            sb.append("DSO " + name + " not found").append("\n");
            status = ERROR;
        }

        message = sb.toString();
    }

    /**
     * @see org.kisst.cordys.caas.cm.AbstractObjective#myConfigure(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    @Override
    protected void myConfigure(Ui ui)
    {
        String name = m_name;
        String desc = m_description;
        String type = m_type;
        XmlNode config = m_component.clone();
        DsoType dsotype = m_org.dsotypes.getByName(type);
        if (dsotype == null) // Holds true only once per dsotype of org
        {
            ui.info(this, "creating dsotype " + type + " ... ");
            m_org.createDsoType(type);
            ui.info(this, "OK");
            dsotype = m_org.dsotypes.getByName(type);
        }

        Dso dso = dsotype.dsos.getByName(name);
        if (dso == null) // Create DSO
        {
            ui.info(this, "creating dso " + name + " ... ");
            dsotype = m_org.dsotypes.getByName(type);
            dsotype.createDso(name, desc, config);
            ui.info(this, "OK");
        }
        else
        // Update DSO
        {
            ui.info(this, "updating dso " + name + " ... ");
            dsotype.updateDso(dso, config);
            ui.info(this, "OK");
        }
    }

    /**
     * @see org.kisst.cordys.caas.cm.AbstractObjective#myPurge(org.kisst.cordys.caas.cm.Objective.Ui)
     */
    @Override
    protected void myPurge(Ui ui)
    {
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getChildren()
     */
    @Override
    public List<Objective> getChildren()
    {
        return new ArrayList<Objective>();
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getStatus()
     */
    @Override
    public int getStatus()
    {
        return status;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getMessage()
     */
    @Override
    public String getMessage()
    {
        return message;
    }

    /**
     * @see org.kisst.cordys.caas.cm.Objective#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return m_org.getSystem();
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        String type = getClass().getSimpleName();
        return type + "(" + m_name + ")";
    }
}
