package org.kisst.cordys.caas;

import java.util.Date;
import java.util.List;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.XmlProperty;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This object wraps an assignment of a user to a team.
 * 
 * @author pgussow
 */
public class Assignment<T extends CordysObject> extends CordysObject
{
    /** Holds the parent team to which this assignment is connected. */
    private T m_parent;
    /** Holds the XML data for this assignment. */
    private XmlNode m_assignmentData;
    /** Holds the ID of the assignment. */
    public final XmlProperty<String> id;
    /** Holds the user DN of the assignment. */
    public final XmlProperty<String> userDn;
    /** Holds the date on which the assignment is effective. */
    public final XmlProperty<Date> effectiveDate;
    /** Holds the date on which the assignement stops. */
    public final XmlProperty<Date> finishDate;
    /** Holds whether or not this assignment is the user's principal team. */
    public final XmlProperty<Boolean> isPrincipal;
    /** Holds whether or not this assignment is currently effective. */
    public final XmlProperty<Boolean> isEffective;
    /** Holds the ID of the unit. */
    public final XmlProperty<String> unitId;
    /** Holds the DN of the role. */
    public final XmlProperty<String> roleDn;
    /** Holds the ID of the role within the unit. */
    public final XmlProperty<String> unitRoleId;
    /** Holds whether or not this is a lead role. */
    public final XmlProperty<Boolean> isLead;
    /** Holds the ID of the assignment root. */
    public final XmlProperty<String> assignmentRoot;
    /** Holds the actual team that is linked to this assignment */
    public final Team team;
    /** Holds the actual user this assignment applies to */
    public final User user;
    /** Holds the role this assignment applies to */
    public final Role role;

    /**
     * Instantiates a new team.
     * 
     * @param parent The parent
     * @param assignment The assignment data
     */
    public Assignment(T parent, XmlNode assignment)
    {
        m_parent = parent;
        m_assignmentData = assignment.clone();

        id = new XmlProperty<String>(m_assignmentData, "", "ID", "ua", Constants.NS, String.class);
        userDn = new XmlProperty<String>(m_assignmentData, "", "UserDN", "ua", Constants.NS, String.class);
        effectiveDate = new XmlProperty<Date>(m_assignmentData, "", "EffectiveDate", "ua", Constants.NS, Date.class);
        finishDate = new XmlProperty<Date>(m_assignmentData, "", "FinishDate", "ua", Constants.NS, Date.class);
        isPrincipal = new XmlProperty<Boolean>(m_assignmentData, "", "IsPrincipalUnit", "ua", Constants.NS, Boolean.class);
        isEffective = new XmlProperty<Boolean>(m_assignmentData, "", "IsEffective", "ua", Constants.NS, Boolean.class);
        unitId = new XmlProperty<String>(m_assignmentData, "", "UnitID", "ua", Constants.NS, String.class);
        roleDn = new XmlProperty<String>(m_assignmentData, "", "RoleDN", "ua", Constants.NS, String.class);
        unitRoleId = new XmlProperty<String>(m_assignmentData, "", "UnitRoleID", "ua", Constants.NS, String.class);
        isLead = new XmlProperty<Boolean>(m_assignmentData, "", "IsLeadRole", "ua", Constants.NS, Boolean.class);
        assignmentRoot = new XmlProperty<String>(m_assignmentData, "", "AssignmentRoot", "ua", Constants.NS, String.class);

        // The name of the team is not returned. So it would be a nice to know the team. This also applies to the user. So we'll
        // look up those objects within the current organization
        team = getOrganization().teams.findByUnitID(unitId.get());
        user = getOrganization().users.findByDn(userDn.get());

        Role tmp = null;
        if (!StringUtil.isEmptyOrNull(unitRoleId.get()))
        {
            tmp = getOrganization().roles.get(unitRoleId.get());
        }
        
        if (tmp == null && (roleDn != null && !StringUtil.isEmptyOrNull(roleDn.get())))
        {
            tmp = (Role) getSystem().getLdap(roleDn.get());
        }
        
        role = tmp;
    }

    /**
     * This method gets the actual team for this assignment.
     * 
     * @return The actual team for this assignment.
     */
    public Team getTeam()
    {
        return team;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return m_parent.getSystem();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return m_parent.getOrganization();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        String retVal = "";

        // Depending on the parent we need to decide the name. If the parent is a user, then the name is the team name. If the
        // parent is the team then the name is the user.
        if (m_parent instanceof User)
        {
            retVal = team.teamName.get();
        }
        else if (m_parent instanceof Team)
        {
            retVal = user.getCn();
        }

        return retVal;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return m_parent.getVarName() + ".assignments.\"" + getName() + "\"";
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return id.get();
    }
    
    /**
     * This method gets the plain username of the assignment.
     * 
     * @return The plain username of the assignment.
     */
    public String getUsername()
    {
        String retVal = null;
        
        if (userDn != null && !StringUtil.isEmptyOrNull(userDn.get()))
        {
            retVal = StringUtil.getCN(userDn.get());
        }
        
        return retVal;
    }

    /**
     * Holds the Class AssignmentList. The parent can be either a user or a team.
     */
    public static class AssignmentList<T extends CordysObject> extends CordysObjectList<Assignment<T>>
    {
        /** Holds the unit (team) id to filter on */
        private String m_unitId = "";
        /** Holds the role dn to filter on */
        private String m_roleDn = "";
        /** Holds the user dn to filter on */
        private String m_userDn = "";
        /** Holds the additional filter to use */
        private String m_filter = "";
        /** Holds the organizational context */
        private final Organization m_org;
        /** Holds the parent object for this assignment */
        private T m_parent;

        /**
         * Instantiates a new assignment list.
         * 
         * @param org The organization context for this assignment.
         * @param unitId The unit id (in other words: the ID for the team).
         * @param roleDn The role dn
         * @param userDn The user dn
         * @param filter The filter to use
         */
        public AssignmentList(T parent)
        {
            super(parent.getSystem());
            m_parent = parent;

            // Check the type of the parent.
            if (parent instanceof Team)
            {
                Team t = (Team) parent;
                m_org = t.getOrganization();
                m_unitId = t.id.get();
            }
            else if (parent instanceof User)
            {
                User u = (User) parent;
                m_org = u.getOrganization();
                m_userDn = u.getDn();
            }
            else
            {
                throw new CaasRuntimeException("Invalid parent for an assignment: only Team or User are supported");
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return m_parent.getOrganization();
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // Get all the teams that are accessible via this organization
            XmlNode request = new XmlNode(Constants.GET_ASSIGNMENTS, Constants.XMLNS_USER_ASSIGNMENT);
            request.add("WorkspaceID").setText("__Organization Staging__");
            request.add("AssignmentRoot").setText(m_org.getAssignmentRoot());
            request.add("UnitID").setText(m_unitId);
            request.add("RoleDN").setText(m_roleDn);
            request.add("UserDN").setText(m_userDn);
            request.add("Filter").setText(m_filter);
            request.add("cursor").setText("");
            request.add("EffectiveOnly").setText("false");

            XmlNode response = m_org.call(request);

            List<XmlNode> units = response.xpath("//ua:GetAssignments/ua:dataset/ua:tuple/ua:old/ua:Assignment", Constants.NS);
            for (XmlNode unit : units)
            {
                Assignment<T> a = new Assignment<T>(m_parent, unit);
                grow(a);
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return m_parent.getKey() + ".assignments";
        }
    }
}
