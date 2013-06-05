package org.kisst.cordys.caas.main;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * Holds the Class TestTemplate.
 */
public class TestTemplate
{
    /**
     * Main method.
     *
     * @param saArguments Commandline arguments.
     */
    public static void main(String[] saArguments)
    {
        try
        {
            String text = FileUtil.loadString("C:/development/workspaces/MainWorkspace/backend-cordys-configuration-nv/templates/nv_config.ctf");
            
            Properties p = new Properties();
            FileUtil.load(p, "C:/development/workspaces/MainWorkspace/backend-cordys-configuration-nv/properties/int.properties");
            
            Map<String, String> m = new LinkedHashMap<String, String>();
            for (Object key : p.keySet())
            {
                String value = p.getProperty((String) key);
                m.put((String) key, value);
            }
            
            String result = StringUtil.substitute(text, m);
            
            System.out.println(result);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
