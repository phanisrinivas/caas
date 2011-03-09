package org.kisst.cordys.caas;

import java.util.ArrayList;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.util.XmlNode;

public class XMLStoreObject extends CordysObject{

	//Location of the XMLStoreObject content in Cordys XMLStore
	private String key;
	//Version of the XMLStoreObject. Either "organization" or "user"
	private String version;
	//Xml content of the XMLStoreObject
	private XmlNode xml;
	//Represents the organization to which this XMLStore object belongs
	private Organization org;
	//Contains the list of XMLStore versions
	public static ArrayList<String> versionList;
	
	@Override
	public String getKey() {
		return key;
	}
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}
	@Override
	public String getVarName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	/**
	 * Returns the CordysSystem object of XMLStoreObject
	 */
	public CordysSystem getSystem() {
		
		CordysSystem system = org.getSystem();
		if(system==null)
			throw new CaasRuntimeException("Cordys system is null for organization '"+org.getName()+"'");
		return system;
	}
	
	/**
	 *  Sets the "organization" the as default version
	 *  
	 * @param key  Full path of XMLStoreObject in Cordys XMLStore
	 * @param org Organization object to which the XMLStoteObject belongs
	 */
	public XMLStoreObject(String key,Organization org)
	{
		this(key, "organization", org);
	}
	
	/**
	 *  Sets the key, version and xml of the XMLStoreObject
	 *  
	 * @param key  Complete path to the XMLStoreObject
	 * @param version Version of the XMLStoreObject
	 */
	public XMLStoreObject(String key,String version, Organization org)
	{
		this.key = key;
		this.version = version;
		this.org = org;
		this.xml = readXMLStoreObject(key, version);
	}
	
	/**
	 *  Fetches the Xml content of the XMLStoreObject from Cordys
	 *  XMLStore using GetXMLObject service
	 *  
	 * @param key  Complete path to the XMLStoreObject
	 * @param version Version of the XMLStoreObject
	 * @return Xml content
	 */
	public XmlNode readXMLStoreObject(String key, String version) 
	{	
		//Check if the key or version is null 
		if(key==null || version==null)
			throw new CaasRuntimeException("key or version of the XMLStoreObject is null. key:: "+key+" version:: "+version);
		
		key = key.trim();
		version = version.trim();
		//Check if the key or version is empty 
		if(key.length()==0 || version.length()==0)
			throw new CaasRuntimeException("key or version of the XMLStoreObject is empty. key:: "+key+" version:: "+version);
		
		//Check for version type
		if(!versionList.contains(version))
			throw new CaasRuntimeException("Invalid XMLStore version '"+version+"'. Please change it to either 'organization' or 'user'");
		
		XmlNode request = new XmlNode("GetXMLObject",xmlns_xmlstore);
		request.add("key").setAttribute("version", version).setText(key);
		XmlNode response = call(request);
		return response.getChild("tuple/old");
	}
	
	/**
	 * Gets the Xml content of the XMLStoreObject
	 * 
	 * @return Xml content of the XMLStoreObject 
	 */
	public XmlNode getXML()
	{	
		return xml;
	}
	
	/**
	 * Updates the Xml content of the XMLStoreObject using UpdateXMLObject service
	 * If 'update' flag is set to true, then the Xml content is overwritten unconditionally
	 * If 'update' flag is set to false and the XMLStore object is already existing in
	 * Cordys XMLStore then exception is thrown
	 * 
	 * @param newXml - Xml content to be updated
	 * @param update - true/false
	 */
	public void updateXML(XmlNode newXml, boolean update)
	{
		XmlNode request=new XmlNode("UpdateXMLObject", xmlns_xmlstore);
		XmlNode tuple=request.add("tuple");
		tuple.setAttribute("key", key);
		tuple.setAttribute("version", version);
		tuple.setAttribute("unconditional", String.valueOf(update));
		
		if (newXml!=null)
			tuple.add("new").add(newXml);
		call(request);
		this.xml = newXml;
	}
	
	/**
	 * Creates the XMLStoreObject with given Xml content
	 * It throws an exception if the XMLStoreObject is already existing
	 * 
	 * @param newXml Xml content
	 */
	public void updateXML(XmlNode newXml)
	{
		updateXML(newXml,false);
	}

	/**
	 * Executes the XMLStore web services. It specifies the organization name under which context
	 * the web services will be executed
	 * 
	 * @param method Web service request xml
	 * @return
	 */
	public XmlNode call(XmlNode method) { 
		return getSystem().call(method,org.getDn(),null); 
	}

	//Initialize and load XMLStore versions
	static{
		 versionList = new ArrayList<String>();
		 versionList.add("organization");
		 versionList.add("user");
	}
}
