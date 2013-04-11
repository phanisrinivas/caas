package org.kisst.cordys.caas;

import java.util.ArrayList;
import java.util.List;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.support.XmlProperty;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class wraps the information that a worklist contains.
 */
public class Worklist extends CordysObject
{

    /**
     * The space in which a team can be defined.
     */
    public enum ESpace
    {
        Organization, ISV
    };

    /** Holds the organization in which the worklist resides. */
    private Organization m_org;
    /** Holds the XML data for this worklist. */
    private XmlNode m_worklistData;
    /** Holds the fully qualified name of the worklist. */
    public final XmlProperty<String> qname;
    /** Holds the space if which the worklist is defined. */
    public final XmlProperty<String> space;
    /** Holds the name of the worklist. */
    public final XmlProperty<String> worklistName;
    /** Holds the ID of the worklist. */
    public final XmlProperty<String> id;
    /** Holds the last modified date of the worklist. */
    public final XmlProperty<Long> lmd;
    /** Holds the acl of the worklist. */
    public final XmlProperty<String> acl;
    /** Holds the execution order of the worklist. */
    public final XmlProperty<String> executionOrder;
    /** Holds the users that are attached to this worklist */
    public final WorklistUsers users;
    /** Alias for the users */
    public final WorklistUsers user;
    /** Alias for the users */
    public final WorklistUsers u;

    /**
     * Instantiates a new worklist.
     * 
     * @param org The parent organization
     * @param worklist The fqn
     */
    public Worklist(Organization org, XmlNode worklist)
    {
        m_org = org;
        m_worklistData = worklist.clone();

        id = new XmlProperty<String>(m_worklistData, "", "Id", "nw", Constants.NS, String.class);
        worklistName = new XmlProperty<String>(m_worklistData, "", "Name", "nw", Constants.NS, String.class);
        qname = new XmlProperty<String>(m_worklistData, "", "QName", "nw", Constants.NS, String.class);
        lmd = new XmlProperty<Long>(m_worklistData, "", "LastModified", "nw", Constants.NS, Long.class);
        acl = new XmlProperty<String>(m_worklistData, "", "Deleted", "nw", Constants.NS, String.class);
        space = new XmlProperty<String>(m_worklistData, "", "Organization", "nw", Constants.NS, String.class);
        executionOrder = new XmlProperty<String>(m_worklistData, "", "ExecutionOrderPolicy", "nw", Constants.NS, String.class);

        users = new WorklistUsers(org, this);
        user = users;
        u = users;
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
        return m_org.getSystem();
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
        return qname.get();
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
        return m_org.getVarName() + ".worklists.\"" + qname.get() + "\"";
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
        return qname.get();
    }

    /**
     * This method gets the organization to which this team is bound.
     * 
     * @return The organization to which this team is bound.
     */
    public Organization getOrganization()
    {
        return m_org;
    }

    /**
     * This method gets the XML data for this worklist.
     * 
     * @return The XML data for this worklist.
     */
    public XmlNode getWorklistData()
    {
        return m_worklistData;
    }

    /**
     * This method sets the XML data for this worklist.
     * 
     * @param worklistData The XML data for this worklist.
     */
    public void setWorklistData(XmlNode worklistData)
    {
        m_worklistData = worklistData;
    }

    /**
     * Holds the list of all the worklists in the organization.
     * 
     * @param <T> The generic type
     */
    public static class WorklistList<T extends CordysObject> extends CordysObjectList<Worklist>
    {

        /** Holds the organization to get the worklists for. */
        private Organization m_organization;

        /** Holds the parent object. */
        private T m_parent;

        /**
         * Instantiates a new list.
         * 
         * @param organization The organization
         */
        @SuppressWarnings("unchecked")
        protected WorklistList(Organization organization)
        {
            this(organization, (T) organization);
        }

        /**
         * Instantiates a new list.
         * 
         * @param organization The organization
         * @param parent The parent
         */
        protected WorklistList(Organization organization, T parent)
        {
            super(organization.getSystem());
            m_organization = organization;
            m_parent = parent;
        }

        /**
         * Retrieve list.
         * 
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            // Get all the worklists that are accessible via this organization
            List<XmlNode> worklists = null;
            if (m_parent instanceof Organization)
            {
                XmlNode request = new XmlNode(Constants.GET_WORKLISTS, Constants.XMLNS_NOTIFICATION_WORKFLOW);
                request.add("Version").setText("All");

                XmlNode response = m_organization.call(request);

                worklists = response.xpath("//nw:GetWorklistsResponse/nw:tuple/nw:old/nw:Worklist", Constants.NS);

            }
            else if (m_parent instanceof Team)
            {
                XmlNode request = new XmlNode(Constants.GET_WORKLISTS_BY_TEAM, Constants.XMLNS_NOTIFICATION_WORKFLOW);
                request.add("TeamId").setText(((Team) m_parent).id.get());

                XmlNode response = m_organization.call(request);

                worklists = response.xpath("//nw:GetWorklistsByTeamResponse/nw:tuple/nw:old/nw:Worklist", Constants.NS);
            }

            if (worklists != null)
            {
                for (XmlNode worklist : worklists)
                {
                    Worklist t = new Worklist(m_organization, worklist);
                    grow(t);
                }
            }
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
            return m_organization.getVarName() + ".worklists";
        }

        /**
         * This method gets the organization.
         * 
         * @return The organization
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return m_organization;
        }

        /**
         * This method will search for the worklist with the given Unit ID.
         * 
         * @param worklistId The ID of the worklist to be found.
         * @return The worklist with the given ID.
         */
        public Worklist findByWorklistID(String worklistId)
        {
            Worklist retVal = null;

            // Make sure the list is available
            ArrayList<Worklist> allWorklists = fetchList();

            for (Worklist worklist : allWorklists)
            {
                if (worklist.id.get().equals(worklistId))
                {
                    retVal = worklist;
                    break;
                }
            }

            return retVal;
        }
    }

    /**
     * Holds the Class WorklistUsers.
     */
    private static class WorklistUsers extends CordysObjectList<User>
    {

        /** Holds the m_org. */
        private final Organization m_organization;

        /** Holds the m_worklist. */
        private final Worklist m_worklist;

        /**
         * Instantiates a new worklist users.
         * 
         * @param org The org
         * @param w The w
         */
        protected WorklistUsers(Organization org, Worklist w)
        {
            super(org.getSystem());
            m_organization = org;
            m_worklist = w;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            XmlNode request = new XmlNode(Constants.GET_USERS_BY_WORKLIST, Constants.XMLNS_NOTIFICATION_WORKFLOW);
            request.add("WorklistId").setText(m_worklist.id.get());

            XmlNode response = m_organization.call(request);

            List<XmlNode> users = response.xpath("//nw:GetUsersByWorklistResponse/nw:tuple/nw:old/nw:User/nw:UserDN", Constants.NS);

            for (XmlNode user : users)
            {
                String dn = user.getText();

                User u = m_organization.users.get(StringUtil.getCN(dn));
                if (u != null)
                {
                    grow(u);
                }
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return m_organization;
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return m_worklist.getKey() + ".users";
        }
    }
}
