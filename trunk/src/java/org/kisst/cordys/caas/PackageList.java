package org.kisst.cordys.caas;

import java.util.HashMap;
import java.util.List;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.soap.SamlClientCaller;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.ExceptionUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class maintains the list of package definitions that are available on the Cordys system. It will use a combination of
 * several web services to determine which definitions are available. Also there is a difference between a package definition and
 * a loaded package. This list contains the packages which are not necesarily loaded.
 */
public class PackageList extends CordysObjectList<Package>
{
    /** Holds whether or not the system supports CAP packages. */
    private boolean supportsCap;

    /**
     * Instantiates a new package definition list.
     * 
     * @param system The system
     */
    PackageList(CordysSystem system)
    {
        super(system);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
     */
    @Override
    protected void retrieveList()
    {
        // First we need to retrieve the ISV packages that are available on the system. For that we need to call the
        // http://server/cordys/com.eibus.web.application.ListISVPackages.wcp?isvpackage
        SoapCaller c = system.getSoapCaller();

        XmlNode request = new XmlNode("GetInstalledISVPackages", "http://schemas.cordys.com/1.0/isvpackage");
        request.add("computer").setText(system.machines.get(0).getName());

        XmlNode response = c.call(request);

        // There could be packages that have no file name. Those packages are loaded, but we only have the information in the
        // GetInstalledISVPackages. So let's try to also add the packages of which we know they are there, but have no isvp file.
        List<XmlNode> platformPackages = response.xpath(".//isv:computer/isv:isvp[@name='']", Constants.NS);
        if (platformPackages != null)
        {
            for (XmlNode platformPackage : platformPackages)
            {
                Package p = new Package(getSystem(), platformPackage);
                grow(p);
            }
        }

        // Next step is to get the definition of the packages for which filenames are specified.
        request = buildGetISVPackageDefinition(response);
        if (request != null)
        {
            // There are 1 or more ISV packages available.
            response = c.call(request);

            // Now we have all the definitions. This means we can create the package definitions
            List<XmlNode> isvs = response.getChildren("ISVPackage");
            for (XmlNode node : isvs)
            {
                Package p = new Package(getSystem(), node);
                grow(p);
            }
        }

        // Next step is to get the list of deployed CAP packages
        request = new XmlNode(Constants.GET_DEPLOYED_CAP_SUMMARY, "http://schemas.cordys.com/cap/1.0");
        try
        {
            response = c.call(request);

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
                Package p = new Package(getSystem(), node);
                grow(p);
            }

            // Now get the CAPs that are new
            request = new XmlNode("GetNewCapSummary", "http://schemas.cordys.com/cap/1.0");
            response = c.call(request);

            caps = response.xpath("./*[local-name()='tuple']/*[local-name()='old']/*[local-name()='ApplicationPackage']");
            for (XmlNode node : caps)
            {
                Package p = new Package(getSystem(), node);

                // Now it could be that this package is already loaded. If a cap version 1.0.1 is loaded and 1.0.2 is already
                // uploaded (but not deployed) then the package will also apear in the 'GetNewCapSummary'. So we need to validate
                // if the package is already there.

                // Important!! Since we're in the retrieveList we cannot use the getByName call. That is because then we can into
                // a recursive loop. We should use the _getByName method which does not trigger the retrieveList
                Package loadedPackage = dirtyGetByName(p.getName());
                if (loadedPackage == null)
                {
                    grow(p);
                }
                else
                {
                    loadedPackage.setNewVersion(p.getFullVersion());
                }
            }
        }
    }

    /**
     * This method returns an XML which contains all the ISVP files that are on the file system.
     * 
     * @param c The soap caller to use.
     * @return The XML with all the ISVP files on the file system.
     */
    public XmlNode executeListISVPackages(SoapCaller c)
    {
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("isvpackage", null);

        String baseURL = c.getUrlBase();
        if (c.isOLDEnabled())
        {
            baseURL += "system/";
        }
        baseURL += "com.eibus.web.application.ListISVPackages.wcp";

        // Execute the request. For this URL we cannot add the SAML token, so in case of the SAML client we need to call a
        // different method.
        String tmp = null;
        if (c instanceof SamlClientCaller)
        {
            SamlClientCaller scc = (SamlClientCaller) c;
            tmp = scc.sendHttpRequest(baseURL, "", params, false);
        }
        else
        {
            tmp = c.httpCall(baseURL, "", params);
        }
        XmlNode response = new XmlNode(tmp);
        return response;
    }

    /**
     * This method will build up the request for getting the package details of the ISV packages that are present.
     * 
     * @param packages The packages that are available.
     * @return The request for getting the definition details.
     */
    private XmlNode buildGetISVPackageDefinition(XmlNode packages)
    {
        XmlNode retVal = new XmlNode("GetISVPackageDefinition", Constants.XMLNS_ISV);

        List<XmlNode> tmp = packages.xpath(".//isv:computer/isv:isvp", Constants.NS);
        if (tmp == null || tmp.size() == 0)
        {
            // There are no ISV packages. This means that we should not send this message.
            retVal = null;
        }
        else
        {
            for (XmlNode url : tmp)
            {
                // Found an ISV package, so we need to create the element in the new request for it. On a CU6 machine there are a
                // few packages that are mentioned in the 'GetInstalledISVPackages', but they do not have a corresponding LDAP
                // entry.
                String packageName = url.getAttribute("name");
                if (!StringUtil.isEmptyOrNull(packageName))
                {
                    XmlNode node = retVal.add("file");
                    node.setAttribute("type", "isvpackage");
                    node.setAttribute("detail", "false");
                    node.setAttribute("wizardsteps", "true");
                    node.setText(packageName);
                }
            }
        }

        return retVal;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return getSystem().getKey() + ":packageDefinitions";
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return null;
    }

    /**
     * This method gets whether or not the system supports CAP packages.
     * 
     * @return Whether or not the system supports CAP packages.
     */
    public boolean supportsCap()
    {
        return supportsCap;
    }

    /**
     * This method sets whether or not the system supports CAP packages.
     * 
     * @param supportsCap Whether or not the system supports CAP packages.
     */
    public void setsupportsCap(boolean supportsCap)
    {
        this.supportsCap = supportsCap;
    }
}