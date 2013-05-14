package org.kisst.cordys.caas.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.cm.Template;
import org.kisst.cordys.caas.cm.Template.ETemplateOption;
import org.kisst.cordys.caas.util.FileUtil;

/**
 * <p>
 * This class wraps around the Template option. This used to be part of the CmCommand, but it has been taken separately because of
 * the different functional role it plays.
 * </p>
 * <p>
 * The template is meant for applying a full set of configuration to an organization (thus a single-file-per-organization), while
 * the CmCommand is meant for a 'file-per-package' strategy in which the ccm file describes what a certain package adds to the
 * configuration.
 * </p>
 * 
 * @author pgussow
 */
public class TemplateCommand extends CompositeCommand
{
    /**
     * This command will create the template based on the configured system and organization.
     */
    private Command create = new HostCommand("[options] <template file>", "create a template based on the given organization") {

        private final Cli.StringOption isvpName = cli.stringOption("i", "isvpName", "the isvpName to use for custom content",
                null);

        /**
         * @see org.kisst.cordys.caas.main.CommandBase#run(java.lang.String[])
         */
        @Override
        public void run(String[] args)
        {
            args = checkArgs(args);

            // Create the template for the configured organization
            String orgz = System.getProperty("template.org");
            Organization organization = getOrg(orgz);
            Template templ = new Template(organization, isvpName.get(), getOptions());

            // Load the properties for the given organization
            Map<String, String> variables = Environment.get().loadSystemProperties(getSystem().getName(), organization.getName());

            // Add the organization name, system name and LDAP root to the map
            variables.put("sys.org.name", organization.getName());
            variables.put("sys.ldap.root", organization.getSystem().getDn());
            variables.put("sys.name", this.getSystem().getName());

            // Save the template
            templ.save(args[0], variables);
        }
    };
    /**
     * This method will apply the template to the given system and organization.
     */
    private Command apply = new HostCommand("[options] <template file>",
            "create elements in an organization based on the given template") {
        @Override
        public void run(String[] args)
        {
            args = checkArgs(args);
            Template templ = new Template(FileUtil.loadString(args[0]), getOptions());

            // Get the organization in which the template should be applied.
            String orgz = System.getProperty("create.org");
            Organization organization = getOrg(orgz);

            // Load the properties for the given organization.
            Map<String, String> map = Environment.get().loadSystemProperties(this.getSystem().getName(), organization.getName());

            // Add the organization name, system name and LDAP root to the map
            map.put("sys.org.name", organization.getName());
            map.put("sys.ldap.root", organization.getSystem().getDn());
            map.put("sys.name", this.getSystem().getName());

            // Apply the template to the given organization using the given properties.
            templ.apply(organization, map);
        }
    };

    /**
     * Instantiates a new cm command.
     * 
     * @param name The name
     */
    public TemplateCommand()
    {
        super("caas template", "run a caas configuration manager command");

        commands.put("apply", apply);
        commands.put("create", create);
    }

    /**
     * The base host command class. It parses the command's options.
     */
    private abstract class HostCommand extends CommandBase
    {
        /** Holds the command line interface to use */
        protected final Cli cli = new Cli();
        /** Holds the option that specifies the system that we should connect to. */
        protected final Cli.StringOption systemOption = cli.stringOption("s", "system", "the system to use", null);
        /** Holds the name of the organization to connect to */
        protected final Cli.StringOption orgOption = cli.stringOption("o", "organization", "the organization to use", null);
        /** Holds the option that allows the user to specify which types they want to process */
        protected final Cli.StringOption compOption = cli.stringOption("c", "component",
                "the components that should be processed. Valid options", null);

        /**
         * Instantiates a new host command.
         * 
         * @param usage The usage
         * @param summary The summary
         */
        public HostCommand(String usage, String summary)
        {
            super(usage, summary);
        }

        /**
         * This method gets the options valid for the template.
         * 
         * @return The options valid for the template.
         */
        public List<ETemplateOption> getOptions()
        {
            // Build up the list of the components that should be exported.
            List<Template.ETemplateOption> options = new ArrayList<Template.ETemplateOption>();
            if (!compOption.isSet() || compOption.get().indexOf(Template.ETemplateOption.ALL.option()) > -1)
            {
                // If the option is not set OR that the all is specified we do everything
                options.add(ETemplateOption.ALL);
            }
            else
            {
                String tmp = compOption.get();
                for (ETemplateOption o : Template.ETemplateOption.values())
                {
                    if (tmp.indexOf(o.option()) > -1)
                    {
                        options.add(o);
                    }
                }
            }

            return options;
        }

        /**
         * This method gets the system.
         * 
         * @return The system
         */
        protected CordysSystem getSystem()
        {
            return Caas.getSystem(Caas.defaultSystem);
        }

        /**
         * This method gets the organization that should be used for this template command.
         * 
         * @param defaultOrg The default org to use if the orgOption is not set.
         * @return The organziation to use.
         */
        protected Organization getOrg(String defaultOrg)
        {
            if (orgOption.isSet())
            {
                return getSystem().org.getByName(orgOption.get());
            }
            else
            {
                return getSystem().org.getByName(defaultOrg);
            }
        }

        /**
         * This method will check if the Cordys system to use is set. If it is not set it will use the default system from the
         * caas.conf.
         * 
         * @param args The args
         * @return The string[]
         */
        protected String[] checkArgs(String[] args)
        {
            args = cli.parse(args);
            if (systemOption.isSet())
                Caas.defaultSystem = systemOption.get();
            return args;
        }

        /**
         * @see org.kisst.cordys.caas.main.CommandBase#getHelp()
         */
        @Override
        public String getHelp()
        {
            return "\nOPTIONS\n" + cli.getSyntax("\t");
        }
    }

    /**
     * This method returns the Apply command. This is used for backwards compatibility
     * 
     * @return The apply command.
     */
    public Command getApplyCommand()
    {
        return apply;
    }

    /**
     * This method returns the Create command. This is used for backwards compatibility
     * 
     * @return The create command.
     */
    public Command getCreateCommand()
    {
        return create;
    }
}
