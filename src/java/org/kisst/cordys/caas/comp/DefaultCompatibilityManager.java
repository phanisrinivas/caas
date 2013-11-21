package org.kisst.cordys.caas.comp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.DeployedPackageInfo;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.ExceptionUtil;
import org.kisst.cordys.caas.util.StringUtil;
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
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#getCAPPackages(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem, org.kisst.cordys.caas.PackageList)
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

        // Tell the package list whether or not CAP is supported.
        packageList.setSupportsCap(supportsCap);

        return new ArrayList<Package>(retVal.values());
    }

    /**
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#supportsCap(org.kisst.cordys.caas.soap.SoapCaller,
     *      org.kisst.cordys.caas.CordysSystem)
     */
    @Override
    public Boolean supportsCap(SoapCaller c, CordysSystem system)
    {
        XmlNode request = new XmlNode(Constants.GET_CAP_DETAILS, "http://schemas.cordys.com/cap/1.0");
        boolean retVal = true;
        try
        {
            c.call(request, PackageList.DEFAULT_PACKAGE_TIMEOUT);
        }
        catch (Exception e)
        {
            // Dirty: check if there CAPs are supported on this system:
            String tmp = ExceptionUtil.getStacktrace(e);
            if (tmp.indexOf("Could not find a soap node implementing") > -1
                    && tmp.indexOf("http://schemas.cordys.com/cap/1.0:GetDeployedCapSummary") > -1)
            {
                retVal = false;
            }
        }

        return retVal;
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

        XmlNode request = new XmlNode(Constants.GET_CAP_DEPLOYMENT_DETAILS, Constants.XMLNS_CAP);
        request.add("ApplicationName").setText(name);

        XmlNode response = c.call(request);

        XmlNode url = response
                .xpathSingle(
                        "cap:tuple/cap:old/cap:ApplicationPackage/cap:node/cap:Application[@operation='Deploy' or @operation='Upgrade' or @operation='Install']/cap:url",
                        Constants.NS);

        if ((url == null) || StringUtil.isEmptyOrNull(url.getText()))
        {
            throw new CaasRuntimeException("Could not find the URL for CAP " + name
                    + ". Cause could be that there is no Upgrade / Deploy operation for this package");
        }

        // Now create the request to deploy the package
        request = new XmlNode(Constants.DEPLOY_CAP, Constants.XMLNS_CAP);
        request.setAttribute("Timeout", String.valueOf(timeout));
        request.setAttribute("revertOnFailure", "false");

        request.add("url").setText(url.getText());

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
        XmlNode request = new XmlNode(Constants.GET_DEPLOYED_CAP_SUMMARY, Constants.XMLNS_CAP);
        request.setAttribute("isInComplete", "true");

        XmlNode response = c.call(request);

        XmlNode ap = response.xpathSingle("cap:tuple/cap:old/cap:ApplicationPackage[cap:ApplicationName='" + name + "']",
                Constants.NS);

        if (ap == null)
        {
            throw new CaasRuntimeException("The package " + name + " is not in an incomplete state.");
        }

        // Now we need to get the URL of the package to undeploy
        request = new XmlNode(Constants.GET_CAP_DEPLOYMENT_DETAILS, Constants.XMLNS_CAP);
        request.add("ApplicationName").setText(name);
        response = c.call(request);

        XmlNode url = response.xpathSingle(
                "cap:tuple/cap:old/cap:ApplicationPackage/cap:node/cap:Application[@operation='Deploy']/cap:url", Constants.NS);

        if ((url == null) || StringUtil.isEmptyOrNull(url.getText()))
        {
            throw new CaasRuntimeException("Could not find the URL for CAP " + name
                    + ". Cause could be that the package is not deployed");
        }

        long timeout = timeoutInMinutes * 60 * 1000;

        // Now create the request to revert the deployment of the cap
        request = new XmlNode(Constants.DEPLOY_CAP, Constants.XMLNS_CAP);
        request.setAttribute("Timeout", String.valueOf(timeout));
        request.setAttribute("isRevert", "true");
        request.setAttribute("revertOnFailure", "false");

        request.add("url").setText(url.getText());

        // Add the timeout
        HashMap<String, String> p = new LinkedHashMap<String, String>();
        p.put("timeout", String.valueOf(timeout));

        c.call(request, p);
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
        XmlNode request = new XmlNode(Constants.GET_CAP_DEPLOYMENT_DETAILS, Constants.XMLNS_CAP);
        request.add("ApplicationName").setText(name);

        XmlNode response = c.call(request);

        XmlNode url = response
                .xpathSingle(
                        "cap:tuple/cap:old/cap:ApplicationPackage/cap:node/cap:Application[@operation='Deployed' or @operation='Deploy']/cap:url",
                        Constants.NS);

        if ((url == null) || StringUtil.isEmptyOrNull(url.getText()))
        {
            throw new CaasRuntimeException("Could not find the URL for CAP " + name
                    + ". Cause could be that the package is not deployed");
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
     * @see org.kisst.cordys.caas.comp.ICompatibilityManager#loadCAPInfo(org.kisst.cordys.caas.soap.SoapCaller, org.kisst.cordys.caas.CordysSystem, org.kisst.cordys.caas.Package)
     */
    @Override
    public IDeployedPackageInfo loadCAPInfo(SoapCaller soapCaller, CordysSystem system, Package p)
    {
        DeployedPackageInfo retVal = null;
        
        XmlNode request = new XmlNode(Constants.GET_CAP_DETAILS, Constants.XMLNS_CAP);
        XmlNode cap = request.add("cap");
        cap.setText(p.getPackageDN());

        XmlNode response = system.call(request);

        XmlNode header = (XmlNode) response.get("tuple/old/ApplicationPackage/ApplicationDetails/Header");

        if (header != null)
        {
            String vendor = header.getChildText("Vendor");
            String version = header.getChildText("Version");
            String buildNumber = header.getChildText("BuildNumber");

            retVal =  new DeployedPackageInfo(p.getPackageDN(), null, vendor, version, buildNumber);
        }
        
        return retVal;
    }
}
