package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;

/**
 * Class to represent a DSO
 */
public class Dso extends LdapObjectBase
{
    /** Holds the datasource configuration */
    public final XmlProperty config = new XmlProperty("datasourceconfiguration");
    /** Holds the default database that is configured. */
    public final XmlSubProperty defaultdb = new XmlSubProperty(config, "defaultDatabase");
    /** Holds the username to use */
    public final XmlSubProperty username = new XmlSubProperty(config, "userName");
    /** Holds the base64 encoded password */
    public final XmlSubProperty password = new XmlSubProperty(config, "password");
    /** Holds the actual conenction string to use */
    public final XmlSubProperty connectionstring = new XmlSubProperty(config, "connectionString");

    /**
     * Instantiates a new dso.
     * 
     * @param parent the parent LDAP entry.
     * @param dn the dn of this DSO.
     */
    protected Dso(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "dso";
    }
}