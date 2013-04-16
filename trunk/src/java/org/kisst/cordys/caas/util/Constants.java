package org.kisst.cordys.caas.util;

import java.util.regex.Pattern;

public class Constants
{
    /** The name of the -D property to tell CAAS where to get the caas.conf from */
    public static final String CAAS_CONF_LOCATION = "caas.conf.location";
    public static final String GET_INSTALLATION_INFO = "GetInstallationInfo";
    public static final String GET_XML_OBJECT = "GetXMLObject";
    public static final String UPLOAD_ISVP = "UploadISVPackage";
    public static final String UPLOAD_CAP = "UploadCAP";
    public static final String GET_NEW_CAP_SUMMARY = "GetNewCapSummary";
    public static final String DEPLOY_CAP = "DeployCAP";
    public static final String GET_CAP_DEPLOYMENT_DETAILS = "GetCapDeploymentDetails";
    public static final String GET_DEPLOYED_CAP_SUMMARY = "GetDeployedCapSummary";
    public static final String UNDEPLOY_CAP = "UnDeployCAP";
    public static final String LOAD_ISVP = "LoadISVPackage";
    public static final String UPGRADE_ISVP = "UpgradeISVPackage";
    public static final String GET_PROPERTY = "GetProperty";
    public static final String LIST_ISVPS = "ListISVPackages";
    public static final String UNLOAD_ISVP = "UnloadISVPackage";
    public static final String GET_ISVP_DEFINITION = "GetISVPackageDefinition";
    public static final String GET_LDAP_OBJECT = "GetLDAPObject";
    public static final String GET_ALL_PROCESS_MODELS = "GetAllProcessModels";
    public static final String DELETE_PROCESS_MODEL = "DeleteProcessModel";
    public static final String START = "Start";
    public static final String STOP = "Stop";
    public static final String RESTART = "Restart";
    public static final String LIST = "List";
    public static final String APPEND_XML_OBJECT = "AppendXMLObject";
    public static final String UPDATE_XML_OBJECT = "UpdateXMLObject";
    public static final String GET_COLLECTION = "GetCollection";
    public static final String GET_CHILDREN = "GetChildren";
    public static final String UPDATE = "Update";
    public static final String DELETE_RECURSIVE = "DeleteRecursive";
    public static final String GET_INSTALLED_ISVPS = "GetInstalledISVPackages";
    public static final String GET_CAP_DETAILS = "GetCapDetails";
    public static final String INITIALIZE_ASSIGNMENT_ROOT = "InitializeAssignmentRoot";
    public static final String GET_UNITS_FOR_ASSIGNMENT = "GetUnitsForAssignments";
    public static final String GET_ASSIGNMENTS = "GetAssignments";
    public static final String GET_WORKLISTS = "GetWorklists";
    public static final String GET_WORKLISTS_BY_TEAM = "GetWorklistsByTeam";
    public static final String GET_USERS_BY_WORKLIST = "GetUsersByWorklist";
    public static final String GET_UNIT_ROLES = "GetUnitRoles";
    public static final String SEARCH_LDAP = "SearchLDAP";

    public static final String XMLNS_TEMPLATE = "http://caas.kisst.org/2.0/template";
    public final static String XMLNS_MONITOR = "http://schemas.cordys.com/1.0/monitor";
    public final static String XMLNS_LDAP = "http://schemas.cordys.com/1.0/ldap";
    public final static String XMLNS_ISV = "http://schemas.cordys.com/1.0/isvpackage";
    public final static String XMLNS_XMLSTORE = "http://schemas.cordys.com/1.0/xmlstore";
    public final static String XMLNS_COBOC = "http://schemas.cordys.com/1.0/coboc";
    public final static String XMLNS_NOTIFICATION = "http://schemas.cordys.com/1.0/notification";
    public final static String XMLNS_CAP = "http://schemas.cordys.com/cap/1.0";
    public static final String XMLNS_USER_ASSIGNMENT = "http://schemas.cordys.com/userassignment/UserAssignmentService/1.0";
    public static final String XMLNS_NOTIFICATION_WORKFLOW = "http://schemas.cordys.com/notification/workflow/1.0";

    public static final String[][] NS = new String[][] { { "mon", XMLNS_MONITOR }, { "ldap", XMLNS_LDAP }, { "isv", XMLNS_ISV },
            { "xs", XMLNS_XMLSTORE }, { "coboc", XMLNS_COBOC }, { "cap", XMLNS_CAP }, { "ua", XMLNS_USER_ASSIGNMENT },
            { "nw", XMLNS_NOTIFICATION_WORKFLOW } };
    
    /** Holds the regex to get the CN of the current entry. */
    public static final Pattern GET_CN = Pattern.compile("^cn=([^,]+)");

}
