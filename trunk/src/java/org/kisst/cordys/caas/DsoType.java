package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Class to represent a DSO Type
 */
public class DsoType extends LdapObjectBase
{
    public final XmlProperty implementationclass = new XmlProperty("implementationclass");
    public final ChildList<Dso> dsos = new ChildList<Dso>(this, Dso.class);
    public final ChildList<Dso> dso = dsos;
    public final ChildList<Dso> d = dsos;

    /**
     * Constructs a DsoType as a child of the given parent and with the given dn.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected DsoType(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "dsotype";
    }

    /**
     * Creates a DSO for this DsoType
     * 
     * @param name DSO name
     * @param desc DSO description
     * @param config DSO configuration XML
     */
    public void createDso(String name, String desc, XmlNode config)
    {
        XmlNode newEntry = newEntryXml("", name, "datasource");
        newEntry.add("description").add("string").setText(desc);
        newEntry.add("datasourceconfiguration").add("string").setText(config.compact());
        createInLdap(newEntry);
        dsos.clear();
    }

    /**
     * Updates the DSO of this DsoType with the new configuration
     * 
     * @param dso DSO whose configuration needs to be updated
     * @param newConfig XmlNode representing the new configuration of the DSO
     */
    public void updateDso(Dso dso, XmlNode newConfig)
    {
        XmlNode oldEntry = dso.getEntry();
        XmlNode newEntry = oldEntry.clone();
        newEntry.getChild("datasourceconfiguration/string").setText(newConfig.compact());
        updateLdap(oldEntry, newEntry);
        dsos.clear();
    }
}
