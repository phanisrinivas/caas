package org.kisst.cordys.caas.template;

/**
 * This class defines the template content types.
 */
public enum ETemplateOption
{
    ALL("All", 'a'), NON_CORDYS_PACKAGES("Non-Cordys owned packages", 'p'), DSO("Datasource objects", 'd'), XML_STORE_OBJECTS(
            "XML Store objects", 'x'), ROLES("Organizational roles", 'r'), USERS("Organizational users", 'u'), SERVICE_GROUPS(
            "Service groups, containers and connection points", 's');

    /** Holds the description of the option */
    private String m_description;
    /** Holds the character enabling this option */
    private char m_option;

    /**
     * Instantiates a new e template option.
     * 
     * @param description The description
     * @param option The option
     */
    ETemplateOption(String description, char option)
    {
        m_description = description;
        m_option = option;
    }

    /**
     * This method gets the description.
     * 
     * @return The description.
     */
    public String description()
    {
        return m_description;
    }

    /**
     * This method gets the option to use in the commandline.
     * 
     * @return The option to use in the commandline.
     */
    public char option()
    {
        return m_option;
    }

    /**
     * This method returns a list with all the options that can be set
     * 
     * @param prefix The prefix for the line.
     * @return The options in a descriptive manner.
     */
    public static String options(String prefix)
    {
        StringBuilder sb = new StringBuilder(1024);

        for (ETemplateOption to : ETemplateOption.values())
        {
            sb.append(prefix).append(to.m_option).append(": ").append(to.description()).append("\n");
        }

        return sb.toString();
    }
}