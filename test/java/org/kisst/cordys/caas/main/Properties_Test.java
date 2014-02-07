package org.kisst.cordys.caas.main;

import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;
import org.kisst.cordys.caas.support.LoadedPropertyMap;
import org.kisst.cordys.caas.util.Constants;

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

        LoadedPropertyMap p = env.getProperties();

        assertEquals(p.get("system.dev.gateway.old"), "true");
        assertEquals(p.get("system.dev.gateway.username"), "pgussow");
        assertEquals(p.get("system.dev.gateway.class"), "SamlClientCaller");

        LoadedPropertyMap sp = env.loadSystemProperties("dev", "org");

        assertEquals(sp.get("organization.name"), "dev-org-user");
        assertEquals(sp.get("dev.only"), "true");

    }
}
