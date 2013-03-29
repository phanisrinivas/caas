package org.kisst.cordys.caas.cm;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class XmlStoreObjective.
 */
public class XmlStoreObjective extends CompositeObjective
{
    /** Holds the key for the XML store object. */
    private String m_key;
    /** Holds the version for the XML store object. */
    private String m_version;
    /** Holds the organization to check with this objective. */
    private final Organization org;

    /**
     * Instantiates a new xml store objective.
     *
     * @param  org   The org
     * @param  node  The node
     */
    public XmlStoreObjective(Organization org, XmlNode node)
    {
        super("XmlStore(" + node.getAttribute("key") + ")");
        this.org = org;
        m_key = node.getAttribute("key");
        m_version = node.getAttribute("version");

        for (XmlNode child : node.getChildren())
        {
            if ("contains".equals(child.getName()))
            {
                entries.add(new TextObjective(child));
            }
            else
            {
                throw new RuntimeException("Unknown element in xmlstore section " + m_key + ":\n" + child.getPretty());
            }
        }
    }

    /**
     * @see  org.kisst.cordys.caas.cm.Objective#getSystem()
     */
    public CordysSystem getSystem()
    {
        return org.getSystem();
    }

    /**
     * Holds the Class TextObjective.
     */
    private class TextObjective extends AbstractObjective
    {
        /** Holds the xml content string. */
        private final String xmlContentString;

        /**
         * Instantiates a new text objective.
         *
         * @param  node  The node
         */
        public TextObjective(XmlNode node)
        {
            super(org.getSystem());
            this.xmlContentString = node.getAttribute("string");
        }

        /**
         * @see  java.lang.Object#toString()
         */
        @Override public String toString()
        {
            return "TextObjective(" + xmlContentString + ")";
        }

        /**
         * @see  org.kisst.cordys.caas.cm.AbstractObjective#myCheck(org.kisst.cordys.caas.cm.Objective.Ui)
         */
        @Override protected void myCheck(Ui ui)
        {
            // All the values that we've read from the caaspm file still have the variable names. So in order to
            // properly check them we need to substitube all PM-values based on the template
            Properties props = org.getSystem().getProperties();
            @SuppressWarnings({ "rawtypes", "unchecked" })
            Map<String, String> variables = new HashMap<String, String>((Map) props);

            // Reparse the XML with the parameters replaced
            String actual = StringUtil.substitute(xmlContentString, variables);

            String version = StringUtil.substitute(m_version, variables);
            String key = StringUtil.substitute(m_key, variables);

            // Get the object from the XMl store
            XmlNode xml = org.getSystem().getXml(key, version, org.getName());

            if (xml == null)
            {
                message = "required xmlstore key " + key + " is not available";
                ui.error(this, message);
                status = ERROR;
                return;
            }
            status = OK;

            // System.out.println(xml.getPretty());
            String text = xml.getText();
            // System.out.println(text);
            message = "";

            if (text.indexOf(actual) < 0)
            {
                status = ERROR;
                ui.error(this, "xmlstore " + key + " is missing text " + actual);
                message += "xmlstore " + key + " is missing text " + actual + "\n";
            }
        }

        
        /**
         * @see org.kisst.cordys.caas.cm.AbstractObjective#myConfigure(org.kisst.cordys.caas.cm.Objective.Ui)
         */
        @Override protected void myConfigure(Ui ui)
        { 
            /* do nothing, automatically editing not supported */
        }

        /**
         * @see org.kisst.cordys.caas.cm.AbstractObjective#myPurge(org.kisst.cordys.caas.cm.Objective.Ui)
         */
        @Override protected void myPurge(Ui ui)
        { 
            /* do nothing, automatically editing not supported */
        }
    }
}
