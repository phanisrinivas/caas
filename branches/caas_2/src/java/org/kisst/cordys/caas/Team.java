package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;

public class Team extends CordysObject
{
    /** Holds the organization in which the team resides */
    private Organization m_org;
    /** Holds the fully qualified name of the team */
    private String m_fqn;

    public Team(Organization org, String fqn)
    {
        m_org = org;
    }

    @Override
    public CordysSystem getSystem()
    {
        return m_org.getSystem();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return null;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return null;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return null;
    }

    /**
     * Holds the list of all the teams in the organization.
     */
    public static class List extends CordysObjectList<Team>
    {
        /** Holds the organization to get the teams for */
        private Organization m_organization;

        /**
         * Instantiates a new list.
         * 
         * @param organization The organization
         */
        protected List(Organization organization)
        {
            super(organization.getSystem());
            m_organization = organization;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
        }

        @Override
        public String getKey()
        {
            return null;
        }

    }

}
