package org.kisst.cordys.caas;


import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;

/**
 * Class to represent a DSO
 * 
 * @author galoori
 *
 */
public class Dso extends LdapObjectBase 
{
	public final XmlProperty config = new XmlProperty("datasourceconfiguration");
	public final XmlSubProperty defaultdb = new XmlSubProperty(config, "defaultDatabase");
	public final XmlSubProperty username = new XmlSubProperty(config, "userName");
	public final XmlSubProperty password = new XmlSubProperty(config, "password");
	public final XmlSubProperty connectionstring = new XmlSubProperty(config, "connectionString");
	
	/**
	 * Constructs a DSO with the given parent and dn
	 * 
	 * @param parent
	 * @param dn
	 */
	protected Dso(LdapObject parent, String dn) 
	{
		super(parent, dn);
	}
	@Override protected String prefix() 
	{ 
		return "dso"; 
	}
}