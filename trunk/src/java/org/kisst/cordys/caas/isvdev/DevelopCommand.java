package org.kisst.cordys.caas.isvdev;

import java.io.File;

import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.main.Cli;
import org.kisst.cordys.caas.main.Command;
import org.kisst.cordys.caas.main.CompositeCommand;
import org.kisst.cordys.caas.main.SysOrgCommand;
import org.kisst.cordys.caas.util.FileUtil;

public class DevelopCommand extends CompositeCommand
{
    private class ToCordysCommand extends SysOrgCommand {
        protected final Cli.Flag clearExistingOption = cli.flag("c", "clearExisting", "an existing wsi should be cleared (destroyed) first.");
        public ToCordysCommand() { 
            super("[options] <wsi file>", "create a wsi on the given system and organization");
            setSyntax("[options] <wsi file>" + "\n" + cli.getSyntax("                "));
        }
        @Override public void run(String[] args) {
            args = checkArgs(args);
            Organization org = getOrg();
            WsiDef wsidef= new WsiDef(args[0]);
            wsidef.update(org, clearExistingOption.isSet());
        }
    };

    private class FromCordysCommand extends SysOrgCommand {
        public FromCordysCommand() { super("[options] <wsiName> <wsi file>", "extract a wsi file from the given system and organization"); }
        @Override public void run(String[] args) {
            args = checkArgs(args);
            String wsiName=args[0];
            String fileName=wsiName+".wsi";
            if (args.length>1)
                fileName=args[1];
            Organization org = getOrg();
            WsiDef wsidef= new WsiDef(org.webServiceInterfaces.getByName(wsiName));
            FileUtil.saveString(new File(fileName), wsidef.toXml().getPretty());
        }
    };

    
    Command toCordys   = new ToCordysCommand(); 
    Command fromCordys = new FromCordysCommand(); 
    public DevelopCommand(){
        super("caas dev", "run a caas development command");
        commands.put("toCordys", toCordys);
        commands.put("fromCordys", fromCordys);
    }


}
