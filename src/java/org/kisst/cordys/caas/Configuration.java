package org.kisst.cordys.caas;

import java.io.File;

import org.kisst.cordys.caas.support.LoadedPropertyMap;
import org.kisst.cordys.caas.support.LoadedPropertyMap.LoadedProperty;
import org.kisst.cordys.caas.template.Template;
import org.kisst.cordys.caas.util.FileUtil;

/**
 * This class wraps an individual configuration file.
 */
public class Configuration
{
    /** Holds the org. */
    private final Organization org;
    /** Holds the props. */
    private final LoadedPropertyMap props = new LoadedPropertyMap();

    /**
     * Instantiates a new configuration.
     * 
     * @param filename The filename
     */
    public Configuration(String filename)
    {
        FileUtil.loadProperties(props, new File(filename));

        org = Caas.getSystem(props.get("system")).organizations.get(props.get("org"));
    }

    /**
     * This method gets the organization.
     * 
     * @return The organization
     */
    public Organization getOrganization()
    {
        return org;
    }

    /**
     * This method gets the.
     * 
     * @param key The key
     * @return The string
     */
    public String get(String key)
    {
        return props.get(key);
    }

    /**
     * Put.
     * 
     * @param key The key
     * @param value The value
     */
    public void put(String key, String value)
    {
        props.put(new LoadedProperty(key, value, "dynamic"));
    }

    /**
     * Apply.
     * 
     * @param templ The templ
     */
    public void apply(Template templ)
    {
        templ.apply(org, props);
    }

    /**
     * This method gets the props.
     * 
     * @return The props
     */
    public LoadedPropertyMap getProps()
    {
        LoadedPropertyMap retVal = null;

        try
        {
            retVal = (LoadedPropertyMap) props.clone();
        }
        catch (CloneNotSupportedException e)
        {
            // Will not happen
        }

        return retVal;
    }
}
