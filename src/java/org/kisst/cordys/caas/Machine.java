/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.LinkedList;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.XmlNode;

public class Machine extends CordysObject {
	private final ServiceContainer monitor;
	private final String hostname;
	private final String cordysInstallDir;
	
	protected Machine(ServiceContainer monitor) 
	{
		this.monitor=monitor;
		String tmp=monitor.getName();
		this.hostname=tmp.substring(tmp.indexOf("monitor@")+8);
		this.cordysInstallDir=readEIBProperty("CORDYS_INSTALL_DIR");
	}

	@Override public String toString() { return getVarName();}
	@Override public String getName() { return hostname;}
	@Override public String getKey() { return "machine:"+getName();	}
	@Override public CordysSystem getSystem() { return monitor.getSystem();	}
	@Override public String getVarName() { return getSystem().getVarName()+".machine."+getName();}

	public ServiceContainer getMonitor() { return monitor; }
	
	public void refreshSoapProcessors() {
		XmlNode request=new XmlNode(Constants.LIST, Constants.XMLNS_MONITOR);
		XmlNode response=monitor.call(request);
		for (XmlNode tuple: response.getChildren("tuple")) {
			XmlNode workerprocess=tuple.getChild("old/workerprocess");
			String dn=workerprocess.getChildText("name");
			ServiceContainer obj= (ServiceContainer) getSystem().getLdap(dn);
			obj.setWorkerprocess(workerprocess);
		}
	}

	/**
	 * Loads the ISVP
	 * 
	 * @param isvpName
	 * @return
	 */
	public String loadIsvp(String isvpName,long timeOutInMillis) 
	{
		return loadIsvp(isvpName,null,timeOutInMillis);
	}
	/**
	 * Loads the ISVP
	 * 
	 * @param isvpName
	 * @return
	 */
	public String loadIsvp(String isvpName, XmlNode prompSetsXMLNode,long timeOutInMilis) 
	{
		//Get the ISVP definition details
		XmlNode details=getIsvpDefinition(isvpName);
		XmlNode isvpPackageNode = details.getChild("ISVPackage");
		
		//Checking if dependent ISVPs are installed
		checkIfDependentIsvpsInstalled(isvpName,isvpPackageNode);
		
		
		XmlNode request=new XmlNode(Constants.LOAD_ISVP, Constants.XMLNS_ISV);
		//Set the timeout
		request.setAttribute("timeOut", String.valueOf(timeOutInMilis));
		request.add("url").setText("http://"+hostname+"/cordys/wcp/isvcontent/packages/"+isvpName);		

		if (prompSetsXMLNode!=null){
			isvpPackageNode.add(prompSetsXMLNode.detach());
		}
		request.add(isvpPackageNode.detach());		
		if(getSystem().getEnv().debug){
			getSystem().getEnv().debug("LoadISVP Request "+request.toString());
		}
		//Load the ISVP on the specific monitor. Required in case of HA installation
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("receiver", monitor.getDn());
		XmlNode response = monitor.call(request, queryParams);
		if(getSystem().getEnv().debug){
			getSystem().getEnv().debug("LoadISVP Response "+response.toString());
		}
		//Read the status message		
		return response.getChildText("status");
	}
	
	private void checkIfDependentIsvpsInstalled(String isvpName, XmlNode isvpPackageNode){
		System.out.print("Checking if dependent ISVPs of '"+isvpName+"' are installed on '"+hostname+"' Node ... ");
		List<XmlNode> installedIsvps = getInstalledIsvps();
		List<XmlNode> dependentIsvps = isvpPackageNode.getChildren("dependencies");
		for(XmlNode dependentIsvp: dependentIsvps){
			String dependentIsvpName = dependentIsvp.getChildText("isvpackage/cn");
			boolean installed = false;
			for(XmlNode installedIsvp:installedIsvps){
				if(installedIsvp.getAttribute("cn").equals(dependentIsvpName)){
					installed = true;
					break;
				}
			}
			if(!installed){
				System.out.println("FAILED");
				throw new CaasRuntimeException("Dependent ISVP '"+dependentIsvpName+"' is not installed on "+hostname);
			}
		}
		System.out.println("OK");
	}

	/**
	 * Upgrades ISVP
	 * 
	 * It needs to consider the rules and other runtime content as well
	 * Currently It overwrites the BPM content in the previous ISVP
	 * 
	 * @param isvpName
	 * @return
	 */	
	public String upgradeIsvp(String isvpName, boolean deleteReferences, long timeOutInMillis)
	{
		return upgradeIsvp(isvpName,null,deleteReferences,timeOutInMillis);
	}
	
