package org.kisst.cordys.caas.util;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * This class is able to replace the values in the XML with the parameter names.
 * </p>
 * <p>
 * It will look at all attribute values and text nodes to see if there is data that could be replaced. It will of course ignore
 * empty values.
 * </p>
 * 
 * @author pgussow
 */
public class XMLSubstitution
{
    /** Holds the XML for the substitution */
    private XmlNode m_xml;
    /** Holds the variables and their values. Note that we will be searching for the m_vars.getValue() */
    private Map<String, String> m_vars;
    /** Holds the regex patterns for each variable */
    private Map<String, Pattern> m_patterns = new LinkedHashMap<String, Pattern>();

    /**
     * Instantiates a new xML substitution.
     * 
     * @param xml The xml to substitute.
     * @param variables The variables
     */
    public XMLSubstitution(String xml, Map<String, String> variables)
    {
        m_xml = new XmlNode(xml);
        m_vars = variables;

        for (Entry<String, String> s : m_vars.entrySet())
        {
            if (!StringUtil.isEmptyOrNull(s.getValue()))
            {
                Pattern pattern = Pattern.compile("(" + Pattern.quote(s.getValue()) + ")");
                m_patterns.put(s.getKey(), pattern);
            }
        }
    }

    /**
     * This method does the real substitution. It will search the XML and find all instances of the parameter values.
     * 
     * @return
     */
    public XmlNode execute()
    {
        processNode(m_xml);
        
        return m_xml;
    }

    /**
     * This method will process the content of the given XML node.
     * 
     * @param node The node
     */
    private void processNode(XmlNode node)
    {
        String text = node.getText();

        if (!StringUtil.isEmptyOrNull(text))
        {
            text = replaceText(text);

            // Set the changed text to the node.
            node.setText(text);
        }

        // Now itterate of the attributes
        Map<String, String> attributes = node.getAttributes();
        for (Entry<String, String> e : attributes.entrySet())
        {
            String value = e.getValue();
            if (!StringUtil.isEmptyOrNull(value))
            {
                node.setAttribute(e.getKey(), replaceText(value));
            }
        }

        // Next step is to go through all the child elements.
        for (XmlNode child : node.getChildren())
        {
            processNode(child);
        }
    }

    /**
     * THis method will find a value and replace it with the key name like ${key}. As soon as 1 value was found it will stop
     * looking at the other values.
     * 
     * @param text The text to search.
     * @return The new text. If no variable values were foud it will return the same input text.
     */
    private String replaceText(String text)
    {
        for (Entry<String, String> entry : m_vars.entrySet())
        {
            if (!StringUtil.isEmptyOrNull(entry.getValue()) && m_patterns.containsKey(entry.getKey()))
            {
                Pattern p = m_patterns.get(entry.getKey());

                Matcher matcher = p.matcher(text);

                boolean found = false;
                StringBuffer buffer = new StringBuffer();
                while (matcher.find())
                {
                    found = true;

                    matcher.appendReplacement(buffer, "");
                    buffer.append("${").append(entry.getKey()).append("}");
                }
                matcher.appendTail(buffer);

                text = buffer.toString();

                if (found)
                {
                    // If a parameter was found, we'll exit the loop to avoid replacement within parameter names.
                    break;
                }
            }
        }
        return text;
    }
}
