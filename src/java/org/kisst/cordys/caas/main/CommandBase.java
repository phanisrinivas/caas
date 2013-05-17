/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.main;

/**
 * Holds the Class CommandBase.
 */
abstract public class CommandBase implements Command
{
    /** Holds the usage string for this command. */
    private String usage;
    /** Holds the summary for this command. */
    private String summary;

    /**
     * Instantiates a new command base.
     * 
     * @param usage The usage
     * @param summary The summary
     */
    public CommandBase(String usage, String summary)
    {
        this.usage = usage;
        this.summary = summary;
    }

    /**
     * @see org.kisst.cordys.caas.main.Command#run(java.lang.String[])
     */
    abstract public void run(String[] args);

    /**
     * @see org.kisst.cordys.caas.main.Command#getSyntax()
     */
    public String getSyntax()
    {
        return usage;
    }

    /**
     * This method sets the syntax.
     * 
     * @param usage The new syntax
     */
    protected void setSyntax(String usage)
    {
        this.usage = usage;
    }

    /**
     * This method sets the summary.
     * 
     * @param summary The new summary
     */
    protected void setSummary(String summary)
    {
        this.summary = summary;
    }

    /**
     * @see org.kisst.cordys.caas.main.Command#getHelp()
     */
    public String getHelp()
    {
        return null;
    }

    /**
     * @see org.kisst.cordys.caas.main.Command#getSummary()
     */
    public String getSummary()
    {
        return summary;
    }

    /**
     * Check help.
     * 
     * @param prefix The prefix
     * @param args The args
     */
    public void checkHelp(String prefix, String[] args)
    {
        if (args.length == 0)
            return;
        if (args[0].equals("help") || args[0].equals("--help"))
            System.out.println(getHelp());
    }
}