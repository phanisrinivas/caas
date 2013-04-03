package org.kisst.cordys.caas.main;

import java.util.Map;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kisst.cordys.caas.util.Constants;
import static org.junit.Assert.*;

/**
 * Holds the Class Properties_Test.
 */
public class Properties_Test
{
    /**
     * One time set up.
     */
    @BeforeClass
    public static void oneTimeSetUp()
    {
        System.setProperty(Constants.CAAS_CONF_LOCATION, "test/java/org/kisst/cordys/caas/main/properties/caas.conf");
        Environment.debug = true;
    }

    /**
     * This method tests the loading of the properties.
     */
    @Test
    public void loadProperties()
    {
        Environment env = Environment.get();

        Properties p = env.getProperties();

        assertEquals(p.getProperty("system.dev.gateway.old"), "true");
        assertEquals(p.getProperty("system.dev.gateway.username"), "pgussow");
        assertEquals(p.getProperty("system.dev.gateway.class"), "SamlClientCaller");
        
        Map<String, String> sp = env.loadSystemProperties("dev", "org");
        
        assertEquals(sp.get("organization.name"), "dev-org-user");
        assertEquals(sp.get("dev.only"), "true");

    }
}
