package org.kisst.cordys.caas;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.kisst.cordys.caas.soap.SamlClientCaller;
import org.kisst.cordys.caas.soap.SoapCaller;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class maintains the list of package definitions that are available on the Cordys system. It will use a combination of
 * several web services to determine which definitions are available. Also there is a difference between a package definition and
 * a loaded package. This list contains the packages which are not necesarily loaded.
 */
public class PackageList extends CordysObjectList<Package>
{
    /** The default timeout for retrieving the package list */
    public static final long DEFAULT_PACKAGE_TIMEOUT = 90000L;
    /** Holds whether or not the system supports CAP packages. */
    private Boolean supportsCap = null;
    private final  Machine machine;

    /**
     * Instantiates a new package definition list.
     * 
     * @param system The system
     */
    PackageList(Machine machine)
    {
        super(machine.getSystem());
        this.machine=machine;
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
        request.add("computer").setText(machine.getName());

        XmlNode response = c.call(request, DEFAULT_PACKAGE_TIMEOUT);

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

        getISVPackageDefinitions(response);        
        /*
        // Next step is to get the definition of the packages for which filenames are specified.
        request = buildGetISVPackageDefinition(response);
        if (request != null)
        {
            // There are 1 or more ISV packages available.
            response = c.call(request, DEFAULT_PACKAGE_TIMEOUT);

            // Now we have all the definitions. This means we can create the package definitions
            List<XmlNode> isvs = response.getChildren("ISVPackage");
            for (XmlNode node : isvs)
            {
                Package p = new Package(getSystem(), node);
                grow(p);
            }
        }
         */
        
        // Retrieve the CAP packages from the compatibility manager as in 4.3 the SOAP API has changed.
        List<Package> packages = getSystem().getCompatibilityManager().getCAPPackages(c, system, this);

        // Add all the found packages to this list.
        for (Package p : packages)
        {
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

    private void getISVPackageDefinitions(XmlNode packages)
    {
        XmlNode request = new XmlNode("GetISVPackageDefinition", Constants.XMLNS_ISV);
        LinkedHashMap<String, XmlNode> defs = new LinkedHashMap<String,XmlNode>();
        
        List<XmlNode> tmp = packages.xpath(".//isv:computer/isv:isvp", Constants.NS);
        if (tmp == null || tmp.size() == 0)
            return;
        for (XmlNode url : tmp)
        {
            // Found an ISV package, so we need to create the element in the new request for it. On a CU6 machine there are a
            // few packages that are mentioned in the 'GetInstalledISVPackages', but they do not have a corresponding LDAP
            // entry.
            String packageName = url.getAttribute("name");
            defs.put(packageName, url);
            if (!StringUtil.isEmptyOrNull(packageName))
            {
                XmlNode node = request.add("file");
                node.setAttribute("type", "isvpackage");
                node.setAttribute("detail", "false");
                node.setAttribute("wizardsteps", "true");
                node.setText(packageName);
            }
        }
        SoapCaller c = system.getSoapCaller();
        XmlNode response = c.call(request, DEFAULT_PACKAGE_TIMEOUT);

        List<XmlNode> isvs = response.getChildren("ISVPackage");
        for (XmlNode node : isvs)
        {
            try {
                Package p = new Package(getSystem(), node);
                grow(p);
            }
            catch (Package.IncompleteDefinitionException e) {
                String key= e.def.getAttribute("file");
                if (key!=null) {
                    XmlNode def = defs.get(key);
                    if (def!=null) {
                        Package p = new Package(getSystem(), def);
                        grow(p);
                    }
                }
            }
        }
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
        if (supportsCap == null)
        {
            synchronized(getSystem())
            {
                supportsCap = getSystem().getCompatibilityManager().supportsCap(system.getSoapCaller(), system);
            }
        }
        
        return supportsCap;
    }

    /**
     * This method sets whether or not the system supports CAP packages.
     * 
     * @param supportsCap Whether or not the system supports CAP packages.
     */
    public void setSupportsCap(boolean supportsCap)
    {
        this.supportsCap = supportsCap;
    }
}