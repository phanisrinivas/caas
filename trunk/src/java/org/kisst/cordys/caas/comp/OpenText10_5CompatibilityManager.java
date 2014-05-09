package org.kisst.cordys.caas.comp;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class is used to handle the OpenText 10.5 specific API changes. There are 2 major changes:
 * <ul>
 * <li>The XML structure of the GetPackageByStatus response has changed.</li>
 * <li>Organization level deployment of CAP packages has been introduced.</li>
 * </ul>
 * 
 * @author pgussow
 */
public class OpenText10_5CompatibilityManager extends Bop43CompatibilityManager
{
    /**
     * @see org.kisst.cordys.caas.comp.DefaultCompatibilityManager#getVersionDetails()
     */
    @Override
    public String getVersionDetails()
    {
        return "D1.004.xxxx";
    }

    /**
     * @see org.kisst.cordys.caas.comp.DefaultCompatibilityManager#getCAPPackages(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem, org.kisst.cordys.caas.PackageList)
     */
    @Override
    public List<Package> getCAPPackages(SoapCaller c, CordysSystem system, PackageList packageList)
    {
        Map<String, Package> retVal = new LinkedHashMap<String, Package>();

        // Tell the package list that this instance supports CAP packages.
        packageList.setSupportsCap(true);

        // Step 1: Now get all the CAPs that are known to the system
        XmlNode request = new XmlNode(GET_PACKAGES_BY_STATUS, "http://schemas.cordys.com/cap/1.0");
        XmlNode states = request.add("States");
        states.add("Status").setText("New");
        states.add("Status").setText("Deployed");
        states.add("Status").setText("Upgrade");
        states.add("Status").setText("Incomplete");
        states.add("Status").setText("Partial");

        XmlNode response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);

        // Step2: build up the request to get all the details for all the CAPs that have been found.
        request = new XmlNode("GetPackageDetails", "http://schemas.cordys.com/cap/1.0");
        XmlNode xmlPackages = request.add("Packages");

        List<XmlNode> knownCAP = response
                .xpath("./*[local-name()='Packages']/*[local-name()='PackageInfo']/*[local-name()='Name']");
        for (XmlNode node : knownCAP)
        {
            xmlPackages.add("Package").setText(node.getText());
        }

        // Step 3: Get the details for all known packages
        response = c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);

        // Now we can either add the new package or set the new version
        knownCAP = response.xpath("./*[local-name()='Packages']/*[local-name()='Package']");
        for (XmlNode cap : knownCAP)
        {
            Package p = new Package(system, cap);
            retVal.put(p.getName(), p);
        }

        return new ArrayList<Package>(retVal.values());
    }
}
