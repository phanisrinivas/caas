package org.kisst.cordys.caas.pm;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

public class XmlStoreObjective implements Objective
{
    /** Holds the environment to use */
    private static final Environment env = Environment.get();
    /** Holds the XML of the actual XML store object. */
    private XmlNode m_object;
    /** Holds the name the XML store object */
    private String m_name;
    /** Holds the key for the XML store object */
    private String m_key;
    /** Holds the version for the XML store object */
    private String m_version;

    /**
     * Instantiates a new dso object.
     * 
     * @param child the configuration XML for the datasource.
     */
    public XmlStoreObjective(XmlNode child)
    {
        m_name = child.getAttribute("name");
        m_key = child.getAttribute("key");
        m_version = child.getAttribute("version");

        // Store the configuration of the component for comparison
        m_object = child.getChild("*").clone();
    }

    /**
     * @see org.kisst.cordys.caas.pm.Objective#check(org.kisst.cordys.caas.Organization)
     */
    @Override
    public boolean check(Organization org)
    {
        // All the values that we've read from the caaspm file still have the variable names. So in order to properly check them
        // we need to substitube all PM-values based on the template
        Properties props = org.getSystem().getProperties();
        @SuppressWarnings({ "rawtypes", "unchecked" })
        Map<String, String> variables = new HashMap<String, String>((Map) props);

        // Reparse the XML with the parameters replaced
        XmlNode actual = new XmlNode(StringUtil.substitute(m_object.toString(), variables));

        String name = StringUtil.substitute(m_name, variables);
        String version = StringUtil.substitute(m_version, variables);
        String key = StringUtil.substitute(m_key, variables);

        boolean retVal = true;

        // Get the object from the XMl store
        XmlNode xmlInStore = org.getSystem().getXml(key, version, org.getName());

        if (!StringUtil.equals(xmlInStore.toString(), actual.toString()))
        {
            env.error("XmlStore object " + name + " does not match.");
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