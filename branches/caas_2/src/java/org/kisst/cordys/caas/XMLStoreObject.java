package org.kisst.cordys.caas;

import java.util.HashMap;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 *  Class to represent an object in XMLStore
 *  
 *  @author galoori
 */
public class XMLStoreObject extends CordysObject
{
	private String key;
	private String version;
	private XmlNode xml=null;
	private Organization org;
	private String name=null;
	private String lastModified=null;
	private final CordysSystem system;
	
	@Override
	public String getKey() 
	{
		return key;
	}
	@Override
	public String getName() 
	{
		return name;
	}
	@Override
	public String getVarName() 
	{
		return name;
	}
	
	public String getLastModified()
	{
		return lastModified;
	}
	
	/**
	 * @return XML of the XMLStore object
	 */
	public XmlNode getXML()
	{	
		return xml;
	}
	
	/**
	 * @return version of the XMLStore object
	 */
	public String getVersion()
	{
		return version;
	}
		
	@Override
	public CordysSystem getSystem() 
	{
		return system;
	}
	
	
	/**
	 * Constructs XMLStore object with given key and organization
	 * 
	 * @param key
	 * @param org
	 */
	public XMLStoreObject(String key,Organization org)
	{
		this(key, "organization", org);
	}
	
	/**
	 * Constructs XMLStore object with given key, version and organization
	 * 
	 * @param key
	 * @param version
	 * @param org
	 */
	public XMLStoreObject(String key,String version,  Organization org)
	{	
		this.key = key;
		this.version = version;
		this.org = org;
		this.system=org.getSystem();
		XmlNode response = getXMLObject(key, version);
		if(response.getChild("tuple/old")!=null)
		{
			this.xml = response.getChild("tuple/old").getChildren().get(0);
			this.name=response.getChild("tuple").getAttribute("name");
			this.lastModified=response.getChild("tuple").getAttribute("lastModified");	
		}
	}
	
	/**
	 * Returns the XML of the XMLStore object with the given key and version 
	 * 
	 * @param key
	 * @param version
	 * @return XML of the object
	 */
	public XmlNode getXMLObject(String key, String version)
	{	
		XmlNode request = new XmlNode(Constants.GET_XML_OBJECT,Constants.XMLNS_XMLSTORE);
		request.add("key").setAttribute("version", version).setText(key);
		XmlNode response = call(request);
		return response;
	}
	
	/**
	 * Appends the given XML to the XMLStore object 
	 * 
	 * @param node XML to be appended
	 */
	public void appendXML(XmlNode node)
	{
		XmlNode request=new XmlNode(Constants.APPEND_XML_OBJECT, Constants.XMLNS_XMLSTORE);
		XmlNode tuple=request.add("tuple");
		tuple.setAttribute("key", key);
		tuple.setAttribute("version", version);		
		if (node!=null)
			tuple.add("new").add(node);
		call(request);
		XmlNode response = getXMLObject(key, version);
		this.xml = response.getChild("tuple/old").getChildren().get(0);
	}
	
	/**
	 * Overwrites the existing XML of the object with the given XML
	 * 
	 * @param newXml XML that would overwrite the existing one
	 */
	public void overwriteXML(XmlNode newXml)
	{
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
	
	public XmlNode call(XmlNode request)
	{ 	
		HashMap<String, String> queryParams = new HashMap<String, String>();
		queryParams.put("organization", org.getDn());
		return getSystem().call(request, queryParams);			
	}

	/**
	 * @author galoori
	 *
	 */
	public static class List extends CordysObjectList<XMLStoreObject> 
	{
		private final Organization org;
		public List(Organization org) 
		{ 
			super(org.getSystem());
			this.org=org;
		}
		
		/* (non-Javadoc)
		 * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
		 */
		@Override protected void retrieveList() 
		{
			XmlNode method = new XmlNode(Constants.GET_COLLECTION, Constants.XMLNS_XMLSTORE);
			XmlNode folderNode = method.add("folder");
			folderNode.setAttribute("recursive", "true");
			folderNode.setAttribute("detail", "false");
			folderNode.setAttribute("version", "organization");
			HashMap<String, String> queryParams = new HashMap<String, String>();
			queryParams.put("timeout", "60000");
			queryParams.put("organization", org.getDn());
			XmlNode response=system.call(method,queryParams);
			for (XmlNode tuple :response.getChildren("tuple"))
			{	
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
		@Override public String getKey() 
		{ 
			return "XMLStoreObjects:"+org.getKey(); 
		}
	};


	public void delete() 
	{
		if ("isv".equals(version))
			throw new RuntimeException("Can not delete isv xmlstoreobject "+getVarName());
		XmlNode  request=new XmlNode(Constants.UPDATE_XML_OBJECT, Constants.XMLNS_XMLSTORE);
		XmlNode tuple=request.add("tuple");
		tuple.setAttribute("key", key);
		tuple.setAttribute("level", version);
		tuple.setAttribute("lastModified", lastModified);
		tuple.setAttribute("recursive", "true");
		call(request);
	}	
	
}
