package org.kisst.cordys.caas;

import java.util.HashMap;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 *  Class to represent an item in Cordys XMLStore
 *  It provides operations to read, create and update the XML
 *  
 *  @author galoori
 */

public class XMLStoreObject extends CordysObject{
	private String key;
	private String version;
	private XmlNode xml;
	private Organization org;
	private final String name;
	private final CordysSystem system;
	
	@Override
	public String getKey() {
		return key;
	}
	@Override
	public String getName() {
		int pos = key.lastIndexOf("/");
		if(pos>0)
			return key.substring(pos+1);
		return key;
	}
	@Override
	public String getVarName() {
		return getName();
	}
	public XmlNode getXml(){	
		return xml;
	}
	public String getVersion(){
		return version;
	}
		
	@Override
	public CordysSystem getSystem() {
		return org.getSystem();
	}
	
	public XMLStoreObject(String key,Organization org){
		this(key, "organization", org);
	}
	
	public XMLStoreObject(String key,String version,  Organization org){
		this.key = key;
		this.version = version;
		this.org = org;
		this.xml = readXMLStoreObject(key, version);
		this.system=org.getSystem();
		//this.name=name;
		this.name=null;
	}
	
	public XmlNode readXMLStoreObject(String key, String version) {	
		XmlNode request = new XmlNode(Constants.GET_XML_OBJECT,Constants.XMLNS_XMLSTORE);
		request.add("key").setAttribute("version", version).setText(key);
		XmlNode response = call(request);		
		if(response.getChild("tuple/old")!=null)
			return response.getChild("tuple/old").getChildren().get(0);
		else 
			return null;
	}
	public void appendXML(XmlNode node){
		XmlNode request=new XmlNode(Constants.APPEND_XML_OBJECT, Constants.XMLNS_XMLSTORE);
		XmlNode tuple=request.add("tuple");
		tuple.setAttribute("key", key);
		tuple.setAttribute("version", version);		
		if (node!=null)
			tuple.add("new").add(node);
		call(request);
		//Refresh the XML content of the XMLStoreObject after append operation	
		this.xml = readXMLStoreObject(key, version);
	}
	
	public void overwriteXML(XmlNode newXml){
		XmlNode request=new XmlNode(Constants.UPDATE_XML_OBJECT, Constants.XMLNS_XMLSTORE);
		XmlNode tuple=request.add("tuple");
		tuple.setAttribute("key", key);
		tuple.setAttribute("version", version);
		//Set 'unconditional' flag to true to overwrite the existing XML 
		tuple.setAttribute("unconditional", "true");
		if (newXml!=null)
			tuple.add("new").add(newXml);
		call(request);
		//Refresh the XML content of the XMLStoreObject after the update operation
		this.xml = newXml;
	}
	
	public XmlNode call(XmlNode request) { 	
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("organization", org.getDn());
		return getSystem().call(request, queryParams);			
	}

	public static class List extends CordysObjectList<XMLStoreObject> {
		private final Organization org;
		public List(Organization org) { 
			super(org.getSystem());
			this.org=org;
		}
		
		@Override protected void retrieveList() {
			XmlNode method = new XmlNode(Constants.GET_COLLECTION, Constants.XMLNS_XMLSTORE);
			XmlNode folderNode = method.add("folder");
			folderNode.setAttribute("recursive", "true");
			folderNode.setAttribute("detail", "false");
			folderNode.setAttribute("version", "organization");
			HashMap<String, String> queryParams = new HashMap<String, String>();
			queryParams.put("timeout", "60000");
			queryParams.put("organization", org.getDn());
			XmlNode response=system.call(method,queryParams);
			for (XmlNode tuple :response.getChildren("tuple")){	
				//Ignore folders
				if(Boolean.valueOf(tuple.getAttribute("isFolder")).booleanValue())
					continue;
				//Ignore WsApps runtime entries
				if(tuple.getAttribute("name").endsWith(".cmx"))
					continue;
				//Ignore CAF files
				if(tuple.getAttribute("name").endsWith(".caf"))
					continue;				
				XMLStoreObject obj=new XMLStoreObject(tuple.getAttribute("key"), tuple.getAttribute("level"), org);
				grow(obj);
			}
		}
		@Override public String getKey() { return "XMLStoreObjects:"+org.getKey(); }
	};
	
	public XMLStoreObject(Organization org, XmlNode tuple) {
		this.org=org;
		this.system=org.getSystem();
		this.name=null;
	}

	public void delete() {
	/*	if ("isv".equals(modelSpace))
			throw new RuntimeException("Can not delete isv processModel "+getVarName());
		XmlNode  webService=new XmlNode("DeleteProcessModel", xmlns_coboc);
		webService.add("processname").setText(name);
		org.call(webService);*/
	}	
	
}
