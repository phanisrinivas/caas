package org.kisst.cordys.caas.comp;

import static org.kisst.cordys.caas.main.Environment.debug;
import static org.kisst.cordys.caas.main.Environment.info;

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

            if (cordysVersion.startsWith("D1.003"))
            {
                retVal = new Bop43CompatibilityManager();
            }
        }

        info("For cordys version " + cordysVersion + " using compatibility manager " + retVal.getClass().getSimpleName());

        return retVal;
    }
}
