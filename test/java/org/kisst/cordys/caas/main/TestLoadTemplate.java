package org.kisst.cordys.caas.main;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

import org.kisst.cordys.caas.template.Template;
import org.kisst.cordys.caas.util.FileUtil;

/**
 * Holds the Class TestLoadTemplate.
 */
public class TestLoadTemplate
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
            System.setProperty("caas.conf.location", "./test/local/caas.conf");
            
            Environment.debug = true;
            
            File src = new File("./test/local/test_include.ctf");
            String text = FileUtil.loadString(src);
            
            Properties p = new Properties();
            FileUtil.load(p, "P:\\backend-cordys-configuration-nv\\properties\\acc1.properties");
            
            Template t = new Template(text, null, src.getParentFile());
            
            Map<String, String> map = new LinkedHashMap<String, String>();
            for (final String name: p.stringPropertyNames())
            {
                map.put(name, p.getProperty(name));
            }
            
            String finalXml = t.getFinalTemplateXml(map);
            
            System.out.println(finalXml);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
