/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.util.Constants;

public class CaasMainCommand extends CompositeCommand
{

    Cli cli = new Cli();
    Cli.Flag quiet = cli.flag("q", "quiet", "don't output anything unless errors happen");
    Cli.Flag verbose = cli.flag("v", "verbose", "be verbose about what you are doing");
    Cli.Flag trace = cli.flag("t", "trace", "if this option is set trace logging will be shown");
    Cli.Flag debug = cli.flag("d", "debug", "if this option is set debug logging will be shown");
    Cli.StringOption config = cli.stringOption("c", "config", "location of config file with connection properties", null);
    Cli.Flag showhelp = cli.flag("h", "help", "show this help information");
    Cli.Flag version = cli.flag(null, "version", "show the version information");

    private class GroovyCommand extends CompositeCommand
    {
        public GroovyCommand()
        {
            super("caas groovy", "run either a interactive groovy shell or a groovy script");
            this.commands.put("run", new GroovyRunScript());
            this.commands.put("shell", new GroovyShell());
        }
    }

    public CaasMainCommand()
    {
        super("caas", "run any of the caas subcommands");
        commands.put("shell", new GroovyShell());
        commands.put("run", new GroovyRunScript());
        commands.put("cm", new CmCommand("cm"));
        commands.put("groovy", new GroovyCommand());
        commands.put("log", new LogCommand());
        commands.put("setup", new SetupCommand());
    }

    /**
     * @see org.kisst.cordys.caas.main.CompositeCommand#getSyntax()
     */
    @Override
    public String getSyntax()
    {
        return "[options] " + super.getSyntax();
    }

    protected static String[] subArgs(String[] args, int pos)
    {
        String result[] = new String[args.length - pos];
        for (int i = pos; i < args.length; i++)
            result[i - pos] = args[i];
        return result;
    }

    /**
     * @see org.kisst.cordys.caas.main.CompositeCommand#getHelp()
     */
    @Override
    public String getHelp()
    {
        return super.getHelp() + "\nOPTIONS\n" + cli.getSyntax("\t");
    }

    /**
     * @see org.kisst.cordys.caas.main.CompositeCommand#run(java.lang.String[])
     */
    @Override
    public void run(String[] args)
    {
        args = cli.parse(args);

        // If the config file is specified we reload the environment with the specified preoprty file.
        if (config.isSet())
        {
            System.setProperty(Constants.CAAS_CONF_LOCATION, config.get());
        }

        // Initialize the environment
        if (debug.isSet())
        {
            Environment.debug = true;
        }

        if (verbose.isSet())
        {
            Environment.verbose = true;
        }

        if (trace.isSet())
        {
            Environment.trace = true;
        }

        if (quiet.isSet())
        {
            Environment.quiet = true;
        }

        if (version.isSet())
        {
            System.out.println(Caas.getVersion());
            return;
        }

        if (!Environment.quiet)
        {
            System.out.println("caas: Cordys Administration Automation Scripting, version " + Caas.getVersion());
        }

        if (showhelp.isSet())
        {
            help.run(args);
            return;
        }
        super.run(args);
    }
}
