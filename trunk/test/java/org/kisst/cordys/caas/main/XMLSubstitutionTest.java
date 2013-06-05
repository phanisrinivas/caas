package org.kisst.cordys.caas.main;

import java.util.Map;

import org.junit.Test;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.XMLSubstitution;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class XMLSubstitutionTest.
 */
public class XMLSubstitutionTest
{
    /**
     * Test template.
     */
    @Test
    public void testTemplate()
    {
        Map<String, String> m = FileUtil.loadMap("./test/java/org/kisst/cordys/caas/main/test.vars");
        XMLSubstitution xs = new XMLSubstitution(FileUtil.loadString("./test/java/org/kisst/cordys/caas/main/test.ctf"), m);
        
        XmlNode res = xs.execute();
        
        System.out.println(res.getPretty());
    }
}
