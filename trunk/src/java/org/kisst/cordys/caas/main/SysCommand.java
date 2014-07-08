package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;

public abstract class SysCommand extends CommandBase
{
    protected final Cli cli = new Cli();
    protected final Cli.StringOption systemOption = cli.stringOption("s", "system", "the system to use", null);
    public SysCommand(String usage, String summary) {
        super(usage, summary);
    }
    protected CordysSystem getSystem() {
        String sysname=systemOption.get();
        if (sysname==null)
            sysname=Caas.defaultSystem;
        return Caas.getSystem(sysname);
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
    
    @Override public String getSyntax() { return super.getSyntax()+"\n"+cli.getSyntax("                    "); }
    
}