	/**
	 * Upgrades ISVP
	 * 
	 * It needs to consider the rules and other runtime content as well
	 * Currently It overwrites the BPM content in the previous ISVP
	 * 
	 * @param isvpName
	 * @return
	 */	
	public String upgradeIsvp(String isvpName,XmlNode prompSetsXMLNode, boolean deleteReferences, long timeOutInMillis)
	{
		//Get the provided ISVP details
		XmlNode details=getIsvpDefinition(isvpName);
		XmlNode isvPackageNode = details.getChild("ISVPackage");
		isvPackageNode.setAttribute("canUpgrade", "true");
		isvPackageNode.setAttribute("deleteReferences", String.valueOf(deleteReferences));
		
		if (prompSetsXMLNode!=null)
		{
			isvPackageNode.add(prompSetsXMLNode.detach());
		}
		else // add default BPM Promptsets 
		{		
			XmlNode promptsetNode = isvPackageNode.add("promptset", Constants.XMLNS_ISV);
			//Add a prompt node and set its value as true to overwrite the BPM content in the previous ISV
			XmlNode promptNode = promptsetNode.add("BusinessProcessEngine").add("prompt");
			promptNode.setText("yes");
			promptNode.setAttribute("id", "overwrite");
			promptNode.setAttribute("value", "yes");
			promptNode.setAttribute("description", "This value indicates whether or not the ISV has overwritten the contents of a previous ISV.");
		}
		XmlNode request=new XmlNode(Constants.UPGRADE_ISVP, Constants.XMLNS_ISV);
		//Set the timeout value
		request.setAttribute("timeOut", String.valueOf(timeOutInMillis));
		request.add("url").setText("http://"+hostname+"/cordys/wcp/isvcontent/packages/"+isvpName);		
		request.add(isvPackageNode.detach());

		if(getSystem().getEnv().debug)
		{
			getSystem().getEnv().debug("UpgradeISVP Request "+request.toString());
		}
		
		//Upgrade the ISVP on the specific monitor. Required in case of HA installation		
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("receiver", monitor.getDn());
		XmlNode response = monitor.call(request, queryParams);
		if(getSystem().getEnv().debug)
		{
			getSystem().getEnv().debug("UpgradeISVP Response "+response.toString());
		}
		//Read the status message
		return response.getChildText("status");
	}
	
	/**
	 * Gets the complete details of the given ISVP
	 * 
	 * @param isvpName
	 * @return XmlNode ISVP details XML node
	 */	
	private XmlNode getIsvpDefinition(String isvpName)
	{
		XmlNode request=new XmlNode(Constants.GET_ISVP_DEFINITION, Constants.XMLNS_ISV);
		XmlNode file=request.add("file");
		file.setText(isvpName);
		file.setAttribute("type", "isvpackage");
		file.setAttribute("detail", "false");
		file.setAttribute("wizardsteps", "true");
		return monitor.call(request);
	}
	
	/**
	 * Unloads the ISVP
	 * 
	 * @param isvp
	 * @param deleteReferences
	 * @return 
	 */	
	public void unloadIsvp(Isvp isvp, boolean deleteReferences) 
	{
		XmlNode request=new XmlNode(Constants.UNLOAD_ISVP, Constants.XMLNS_ISV);
		XmlNode file=request.add("file");
		file.setText(isvp.getBasename());
		if (deleteReferences)
			file.setAttribute("deletereference", "true");
		else
			file.setAttribute("deletereference", "false");
		monitor.call(request);
		//TODO: do this only for last machine??? getSystem().removeLdap(isvp.getDn());
	}
	
	/**
	 * Lists all the ISVP files
	 * 
	 * @return List<String> Names of the ISVPs 
	 */	
	public java.util.List<String> getIsvpFiles() 
	{
		LinkedList<String> result=new LinkedList<String>();
		XmlNode request=new XmlNode(Constants.LIST_ISVPS, Constants.XMLNS_ISV);
		request.add("type").setText("ISVPackage");
		XmlNode response=monitor.call(request);
		for (XmlNode child: response.getChildren()) {
			String url=child.getText();
			result.add(url.substring(url.lastIndexOf("/")+1));
		}
		return result;
	}
	
	public String getCordysInstallDir()
	{
		return cordysInstallDir;
	}
	
	/**
	 * Reads the value of given EIB property
	 * 
	 * @param propertyName
	 * @return String
	 */
	public String readEIBProperty(String propertyName)
	{
		XmlNode request=new XmlNode(Constants.GET_PROPERTY, Constants.XMLNS_MONITOR);
		request.add("property").setAttribute("name", propertyName);
		XmlNode response = monitor.call(request);
		XmlNode propertyNode = response.getChild("tuple/old/property");
		return propertyNode.getAttribute("value", null);
	}
	
	/**
	 * Uploads the ISVP 
	 * 
	 * @param isvpFilePath
	 * @return
	 */
	public void uploadIsvp(String isvpFilePath)
	{	
		File isvpFile = new File(isvpFilePath);
		String isvpName = isvpFile.getName();
		String isvpEncodedContent = FileUtil.encodeFile(isvpFilePath);
		XmlNode request = new XmlNode(Constants.UPLOAD_ISVP,Constants.XMLNS_ISV);
		request.add("name").setText(isvpName);
		request.add("content").setText(isvpEncodedContent);
		//Upload the ISVP on to the specific monitor. Required in case of HA installation
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("receiver", monitor.getDn());
		monitor.call(request, queryParams);
	}
	
	public List<XmlNode> getInstalledIsvps(){
		XmlNode  request=new XmlNode(Constants.GET_INSTALLED_ISVPS, Constants.XMLNS_ISV);
		request.add("computer").setText(this.hostname);
		XmlNode response = monitor.call(request);
		XmlNode computerNode = response.getChild("computer");
		List<XmlNode> list = new ArrayList<XmlNode>();
		list.addAll(computerNode.getChildren());
		return list;
	}
}
