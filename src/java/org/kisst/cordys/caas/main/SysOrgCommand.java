package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.exception.CaasRuntimeException;

public abstract class SysOrgCommand extends CommandBase
{
    protected final Cli cli = new Cli();
    protected final Cli.StringOption systemOption = cli.stringOption("s", "system", "the system to use", null);
    protected final Cli.StringOption orgOption = cli.stringOption("o", "organization", "the organization to use", null);
    public SysOrgCommand(String usage, String summary) {
        super(usage, summary);
        setSyntax(usage + "\n" + cli.getSyntax("                "));
    }
    protected CordysSystem getSystem() {
        String sysname=systemOption.get();
        if (sysname==null)
            sysname=Caas.defaultSystem;
        return Caas.getSystem(sysname);
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

    protected String[] checkArgs(String[] args) {
        args = cli.parse(args);
        if (systemOption.isSet())
            Caas.defaultSystem = systemOption.get();
        return args;
    }

    @Override public String getHelp() {
        return "\nOPTIONS\n" + cli.getSyntax("\t");
    }
}
