package org.kisst.cordys.caas;

import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.XmlProperty;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This object wraps an assignment of a user to a team.
 * 
 * @author pgussow
 */
public class Assignment extends CordysObject
{
    /** Holds the parent team to which this assignment is connected. */
    private Team m_parent;
    /** Holds the XML data for this assignment. */
    private XmlNode m_assignmentData;
    /** Holds the ID of the assignment. */
    public final XmlProperty<String> id;
    /** Holds the user DN of the assignment. */
    public final XmlProperty<String> user;
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
    public final XmlProperty<String> role;
    /** Holds the ID of the role within the unit. */
    public final XmlProperty<String> unitRoleId;
    /** Holds whether or not this is a lead role. */
    public final XmlProperty<Boolean> isLead;
    /** Holds the ID of the assignment root. */
    public final XmlProperty<String> assignmentRoot;

    /**
     * Instantiates a new team.
     * 
     * @param parent The parent
     * @param assignment The assignment data
     */
    public Assignment(Team parent, XmlNode assignment)
    {
        m_parent = parent;
        m_assignmentData = assignment.clone();

        id = new XmlProperty<String>(m_assignmentData, "", "ID", "ua", Constants.NS, String.class);
        user = new XmlProperty<String>(m_assignmentData, "", "UserDN", "ua", Constants.NS, String.class);
        effectiveDate = new XmlProperty<Date>(m_assignmentData, "", "EffectiveDate", "ua", Constants.NS, Date.class);
        finishDate = new XmlProperty<Date>(m_assignmentData, "", "FinishDate", "ua", Constants.NS, Date.class);
        isPrincipal = new XmlProperty<Boolean>(m_assignmentData, "", "IsPrincipalUnit", "ua", Constants.NS, Boolean.class);
        isEffective = new XmlProperty<Boolean>(m_assignmentData, "", "IsEffective", "ua", Constants.NS, Boolean.class);
        unitId = new XmlProperty<String>(m_assignmentData, "", "UnitID", "ua", Constants.NS, String.class);
        role = new XmlProperty<String>(m_assignmentData, "", "RoleDN", "ua", Constants.NS, String.class);
        unitRoleId = new XmlProperty<String>(m_assignmentData, "", "UnitRoleID", "ua", Constants.NS, String.class);
        isLead = new XmlProperty<Boolean>(m_assignmentData, "", "IsLeadRole", "ua", Constants.NS, Boolean.class);
        assignmentRoot = new XmlProperty<String>(m_assignmentData, "", "AssignmentRoot", "ua", Constants.NS, String.class);
    }

    /**
     * This method gets the system.
     * 
     * @return The system
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return m_parent.getSystem();
    }

    /**
     * This method gets the name.
     * 
     * @return The name
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        String retVal = user.get();
        
        //A user is only assigned once to the same team
        Matcher m = Constants.GET_CN.matcher(retVal);
        if (m.find())
        {
            retVal = m.group(1);
        }
        
        return retVal;
    }

    /**
     * This method gets the var name.
     * 
     * @return The var name
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return m_parent.getVarName() + ".assignments.\"" + getName() + "\"";
    }

    /**
     * This method gets the key.
     * 
     * @return The key
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return id.get();
    }

    /**
     * Holds the Class AssignmentList.
     */
    public static class AssignmentList extends CordysObjectList<Assignment>
    {
        /** Holds the parent team to get the assignments for */
        private Team m_team;

        /**
         * Instantiates a new list.
         * 
         * @param organization The organization
         */
        protected AssignmentList(Team team)
        {
            super(team.getSystem());
            m_team = team;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            Organization org = m_team.getOrganization();

            // Get all the teams that are accessible via this organization
            XmlNode request = new XmlNode(Constants.GET_ASSIGNMENTS, Constants.XMLNS_USER_ASSIGNMENT);
            request.add("WorkspaceID").setText("__Organization Staging__");
            request.add("AssignmentRoot").setText(org.getAssignmentRoot());
            request.add("UnitID").setText(m_team.id.get());
            request.add("RoleDN").setText("");
            request.add("UserDN").setText("");
            request.add("Filter").setText("");
            request.add("cursor").setText("");
            request.add("EffectiveOnly").setText("false");

            XmlNode response = org.call(request);

            List<XmlNode> units = response.xpath("//ua:GetAssignments/ua:dataset/ua:tuple/ua:old/ua:Assignment", Constants.NS);
            for (XmlNode unit : units)
            {
                Assignment a = new Assignment(m_team, unit);
                grow(a);
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return m_team.getKey() + ".assignments";
        }
    }
}
