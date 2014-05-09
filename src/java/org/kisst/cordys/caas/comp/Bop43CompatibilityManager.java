package org.kisst.cordys.caas.comp;

import static org.kisst.cordys.caas.main.Environment.warn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.DeployedPackageInfo;
import org.kisst.cordys.caas.EPackageStatus;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class Bop43CompatibilityManager.
 */
class Bop43CompatibilityManager extends Bop42CompatibilityManager
{
    /** Holds the name of the operation GetPackagesByStatus to get all the packages in the system. */
    public static final String GET_PACKAGES_BY_STATUS = "GetPackagesByStatus";

    /**
     * @see org.kisst.cordys.caas.comp.DefaultCompatibilityManager#getVersionDetails()
     */
    @Override
    public String getVersionDetails()
    {
        return "D1.003.xxxx";
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

        List<XmlNode> knownCAP = response.xpath("./*[local-name()='Packages']/*[local-name()='Package']");
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

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#deployCap(java.lang.String, double)
     */
    @Override
    public void deployCap(SoapCaller c, CordysSystem system, String name, double timeoutInMinutes)
    {
        // Add the timeout
        long timeout = Math.round(timeoutInMinutes * 60 * 1000);
        HashMap<String, String> p = new LinkedHashMap<String, String>();
        p.put("timeout", String.valueOf(timeout));

        Package pkg = system.packages.getByName(name);
        if (pkg.status != EPackageStatus.loaded || !StringUtil.isEmptyOrNull(pkg.newVersion))
        {
            // Now we need to get the version that we can deploy
            XmlNode request = new XmlNode(Constants.GET_ACTIONS, Constants.XMLNS_CAP);
            request.setAttribute("filter", "FORWARD");
            request.add("Packages").add("Package").setText(name);

            XmlNode response = c.call(request);

            XmlNode xmlVersion = response.xpathSingle(
                    "cap:Packages/cap:Package/cap:Actions/cap:Action/cap:Versions/cap:Version[last()]", Constants.NS);

            if ((xmlVersion == null) || StringUtil.isEmptyOrNull(xmlVersion.getText()))
            {
                throw new CaasRuntimeException("Could not find the URL for CAP " + name
                        + ". Cause could be that there is no Upgrade / Deploy operation for this package");
            }

            String version = xmlVersion.getText();

            // Now we need to verify that there are no cyclic dependencies. If so the deploy should fail.
            request = new XmlNode(Constants.GET_CLUSTER_IMPACT, Constants.XMLNS_CAP);
            XmlNode tmp = request.add("Packages");
            tmp.setAttribute("severity", "NONE");
            tmp.setAttribute("dependencies", "true");
            tmp.setAttribute("operation", "deploy");

            tmp = tmp.add("Package");
            tmp.setAttribute(version, version);
            tmp.setText(name);

            c.call(request, p);

            // Now we need to build up the URL that is to be used to deploy the actual package.
            String url = c.getUrlBase() + "system/wcp/capcontent/packages/com.cordys.web.cap.CAPGateway.wcp?capName=" + name
                    + "&capVersion=" + version;

            // Now create the request to deploy the package
            request = new XmlNode(Constants.DEPLOY_CAP, Constants.XMLNS_CAP);
            request.setAttribute("Timeout", String.valueOf(timeout));
            request.setAttribute("revertOnFailure", "false");

            request.add("url").setText(url);

            c.call(request, p);
        }
        else
        {
            warn("Cannot deploy package " + name + " because status is " + pkg.status + " and/or no new version is found");
        }
    }

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#undeployCap(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem, java.lang.String, java.lang.String, java.lang.Boolean, long)
     */
    @Override
    public void undeployCap(SoapCaller c, CordysSystem system, String name, String userInputs, Boolean deleteReferences,
            long timeoutInMinutes)
    {
        // First get the status of the package. Is it indeed a new one
        XmlNode request = new XmlNode(Constants.GET_PACKAGE_DETAILS, Constants.XMLNS_CAP);
        request.add("Packages").add("Package").setText(name);

        XmlNode response = c.call(request);

        XmlNode deploymentDetails = response.xpathSingle(
                "cap:Packages/cap:Package/cap:DeploymentDetails[cap:ClusterStatus='DEPLOYED']", Constants.NS);

        if (deploymentDetails == null)
        {
            throw new CaasRuntimeException("The packages " + name + "is not deployed");
        }

        long timeout = timeoutInMinutes * 60 * 1000;

        // Now create the request to deploy the package
        request = new XmlNode(Constants.UNDEPLOY_CAP, Constants.XMLNS_CAP);
        request.setAttribute("Timeout", String.valueOf(timeout));

        request.add("CAP").setText(name);

        XmlNode ui = request.add("UserInputs");

        if (!StringUtil.isEmptyOrNull(userInputs))
        {
            ui.add(new XmlNode(userInputs));
        }

        XmlNode dr = request.add("deletereference");

        if (deleteReferences != null)
        {
            dr.setText(deleteReferences.toString());
        }

        // Add the timeout
        HashMap<String, String> p = new LinkedHashMap<String, String>();
        p.put("timeout", String.valueOf(timeout));

        c.call(request, p);
    }

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#revertCap(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem, java.lang.String, long)
     */
    @Override
    public void revertCap(SoapCaller c, CordysSystem system, String name, long timeoutInMinutes)
    {
        // First we need to check that it is indeed incomplete. Also we need the URL of the package to call the
        // DeployCAP method.
        XmlNode request = new XmlNode(Constants.GET_PACKAGE_DETAILS, Constants.XMLNS_CAP);
        request.add("Packages").add("Package").setText(name);

        XmlNode response = c.call(request);

        XmlNode deploymentDetails = response.xpathSingle(
                "cap:Packages/cap:Package/cap:DeploymentDetails[cap:ClusterStatus='INCOMPLETE']", Constants.NS);

        if (deploymentDetails == null)
        {
            throw new CaasRuntimeException("The packages " + name + "is not in incomplete state");
        }

        // Now we need to get the URL of the package to undeploy
        String version = "ver" + deploymentDetails.getChildText("Version") + "build"
                + deploymentDetails.getChildText("BuildNumber");
        String url = c.getUrlBase() + "system/wcp/capcontent/packages/com.cordys.web.cap.CAPGateway.wcp?capName=" + name
                + "&capVersion=" + version;

        long timeout = timeoutInMinutes * 60 * 1000;

        // Now create the request to revert the deployment of the cap
        request = new XmlNode(Constants.DEPLOY_CAP, Constants.XMLNS_CAP);
        request.setAttribute("Timeout", String.valueOf(timeout));
        request.setAttribute("isRevert", "true");
        request.setAttribute("revertOnFailure", "false");

        request.add("url").setText(url);

        // Add the timeout
        HashMap<String, String> p = new LinkedHashMap<String, String>();
        p.put("timeout", String.valueOf(timeout));

        c.call(request, p);
    }

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#loadCAPInfo(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem, org.kisst.cordys.caas.Package)
     */
    @Override
    public IDeployedPackageInfo loadCAPInfo(SoapCaller c, CordysSystem system, Package p)
    {
        DeployedPackageInfo retVal = null;

        XmlNode request = new XmlNode(Constants.GET_PACKAGE_DETAILS, Constants.XMLNS_CAP);
        request.add("Packages").add("Package").setText(p.getName());

        XmlNode response = c.call(request);

        XmlNode deploymentDetails = response.xpathSingle(
                "cap:Packages/cap:Package/cap:DeploymentDetails[cap:ClusterStatus='DEPLOYED']", Constants.NS);

        if (deploymentDetails != null)
        {
            String vendor = deploymentDetails.getChildText("Vendor");
            String version = deploymentDetails.getChildText("Version");
            String buildNumber = deploymentDetails.getChildText("BuildNumber");

            retVal = new DeployedPackageInfo(p.getPackageDN(), null, vendor, version, buildNumber);
        }

        return retVal;
    }
}
