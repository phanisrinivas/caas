/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import static org.kisst.cordys.caas.main.Environment.debug;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Class to represent a node of the Cordys cluster.
 */
public class Machine extends CordysObject
{
    /** Holds the monitor. */
    private final ServiceContainer monitor;
    /** Holds the hostname. */
    private final String hostname;
    /** Holds the cordys install dir. */
    private final String cordysInstallDir;

    /**
     * Instantiates a new machine.
     * 
     * @param monitor The monitor
     */
    protected Machine(ServiceContainer monitor)
    {
        this.monitor = monitor;
        String tmp = monitor.getName();
        this.hostname = tmp.substring(tmp.indexOf("monitor@") + 8);
        this.cordysInstallDir = readEIBProperty("CORDYS_INSTALL_DIR");
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#toString()
     */
    @Override
    public String toString()
    {
        return getVarName();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return hostname;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return "machine:" + getName();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return monitor.getSystem();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return getSystem().getVarName() + ".machine." + getName();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        // A machine has no organizational context
        return null;
    }

    /**
     * This method gets the monitor.
     * 
     * @return The monitor
     */
    public ServiceContainer getMonitor()
    {
        return monitor;
    }

    /**
     * Refresh service containers.
     */
    public void refreshServiceContainers()
    {
        XmlNode request = new XmlNode(Constants.LIST, Constants.XMLNS_MONITOR);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);
        for (XmlNode tuple : response.getChildren("tuple"))
        {
            XmlNode workerprocess = tuple.getChild("old/workerprocess");
            String dn = workerprocess.getChildText("name");
            ServiceContainer obj = (ServiceContainer) getSystem().getLdap(dn);
            obj.setWorkerprocess(workerprocess);
        }
    }

    /**
     * Loads the ISVP.
     * 
     * @param isvpName The isvp name
     * @param timeOutInMillis The time out in millis
     * @return The string
     */
    public String loadIsvp(String isvpName, long timeOutInMillis)
    {
        return loadIsvp(isvpName, null, timeOutInMillis);
    }

    /**
     * Loads the ISVP.
     * 
     * @param isvpName The isvp name
     * @param prompSetsXMLNode The promp sets xml node
     * @param timeOutInMilis The time out in milis
     * @return The string
     */
    public String loadIsvp(String isvpName, XmlNode prompSetsXMLNode, long timeOutInMilis)
    {
        // Get the ISVP definition details
        XmlNode details = getIsvpDefinition(isvpName);
        XmlNode isvpPackageNode = details.getChild("ISVPackage");

        // Checking if dependent ISVPs are installed
        checkIfDependentIsvpsInstalled(isvpName, isvpPackageNode);

        XmlNode request = new XmlNode(Constants.LOAD_ISVP, Constants.XMLNS_ISV);
        // Set the timeout
        request.setAttribute("timeOut", String.valueOf(timeOutInMilis));
        request.add("url").setText("http://" + hostname + "/cordys/wcp/isvcontent/packages/" + isvpName);

        if (prompSetsXMLNode != null)
        {
            isvpPackageNode.add(prompSetsXMLNode.detach());
        }
        request.add(isvpPackageNode.detach());

        debug("LoadISVP Request " + request.toString());

        // Load the ISVP on the specific monitor. Required in case of HA installation
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);

        debug("LoadISVP Response " + response.toString());

