package org.kisst.cordys.caas.comp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.ExceptionUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class contains the default implementation of certain operations which are known to have changed in different versions.
 * This class contains all the default implementations. Thus the implementations that are applicable to most Cordys versions.
 * 
 * @author pgussow
 */
public class DefaultCompatibilityManager implements ICompatibilityManager
{
    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#getVersionDetails()
     */
    @Override
    public String getVersionDetails()
    {
        return "default";
    }

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#getCAPPackages(org.kisst.cordys.caas.soap.SoapCaller, org.kisst.cordys.caas.CordysSystem, org.kisst.cordys.caas.PackageList)
     */
    @Override
    public List<Package> getCAPPackages(SoapCaller c, CordysSystem system, PackageList packageList)
    {
        Map<String, Package> retVal = new LinkedHashMap<String, Package>();

        // Next step is to get the list of deployed CAP packages
        XmlNode request = new XmlNode(Constants.GET_DEPLOYED_CAP_SUMMARY, "http://schemas.cordys.com/cap/1.0");
        XmlNode response = null;
        boolean supportsCap = false;
        try
        {
            response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);

            supportsCap = true;
        }
        catch (Exception e)
        {
            // Dirty: check if there CAPs are supported on this system:
            String tmp = ExceptionUtil.getStacktrace(e);
            if (tmp.indexOf("Could not find a soap node implementing") > -1
                    && tmp.indexOf("http://schemas.cordys.com/cap/1.0:GetDeployedCapSummary") > -1)
            {
                supportsCap = false;
            }
            else
            {
                throw new CaasRuntimeException(e);
            }
        }

        if (supportsCap)
        {
            List<XmlNode> caps = response
                    .xpath("./*[local-name()='tuple']/*[local-name()='old']/*[local-name()='ApplicationPackage']");
            for (XmlNode node : caps)
            {
                Package p = new Package(system, node);
                retVal.put(p.getName(), p);
            }

            // Now get the CAPs that are new
            request = new XmlNode("GetNewCapSummary", "http://schemas.cordys.com/cap/1.0");
            response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);

            caps = response.xpath("./*[local-name()='tuple']/*[local-name()='old']/*[local-name()='ApplicationPackage']");
            for (XmlNode node : caps)
            {
                Package p = new Package(system, node);

                // Now it could be that this package is already loaded. If a cap version 1.0.1 is loaded and 1.0.2 is already
                // uploaded (but not deployed) then the package will also apear in the 'GetNewCapSummary'. So we need to validate
                // if the package is already there.

                // Important!! Since we're in the retrieveList we cannot use the getByName call. That is because then we can into
                // a recursive loop. We should use the _getByName method which does not trigger the retrieveList
                Package loadedPackage = retVal.get(p.getName());
                if (loadedPackage == null)
                {
                    retVal.put(p.getName(), p);
                }
                else
                {
                    loadedPackage.setNewVersion(p.getFullVersion());
                }
            }
        }

        //Tell the package list whether or not CAP is supported.
        packageList.setSupportsCap(supportsCap);
        
        return new ArrayList<Package>(retVal.values());
    }
}
