package org.kisst.cordys.caas.main;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.kisst.cordys.caas.support.LoadedPropertyMap;
import org.kisst.cordys.caas.support.LoadedPropertyMap.LoadedProperty;
import org.kisst.cordys.caas.util.Constants;
import static org.junit.Assert.*;

/**
 * Holds the Class LoadedPropertyMapTest. This class tests the functionality of the
 * {@link Environment#loadSystemProperties(String, String)}
 */
public class LoadedPropertyMapTest
{
    /** Holds the environment */
    private Environment m_env;

    /**
     * This method sets the up.
     * 
     * @throws Exception The exception
     */
    @Before
    public void setUp() throws Exception
    {
        System.setProperty(Constants.CAAS_CONF_LOCATION, "test/propertyloading/conf/caas.conf");
        Environment.toTrace();

        m_env = Environment.get();
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testDevSystemOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("dev", "system");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(4, all.size());
        assertEquals("first.properties-dev-user", lp.get("set.in.all"));

        // The property values should support property-in-property values. The definition of nested.test is
        // nested-set.in.all_${set.in.all}. This means that it's value should be resolved after loading to
        // nested-set.in.all_first.properties
        
        assertEquals("Error in nested resolving of properties", "nested-set.in.all_first.properties-dev-user", lp.get("nested.test"));
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testTestSystemOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("test", "system");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(2, all.size());
        assertEquals("first.properties", lp.get("set.in.all"));
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testDevOtherOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("dev", "other");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(3, all.size());
        assertEquals("dev-system-level", lp.get("set.in.all"));
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testTestOtherOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("test", "other");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(1, all.size());
        assertEquals("test.properties", lp.get("set.in.all"));
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testDevMyorgOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("dev", "myorg");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(9, all.size());

        // The value of set.in.all is either onlyuser.properties or first-user.properties. Since we don't control the order in
        // which the files are processed both are valid from a test perspective. In real live you want to avoid the same property
        // being defined in multiple files on the same layer.
        assertEquals("The value of set.in.all is either onlyuser.properties or first-user.properties", true,
                "onlyuser.properties".equals(lp.get("set.in.all")) || "first-user.properties".equals(lp.get("set.in.all")));
    }

    /**
     * This method tests that the properties for the dev system and the organization system are loaded properly.
     */
    @Test
    public final void testTestMyorgOrg()
    {
        LoadedPropertyMap lp = m_env.loadSystemProperties("test", "myorg");

        Map<String, LoadedProperty> all = lp.getPropertySources("set.in.all");

        // Check the values
        assertEquals(3, all.size());

        // The value of set.in.all is either onlyuser.properties or first-user.properties. Since we don't control the order in
        // which the files are processed both are valid from a test perspective. In real live you want to avoid the same property
        // being defined in multiple files on the same layer.
        assertEquals("The value of set.in.all is either first.properties or second.properties", true,
                "first.properties".equals(lp.get("set.in.all")) || "second.properties".equals(lp.get("set.in.all")));
    }

}
