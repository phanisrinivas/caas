/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import java.util.ArrayList;
import java.util.Date;

import org.kisst.cordys.caas.Assignment.AssignmentList;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.EntryObjectList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * This object holds the configuration information for a specific organizational user.
 */
public class User extends LdapObjectBase
{
    /** The authenticated user that is linked to this organizational user */
    public final RefProperty<AuthenticatedUser> authenticatedUser = new RefProperty<AuthenticatedUser>("authenticationuser");
    /** Alias for the authenticated user */
    public final RefProperty<AuthenticatedUser> au = authenticatedUser;
    /** Holds the roles that are assigned to this user */
    public final EntryObjectList<Role> roles = new EntryObjectList<Role>(this, "role");
    /** Alias for the roles */
    public final EntryObjectList<Role> role = roles;
    /** Alias for the roles */
    public final EntryObjectList<Role> r = roles;
    /** Holds the toolbars that are assigned. This is deprecated in BOP 4 and up */
    public final StringList toolbars = new StringList("toolbar");
    /** Holds the menus that are assigned. This is deprecated in BOP 4 and up */
    public final StringList menus = new StringList("menu");
    /** Holds the team assignments to this user */
    public final AssignmentList<User> assignments;
    /** Alias for the assignments */
    public final AssignmentList<User> assignment;
    /** Alias for the assignments */
    public final AssignmentList<User> a;

    /**
     * Instantiates a new user.
     * 
     * @param parent The parent
     * @param dn The dn
     */
    protected User(LdapObject parent, String dn)
    {
        super(parent, dn);

        // Create the assignment list
        assignments = new AssignmentList<User>(this);
        assignment = assignments;
        a = assignments;
    }

    /**
     * This method gets the organization to which this user belongs.
     * 
     * @return The organization to which this user belongs.
     */
    public Organization getOrganization()
    {
        if (getParent() instanceof Organization)
        {
            return (Organization) getParent();
        }

        return null;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#prefix()
     */
    @Override
    protected String prefix()
    {
        return "user";
    }

    /**
     * This method will assign the current user to the given team using the role. 
     * 
     * @param teamName The fully qualified name of the team to assign this user to
     * @param roleName The name of the role to use.
     * @param rolePackage The role package
     * @param principal The principal
     * @param effectiveDate The effective date
     * @param finidhDate The finidh date
     */
    public void assignToTeam(String teamName, String roleName, String rolePackage, boolean principal, Date effectiveDate,
            Date finidhDate)
    {
        Organization o = (Organization) getParent();

        // Find the actual team
        Team team = o.teams.get(teamName);
        if (team == null)
        {
            // Maybe they forgot to add the FQN of the team?
            StringBuilder sb = new StringBuilder(1024);
            sb.append("Could not find team with name ").append(teamName);

            for (Team tmp : o.teams)
            {
                if (tmp.getName().endsWith("/"))
                {
                    sb.append(". Did you mean ").append(tmp.getName()).append("?");
                }
            }

            throw new CaasRuntimeException(sb.toString());
        }

        // Find the actual role to play within the team
        Role role = null;
        if (StringUtil.isEmptyOrNull(rolePackage))
        {
            role = o.roles.get(roleName);
        }
        else
        {
            Package p = getSystem().packages.get(rolePackage);
            if (p == null)  
            {
                throw new CaasRuntimeException("Cannot find package with name " + rolePackage);
            }
            
            role = p.roles.get(roleName);
        }
        
        if (role == null)
        {
            throw new CaasRuntimeException("Cannot find role with name " + roleName + (StringUtil.isEmptyOrNull(rolePackage) ? "" : " in package " + rolePackage));
        }
        
        //Now we have all required information, so let's assign the team to the user.
        assignToTeam(team, role, principal, effectiveDate, finidhDate);
    }

    public void assignToTeam(Team t, Role r, boolean principal, Date effectiveDate, Date finidhDate)
    {

    }

    /**
     * Holds the list containing all the users for the organization.
     */
    public static class UserList extends ChildList<User>
    {
        /**
         * Instantiates a new user list.
         * 
         * @param o The o
         */
        public UserList(Organization o)
        {
            super(o, "cn=organizational users,", User.class);
        }

        /**
         * This method tries to find the user based on the full DN.
         * 
         * @param userDn The DN of the user to find.
         * @return The user for the given DN.
         */
        public User findByDn(String userDn)
        {
            User retVal = null;

            ArrayList<User> users = fetchList();

            for (User u : users)
            {
                if (u.getDn().equals(userDn))
                {
                    retVal = u;
                    break;
                }
            }

            return retVal;
        }
    }
}
