package org.kisst.cordys.caas;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

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
    private final LinkedHashMap<String, String> props = new LinkedHashMap<String, String>();

    /**
     * Instantiates a new configuration.
     * 
     * @param filename The filename
     */
    public Configuration(String filename)
    {
        Properties p = new Properties();
        FileUtil.load(p, filename);
        for (Object key : p.keySet())
            props.put((String) key, (String) p.get(key));
        // TODO: use properties from file? although this might create multiple CordysSystem objects pointing to same system
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
        props.put(key, value);
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
    @SuppressWarnings("unchecked")
    public Map<String, String> getProps()
    {
        return (Map<String, String>) props.clone();
    } // T
}
