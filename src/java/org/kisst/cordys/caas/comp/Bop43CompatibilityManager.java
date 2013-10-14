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
 * Holds the Class Bop43CompatibilityManager.
 */
class Bop43CompatibilityManager extends DefaultCompatibilityManager
{
    /**
     * @see org.kisst.cordys.caas.comp.DefaultCompatibilityManager#getVersionDetails()
     */
    @Override
    public String getVersionDetails()
    {
        return "D1.003.xxxx";
    }

    @Override
    public List<Package> getCAPPackages(SoapCaller c, CordysSystem system)
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
            request = new XmlNode("GetPackagesByStatus", "http://schemas.cordys.com/cap/1.0");
            XmlNode states = request.add("States");
            states.add("Status").setText("New");
            states.add("Status").setText("Upgrade");
            states.add("Status").setText("Incomplete");
            states.add("Status").setText("Partial");

            response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);

            // Build up the request for getting the details
            request = new XmlNode("GetPackageDetails", "http://schemas.cordys.com/cap/1.0");
            XmlNode xmlPackages = request.add("Packages");

            caps = response.xpath("./*[local-name()='Packages']/*[local-name()='Package']");
            for (XmlNode node : caps)
            {
                xmlPackages.add("Package").setText(node.getText());
            }
            
            //Get the details for these packages
            response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);
            
            //Now we can either add the new package or set the new version 
            caps = response.xpath("./*[local-name()='Packages']/*[local-name()='Package']");
            for (XmlNode cap : caps)
            {
                Package p = new Package(system, cap);
                
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

        return new ArrayList<Package>(retVal.values());
    }
}
