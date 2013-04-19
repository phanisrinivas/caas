package org.kisst.cordys.caas;

import java.util.HashMap;
import java.util.List;

import org.kisst.cordys.caas.soap.SamlClientCaller;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class maintains the list of package definitions that are available on the Cordys system. It will use a combination of
 * several web services to determine which definitions are available. Also there is a difference between a package definition and
 * a loaded package. This list contains the packages which are not necesarily loaded.
 */
public class PackageList extends CordysObjectList<Package>
{
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

        // Now we have the list of all the ISV packages that are loaded on the system. Now we need to get the details for each
        // package.
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
        request = new XmlNode("GetDeployedCapSummary", "http://schemas.cordys.com/cap/1.0");
        response = c.call(request);

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
            grow(p);
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
            // There are no ISV packages. THis means that we should not send this message.
            retVal = null;
        }
        else
        {
            for (XmlNode url : tmp)
            {
                // Found an ISV package, so we need to create the element in the new request for it.
                XmlNode node = retVal.add("file");
                node.setAttribute("type", "isvpackage");
                node.setAttribute("detail", "false");
                node.setAttribute("wizardsteps", "true");
                node.setText(url.getAttribute("name"));
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
}