        return response.getChildText("status");
    }

    /**
     * Check if dependent isvps installed.
     * 
     * @param isvpName The isvp name
     * @param isvpPackageNode The isvp package node
     */
    private void checkIfDependentIsvpsInstalled(String isvpName, XmlNode isvpPackageNode)
    {
        // System.out.print("Checking if dependent ISVPs of '"+isvpName+"' are installed on '"+hostname+"' Node ... ");
        List<XmlNode> dependentIsvps = null;
        List<XmlNode> installedIsvps = getInstalledIsvps();
        XmlNode dependenciesNode = isvpPackageNode.getChild("dependencies");
        if (dependenciesNode != null)
        {
            dependentIsvps = dependenciesNode.getChildren();
        }
        for (XmlNode dependentIsvp : dependentIsvps)
        {
            String dependentIsvpName = dependentIsvp.getChildText("isvpackage/cn");
            boolean installed = false;
            for (XmlNode installedIsvp : installedIsvps)
            {
                if (installedIsvp.getAttribute("cn").equals(dependentIsvpName))
                {
                    installed = true;
                    break;
                }
            }
            if (!installed)
            {
                System.out.println("FAILED");
                throw new CaasRuntimeException("Dependent ISVP '" + dependentIsvpName + "' is not installed on " + hostname);
            }
        }
        // System.out.println("OK");
    }

    /**
     * Upgrades ISVP It needs to consider the rules and other runtime content as well Currently It overwrites the BPM content in
     * the previous ISVP.
     * 
     * @param isvpName The isvp name
     * @param deleteReferences The delete references
     * @param timeOutInMillis The time out in millis
     * @return The string
     */
    public String upgradeIsvp(String isvpName, boolean deleteReferences, long timeOutInMillis)
    {
        return upgradeIsvp(isvpName, null, deleteReferences, timeOutInMillis);
    }

    /**
     * Upgrades ISVP It needs to consider the rules and other runtime content as well Currently It overwrites the BPM content in
     * the previous ISVP.
     * 
     * @param isvpName The isvp name
     * @param prompSetsXMLNode The promp sets xml node
     * @param deleteReferences The delete references
     * @param timeOutInMillis The time out in millis
     * @return The string
     */
    public String upgradeIsvp(String isvpName, XmlNode prompSetsXMLNode, boolean deleteReferences, long timeOutInMillis)
    {
        // Get the provided ISVP details
        XmlNode details = getIsvpDefinition(isvpName);
        XmlNode isvPackageNode = details.getChild("ISVPackage");
        isvPackageNode.setAttribute("canUpgrade", "true");
        isvPackageNode.setAttribute("deleteReferences", String.valueOf(deleteReferences));

        if (prompSetsXMLNode != null)
        {
            isvPackageNode.add(prompSetsXMLNode.detach());
        }
        else
        // add default BPM Promptsets
        {
            XmlNode promptsetNode = isvPackageNode.add("promptset", Constants.XMLNS_ISV);
            // Add a prompt node and set its value as true to overwrite the BPM content in the previous ISV
            XmlNode promptNode = promptsetNode.add("BusinessProcessEngine").add("prompt");
            promptNode.setText("yes");
            promptNode.setAttribute("id", "overwrite");
            promptNode.setAttribute("value", "yes");
            promptNode.setAttribute("description",
                    "This value indicates whether or not the ISV has overwritten the contents of a previous ISV.");
        }
        XmlNode request = new XmlNode(Constants.UPGRADE_ISVP, Constants.XMLNS_ISV);
        // Set the timeout value
        request.setAttribute("timeOut", String.valueOf(timeOutInMillis));
        request.add("url").setText("http://" + hostname + "/cordys/wcp/isvcontent/packages/" + isvpName);
        request.add(isvPackageNode.detach());

        debug("UpgradeISVP Request " + request.toString());

        // Upgrade the ISVP on the specific monitor. Required in case of HA installation
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);

        debug("UpgradeISVP Response " + response.toString());

        // Read the status message
        return response.getChildText("status");
    }

    /**
     * Gets the complete details of the given ISVP.
     * 
     * @param isvpName The isvp name
     * @return XmlNode ISVP details XML node
     */
    private XmlNode getIsvpDefinition(String isvpName)
    {
        XmlNode request = new XmlNode(Constants.GET_ISVP_DEFINITION, Constants.XMLNS_ISV);
        XmlNode file = request.add("file");
        file.setText(isvpName);
        file.setAttribute("type", "isvpackage");
        file.setAttribute("detail", "false");
        file.setAttribute("wizardsteps", "true");
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        return monitor.call(request, queryParams);
    }

    /**
     * Unloads the ISVP.
     * 
     * @param isvp The isvp
     * @param deleteReferences The delete references
     */
    public void unloadIsvp(Package isvp, boolean deleteReferences)
    {
        XmlNode request = new XmlNode(Constants.UNLOAD_ISVP, Constants.XMLNS_ISV);
        XmlNode file = request.add("file");
        // TODO: This is causing an issue
        file.setText(isvp.getBasename());
        if (deleteReferences)
            file.setAttribute("deletereference", "true");
        else
            file.setAttribute("deletereference", "false");
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        monitor.call(request, queryParams);
        // TODO: do this only for last machine??? getSystem().removeLdap(isvp.getDn());
    }

    /**
     * Lists all the ISVP files.
     * 
     * @return List<String> Names of the ISVPs
     */
    public java.util.List<String> getIsvpFiles()
    {
        LinkedList<String> result = new LinkedList<String>();
        XmlNode request = new XmlNode(Constants.LIST_ISVPS, Constants.XMLNS_ISV);
        request.add("type").setText("ISVPackage");
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);
        for (XmlNode child : response.getChildren())
        {
            String url = child.getText();
            result.add(url.substring(url.lastIndexOf("/") + 1));
        }
        return result;
    }

    /**
     * This method gets the cordys install dir.
     * 
     * @return The cordys install dir
     */
    public String getCordysInstallDir()
    {
        return cordysInstallDir;
    }

    /**
     * Reads the value of given EIB property.
     * 
     * @param propertyName The property name
     * @return String
     */
    public String readEIBProperty(String propertyName)
    {
        XmlNode request = new XmlNode(Constants.GET_PROPERTY, Constants.XMLNS_MONITOR);
        request.add("property").setAttribute("name", propertyName);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);
        XmlNode propertyNode = response.getChild("tuple/old/property");
        return propertyNode.getAttribute("value", null);
    }

    /**
     * Uploads the ISVP.
     * 
     * @param isvpFilePath The isvp file path
     */
    public void uploadIsvp(String isvpFilePath)
    {
        File isvpFile = new File(isvpFilePath);
        String isvpName = isvpFile.getName();
        String isvpEncodedContent = FileUtil.encodeFile(isvpFilePath);
        XmlNode request = new XmlNode(Constants.UPLOAD_ISVP, Constants.XMLNS_ISV);
        request.add("name").setText(isvpName);
        request.add("content").setText(isvpEncodedContent);
        // Upload the ISVP on to the specific monitor. Required in case of HA installation
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        monitor.call(request, queryParams);
    }

    /**
     * This method gets the installed isvps.
     * 
     * @return The installed isvps
     */
    public List<XmlNode> getInstalledIsvps()
    {
        XmlNode request = new XmlNode(Constants.GET_INSTALLED_ISVPS, Constants.XMLNS_ISV);
        request.add("computer").setText(this.hostname);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = monitor.call(request, queryParams);
        XmlNode computerNode = response.getChild("computer");
        List<XmlNode> list = new ArrayList<XmlNode>();
        list.addAll(computerNode.getChildren());
        return list;
    }
}
