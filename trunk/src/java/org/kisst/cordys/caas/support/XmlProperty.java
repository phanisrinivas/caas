package org.kisst.cordys.caas.support;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class XMLProperty.
 */
public class XmlProperty<T>
{
    /** Holds the xpath identifying the tag to expose as a property. */
    private String m_path;
    /** Holds teh xml data to operate on */
    private XmlNode m_data;
    /** Holds the node name to be created */
    private String m_nodeName;
    /** Holds the prefix to be used */
    private String m_prefix;
    /** Holds the namespace prefixes to be used */
    private String[][] m_ns;
    /** Holds the basepath to get the parent in case the node needs to be created. */
    private String m_basePath;
    /** Holds the data class to use */
    private Class<T> m_dataClass;

    /**
     * Instantiates a new xML property.
     * 
     * @param data The XML data to operate on.
     * @param path The xpath to execute on the unit data.
     */
    public XmlProperty(XmlNode data, String path, Class<T> dataClass)
    {
        this(data, path, null, null, Constants.NS, dataClass);
    }

    /**
     * Instantiates a new xML property.
     * 
     * @param data The XML data to operate on.
     * @param basePath The base xpath to execute to get the parent node
     * @param nodeName The node name of the node to get the real value for.
     * @param prefix The prefix The prefix to use in the XPath.
     * @param namespaces The namespace mappings.
     */
    public XmlProperty(XmlNode data, String basePath, String nodeName, String prefix, String[][] namespaces, Class<T> dataClass)
    {
        m_basePath = basePath;
        m_path = basePath;
        m_data = data;
        m_nodeName = nodeName;
        m_prefix = prefix;
        m_dataClass = dataClass;

        // If the nodename and prefix are set, we need to modify the base path
        if (!StringUtil.isEmptyOrNull(nodeName) && !StringUtil.isEmptyOrNull(prefix))
        {
            if (m_path.length() == 0)
            {
                m_path = ".";
            }

            m_path += "/" + m_prefix + ":" + m_nodeName;
        }

        m_ns = namespaces;
    }

    /**
     * This method gets the value for the given property.
     * 
     * @return The value for the XML property. null is returned when the path was not found in the XML.
     */
    @SuppressWarnings("unchecked")
    public T get()
    {
        T res = null;

        String value = _get();

        if (value != null)
        {
            // Now we need to convert the value
            if (m_dataClass == String.class)
            {
                res = (T) value;
            }
            else if (m_dataClass == Date.class)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                try
                {
                    if (!StringUtil.isEmptyOrNull(value))
                    {
                        res = (T) sdf.parse(value);
                    }
                }
                catch (ParseException e)
                {
                    throw new IllegalArgumentException(e);
                }
            }
            else if (m_dataClass == Boolean.class)
            {
                res = (T) new Boolean("true".equalsIgnoreCase(value));
            }
            else if (m_dataClass == Long.class)
            {
                res = (T) new Long(value);
            }
            else if (m_dataClass == Integer.class)
            {
                res = (T) new Integer(value);
            }
            else
            {
                throw new IllegalArgumentException("Unsupported type class: " + m_dataClass.getName());
            }
        }

        return res;
    }

    /**
     * This method gets the data as a String.
     * 
     * @return The string data
     */
    private String _get()
    {
        XmlNode node = m_data.xpathSingle(m_path, m_ns);
        if (node != null)
        {
            return node.getText();
        }

        return null;
    }

    /**
     * This method sets the value for the XML property. It will update the XML. If the tag does not exist, it tries to create the
     * tag.
     * 
     * @param value The value for the property.
     * @return The xML property name.
     */
    public XmlProperty<T> set(T value)
    {
        String strValue = null;

        if (value != null)
        {
            if (m_dataClass == String.class)
            {
                _set((String) value);
            }
            else if (m_dataClass == Date.class)
            {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                _set(sdf.format(value));
            }
            else if (m_dataClass == Boolean.class)
            {
                _set(String.valueOf(value));
            }
            else if (m_dataClass == Long.class)
            {
                _set(String.valueOf(value));
            }
            else if (m_dataClass == Integer.class)
            {
                _set(String.valueOf(value));
            }
            else
            {
                throw new IllegalArgumentException("Unsupported type class: " + m_dataClass.getName());
            }
        }

        return _set(strValue);
    }

    /**
     * This method sets the value for the XML property. It will update the XML. If the tag does not exist, it tries to create the
     * tag.
     * 
     * @param value The value for the property.
     * @return The xML property name.
     */
    private XmlProperty<T> _set(String value)
    {
        XmlNode node = m_data.xpathSingle(m_path, m_ns);
        if (node != null)
        {
            node.setText(value);
        }
        else
        {
            // Node does not exist. Lets try a single way of creating it.
            try
            {
                String nodeName = m_nodeName;
                String path = m_basePath;
                if (StringUtil.isEmptyOrNull(m_nodeName) || !StringUtil.isEmptyOrNull(m_prefix))
                {
                    path = m_path.substring(0, m_path.lastIndexOf("/"));
                    nodeName = m_path.substring(m_path.lastIndexOf(':') + 1);
                }

                node = m_data.xpathSingle(path, m_ns);
                if (node != null)
                {
                    node.add(nodeName).setText(value);
                }
            }
            catch (Exception e)
            {
                // Apparently it did not work, so forget about it.
            }
        }

        return this;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return "" + _get();
    }
}