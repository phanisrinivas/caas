package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.exception.CaasRuntimeException;

public abstract class SysOrgCommand extends SysCommand
{
    protected final Cli.StringOption orgOption = cli.stringOption("o", "organization", "the organization to use", null);
    public SysOrgCommand(String usage, String summary) {
        super(usage, summary);
        setSyntax(usage + "\n" + cli.getSyntax("                "));
    }
    protected Organization getOrg() {
        Organization retVal = null;
        if (orgOption.isSet()) 
            retVal = getSystem().org.getByName(orgOption.get());
        else
            retVal = getSystem().org.getByName(Caas.getDefaultOrg());
        if (retVal == null)
            throw new CaasRuntimeException("Could not find organization " + (orgOption.isSet() ? orgOption.get() : "unknown")
                    + " in system " + Caas.defaultSystem);
        return retVal;
    }
}
