package org.kisst.cordys.caas.comp;

import static org.kisst.cordys.caas.main.Environment.debug;
import static org.kisst.cordys.caas.main.Environment.info;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kisst.cordys.caas.util.StringUtil;

/**
 * <p>
 * This class will return the compatibility manager that is needed for a specific version of Cordys.
 * </p>
 * <p>
 * The goal of the compatibility manager is to provide an abstraction layer in case Cordys decides to change SOAP APIs or other
 * stuff in the platform. Minor changes can be handled here, but depending on the change it might be needed to make a new version
 * of CAAS.
 * </p>
 * 
 * @author pgussow
 */
public class CompatibilityManagerFactory
{
    /**
     * This method will determine based on the Cordys version which compatibility manager should be used.
     * 
     * @param cordysVersion The version of the Cordys installation.
     * @return The compatibility manager to use for the specific Cordys version
     */
    public static ICompatibilityManager create(String cordysVersion)
    {
        ICompatibilityManager retVal = new DefaultCompatibilityManager();

        if (!StringUtil.isEmptyOrNull(cordysVersion))
        {
            debug("Cordys version: " + cordysVersion);

            Pattern p = Pattern.compile("([^.]+)\\.(\\d+)(\\.(\\d+)){0,1}");
            Matcher m = p.matcher(cordysVersion);
            if (m.find())
            {
                String release = m.group(1);
                int major = Integer.parseInt(m.group(2));

                if ("D1".equals(release) && major == 2)
                {
                    retVal = new Bop42CompatibilityManager();
                }
                else if ("D1".equals(release) && major == 3)
                {
                    retVal = new Bop43CompatibilityManager();
                }
                else if ("D1".equals(release) && major >= 4)
                {
                    retVal = new OpenText10_5CompatibilityManager();
                }
                else if ("C3".equals(release))
                {
                    retVal= new C3DefaultCompatibilityManager();
                }
            }
        }

        info("For cordys version " + cordysVersion + " using compatibility manager " + retVal.getClass().getSimpleName());

        return retVal;
    }
}
