package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.util.XmlNode;

public class XMLStoreObject extends CordysObject{

	private String key = null;
	private String version = null;
	
	@Override
	public String getKey() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public CordysSystem getSystem() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVarName() {
		// TODO Auto-generated method stub
		return null;
	}
	
	/**
	 * 
	 *  sets the "organization" as default version and 
	 *  "false" as overwrite
	 * @param key  Full path to xmlstore object
	 */
	public XMLStoreObject(String key)
	{
		this(key, "organization");
	}
	
	public XMLStoreObject(String key,String version)
	{
	}
	
	/**
	 * Fetches the xmlstore content using GetXMLObject  
	 * @return XML content of the xmlstore object 
	 */
	public XmlNode getXML()
	{
		return null;
	}
	
	
	/**
	 * If overwrite is set to true, then unconditional needs to set to true
	 * in UpdateXMLObject 
	 * 
	 * @param xml - xml content to upate 
	 */
	public void updateXML(XmlNode xml,boolean overwrite)
	{
		
	}

}
