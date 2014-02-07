package org.kisst.cordys.caas.support;

import static org.kisst.cordys.caas.main.Environment.trace;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * <p>
 * This class is used to support the loading of the properties from several locations. Since CAAS supports some different
 * locations for property files it is very handy to known which property was loaded from what location and which one was
 * overridden.
 * </p>
 * <p>
 * The map interface acts as a wrapper to the actual values for certain properties. It works on the actual properties map.
 * </p>
 * 
 * @author pgussow
 */
public class LoadedPropertyMap implements Map<String, String>
{
    /** Holds the actual values of the properties to be used. */
    private TreeMap<String, String> m_actualHashMap = new TreeMap<String, String>();
    /**
     * Holds all the properties that have been read from various locations. The first index is the property name. The second index
     * is the location.
     */
    private LinkedHashMap<String, Map<String, LoadedProperty>> m_loadedProperties = new LinkedHashMap<String, Map<String, LoadedProperty>>();

    /**
     * This method puts the property details in the loaded property list. If the proeprty is already defined it will not be
     * overwritten.
     * 
     * @param property The property to add.
     * @return The string previously associated with this property name and source.
     */
    public LoadedProperty put(LoadedProperty property)
    {
        return put(property, false);
    }

    /**
     * This method puts the property details in the loaded property list.
     * 
     * @param property The property to add.
     * @param isActual Whether or not this value should be marked as the actual value.
     * @return The string previously associated with this property name and source.
     */
    public LoadedProperty put(LoadedProperty property, boolean isActual)
    {
        Map<String, LoadedProperty> allLocations = m_loadedProperties.get(property.getName());

        if (allLocations == null)
        {
            // New property all together
            allLocations = new LinkedHashMap<String, LoadedPropertyMap.LoadedProperty>();
            m_loadedProperties.put(property.getName(), allLocations);
        }

        // Note that oldValue will always be empty, because it does not make sense that the same proeprty is loaded twice from the
        // same location. It only happens if someone has defined the same property twice in the same file.
        LoadedProperty oldValue = allLocations.put(property.getSource(), property);

        if (isActual || !m_actualHashMap.containsKey(property.getName()))
        {
            // The value is the actual value, so update it.
            String oldActual = m_actualHashMap.put(property.getName(), property.getValue());

            if (Environment.trace)
            {
                Environment.trace("Property " + property.getName() + " value was "
                        + (StringUtil.isEmptyOrNull(oldActual) ? "not set" : oldActual) + ", but is now set to "
                        + property.getValue() + " from location " + property.getSource());
            }
        }
        else
        {
            trace("IGNORE: property " + property.getName() + " from file " + property.getSource()
                    + " because it is already defined");
        }

        return oldValue;
    }

    /**
     * This method gets the value for the given property. If it is not set the defaultValue is returned.
     * 
     * @param name The name of the proeprty.
     * @param defaultValue The default value to return if not set.
     * @return The value for the given property.
     */
    public String get(String name, String defaultValue)
    {
        String retVal = m_actualHashMap.get(name);
        if (StringUtil.isEmptyOrNull(retVal))
        {
            retVal = defaultValue;
        }

        return retVal;
    }

    /**
     * This method gets all the sources in which this property was given a value.
     * 
     * @param name The name of the property.
     * @return The property sources.
     */
    public Map<String, LoadedProperty> getPropertySources(String name)
    {
        return m_loadedProperties.get(name);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("Properties:\n");

        for (String name : m_actualHashMap.keySet())
        {
            sb.append("     Name: ").append(name).append("\n");
            sb.append("    Value: ").append(m_actualHashMap.get(name)).append("\n");
            sb.append("Locations: \n");

            Map<String, LoadedProperty> locations = m_loadedProperties.get(name);

            for (LoadedProperty lp : locations.values())
            {
                sb.append("- ").append(lp.getSource()).append(" with value '").append(lp.getValue()).append("'\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * @see java.util.Map#size()
     */
    @Override
    public int size()
    {
        return m_actualHashMap.size();
    }

    /**
     * @see java.util.Map#isEmpty()
     */
    @Override
    public boolean isEmpty()
    {
        return m_actualHashMap.isEmpty();
    }

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    @Override
    public boolean containsKey(Object key)
    {
        return m_actualHashMap.containsKey(key);
    }

    /**
     * @see java.util.Map#containsValue(java.lang.Object)
     */
    @Override
    public boolean containsValue(Object value)
    {
        return m_actualHashMap.containsValue(value);
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    @Override
    public String get(Object key)
    {
        return m_actualHashMap.get(key);
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    @Override
    public String put(String key, String value)
    {
        throw new RuntimeException("Cannot add properties via this way. Use the put(LoadedProperty) method");
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    @Override
    public String remove(Object key)
    {
        m_loadedProperties.remove(key);
        return m_actualHashMap.remove(key);
    }

    /**
     * @see java.util.Map#putAll(java.util.Map)
     */
    @Override
    public void putAll(Map<? extends String, ? extends String> m)
    {
        throw new RuntimeException("Cannot put properties via this way.");
    }

    /**
     * @see java.util.Map#clear()
     */
    @Override
    public void clear()
    {
        m_actualHashMap.clear();
        m_loadedProperties.clear();
    }

    /**
     * @see java.util.Map#keySet()
     */
    @Override
    public Set<String> keySet()
    {
        return m_actualHashMap.keySet();
    }

    /**
     * @see java.util.Map#values()
     */
    @Override
    public Collection<String> values()
    {
        return m_actualHashMap.values();
    }

    /**
     * @see java.util.Map#entrySet()
     */
    @Override
    public Set<java.util.Map.Entry<String, String>> entrySet()
    {
        return m_actualHashMap.entrySet();
    }

    /**
     * Holds the details of the loaded property.
     */
    public static class LoadedProperty
    {
        /** Holds the name of the property. */
        private String m_name;
        /** Holds the value. */
        private String m_value;
        /** Holds the source of the property. */
        private String m_source;

        /**
         * Instantiates a new loaded property.
         * 
         * @param name The name
         * @param value The value
         * @param source The source
         */
        public LoadedProperty(String name, String value, String source)
        {
            m_name = name;
            m_value = value;
            m_source = source;

            if (StringUtil.isEmptyOrNull(name))
            {
                throw new RuntimeException("Name cannot be null for the loaded property.");
            }

            if (StringUtil.isEmptyOrNull(source))
            {
                throw new RuntimeException("Source cannot be null for the loaded property.");
            }
        }

        /**
         * This method gets the source of the property.
         * 
         * @return The source of the property.
         */
        public String getSource()
        {
            return m_source;
        }

        /**
         * This method sets the source of the property.
         * 
         * @param source The source of the property.
         */
        public void setSource(String source)
        {
            m_source = source;
        }

        /**
         * This method gets the value.
         * 
         * @return The value.
         */
        public String getValue()
        {
            return m_value;
        }

        /**
         * This method sets the value.
         * 
         * @param value The value.
         */
        public void setValue(String value)
        {
            m_value = value;
        }

        /**
         * This method gets the name of the property.
         * 
         * @return The name of the property.
         */
        public String getName()
        {
            return m_name;
        }

        /**
         * This method sets the name of the property.
         * 
         * @param name The name of the property.
         */
        public void setName(String name)
        {
            m_name = name;
        }
    }
}
