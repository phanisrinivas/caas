package org.kisst.cordys.caas;

import java.util.List;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.XmlProperty;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class wraps the roles that a user can play in a certain team.
 */
public class TeamRole extends CordysObject
{
    /** Holds the team in which the unit role resides */
    private Team m_team;
    /** Holds the XML data for this unit role. */
    private XmlNode m_unitRoleData;
    /** Holds the name of the unit role. */
    public final XmlProperty<String> roleName;
    /** Holds the space if which the unit role is defined. */
    public final XmlProperty<String> roleDn;
    /** Holds the description of the unit role. */
    public final XmlProperty<String> description;
    /** Holds the ID of the team. */
    public final XmlProperty<String> unitId;
    /** Holds whether or not the unti role it the leader role. */
    public final XmlProperty<Boolean> leader;

    /**
     * Instantiates a new team role.
     * 
     * @param org The org
     * @param unitRole The unit role
     */
    public TeamRole(Team team, XmlNode unitRole)
    {
        m_team = team;
        m_unitRoleData = unitRole.clone();

        roleName = new XmlProperty<String>(m_unitRoleData, "", "Name", "ua", Constants.NS, String.class);
        roleDn = new XmlProperty<String>(m_unitRoleData, "", "RoleDN", "ua", Constants.NS, String.class);
        description = new XmlProperty<String>(m_unitRoleData, "", "Description", "ua", Constants.NS, String.class);
        unitId = new XmlProperty<String>(m_unitRoleData, "", "UnitID", "ua", Constants.NS, String.class);
        leader = new XmlProperty<Boolean>(m_unitRoleData, "", "IsLeader", "ua", Constants.NS, Boolean.class);
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return roleName.get();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return roleName.get();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return m_team.getOrganization();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return m_team.getSystem();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return m_team.getVarName() + ".roles." + getName();
    }

    /**
     * Holds the list of all the teams in the organization.
     */
    public static class TeamRoleList extends CordysObjectList<TeamRole>
    {
        /** Holds the team for which to get the unit roles */
        private Team m_team;

        /**
         * Instantiates a new team role list.
         * 
         * @param team The team
         */
        protected TeamRoleList(Team team)
        {
            super(team.getOrganization().getSystem());
            m_team = team;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // Get all the teams that are accessible via this organization
            XmlNode request = new XmlNode(Constants.GET_UNIT_ROLES, Constants.XMLNS_USER_ASSIGNMENT);
            request.add("WorkspaceID").setText("__Organization Staging__");
            request.add("UnitID").setText(m_team.id.get());

            XmlNode response = m_team.getOrganization().call(request);

            List<XmlNode> unitRoles = response.xpath("//ua:GetUnitRoles/ua:dataset/ua:tuple/ua:old/ua:UnitRole", Constants.NS);
            for (XmlNode unitRole : unitRoles)
            {
                TeamRole t = new TeamRole(m_team, unitRole);
                grow(t);
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return m_team.getVarName() + ".roles";
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return m_team.getOrganization();
        }
    }
}
