package org.kisst.cordys.caas;


import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;

public class Dso extends LdapObjectBase {

	public final XmlProperty config = new XmlProperty("datasourceconfiguration");
	public final XmlSubProperty defaultdb = new XmlSubProperty(config, "defaultDatabase");
	public final XmlSubProperty username = new XmlSubProperty(config, "userName");
	public final XmlSubProperty password = new XmlSubProperty(config, "password");
	public final XmlSubProperty connectionstring = new XmlSubProperty(config, "connectionString");
	
	protected Dso(LdapObject parent, String dn) {
		super(parent, dn);
	}
	@Override protected String prefix() { return "dso"; }
}