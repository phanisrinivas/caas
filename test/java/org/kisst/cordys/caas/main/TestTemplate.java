package org.kisst.cordys.caas.main;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.xml.bind.JAXB;

import org.kisst.caas._2_0.template.DSO;
import org.kisst.caas._2_0.template.Organization;
import org.kisst.caas._2_0.template.ServiceGroup;
import org.kisst.caas._2_0.template.User;
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
            String text = FileUtil.loadString("./test/local/test_include.ctf");
            
            Properties p = new Properties();
            FileUtil.load(p, "P:\\backend-cordys-configuration-nv\\properties\\acc1.properties");
            
            Map<String, String> m = new LinkedHashMap<String, String>();
            for (Object key : p.keySet())
            {
                String value = p.getProperty((String) key);
                m.put((String) key, value);
            }
            
            String result = StringUtil.substitute(text, m);
            
            System.out.println(result);
            
            //Now that we have the template with the proper values, we can parse it
            Organization org = JAXB.unmarshal(new ByteArrayInputStream(result.getBytes()), Organization.class);
            System.out.println("Org: " + org.getOrg());
            
            List<DSO> dsos = org.getDso();
            for (DSO dso : dsos)
            {
                System.out.println("DSO: " + dso.getName());
            }
            
            for (ServiceGroup sg : org.getServicegroup())
            {
                System.out.println("ServiceGroup: " + sg.getName());
            }
            
            for (User user : org.getUser())
            {
                System.out.println("User: " + user.getName());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
