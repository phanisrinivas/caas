package org.kisst.cordys.caas.cws

import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.*

import org.kisst.cordys.caas.helper.Role
import org.kisst.cordys.caas.helper.RoleList
import org.w3c.dom.NodeList

/**
 * This script will output the matrix which contains the runtime security definition of the project. So it
 * will output for each role which service it has access to. This includes direct and indirect access. This means 
 * that if a role has a sub role and that subrole has access to the service it is also displayed.
 * 
 * Limitation: only the ACL on the Web Service Interface is examined. Not the ACL of the nested operations.
 */
if (args.length == 0) {
    println ("Missing parameter: foldername");
    System.exit 1
}

def folder = new File(args[0]);

// Load all the roles.
def RoleList allRoles = new RoleList(folder);

//Filter out only the tech roles
def techRoles = allRoles.findAll({it.value.name.startsWith('exec-')})

def wsList = new ArrayList<String>();

// The result list will have an entry for each role. Then for each role there will be map containing the Runtime
// name of the web service and whether the role has direct, indirect or no access to the web service
def Map<Role, Map<String, String>> acl = new LinkedHashMap<Role, Map<String, String>>();
techRoles.each {
    acl.put(it.value, new LinkedHashMap<String, String>())
}

// Load all the web services that are available and build up the access map.
def builder     = DocumentBuilderFactory.newInstance().newDocumentBuilder()
def xpath = XPathFactory.newInstance().newXPath()
def delClos

delClos = {
    it.eachDir(delClos);
    it.eachFile {
        if (it.name.endsWith("#cws-wsds#.cws")) {

            def inputStream = new ByteArrayInputStream(it.text.bytes)
            def wsd = builder.parse(inputStream).documentElement

            def wsiName = xpath.evaluate("*[local-name()='Name']/text()", wsd)
            def wstype = xpath.evaluate("*[local-name()='Connectors']/*[local-name()='AppliconConnectorRuntime']/*[local-name()='Name']/text()", wsd)

            def bindings = xpath.evaluate(".//*[local-name()='WebserviceBindings']/*[local-name()='WebserviceBinding']", wsd, XPathConstants.NODESET)
            bindings.each { wsb ->
                //Get the fqn
                def fqn = it.parentFile.canonicalPath.substring(folder.canonicalPath.length() + 1).replaceAll('\\\\', '/')

                def rwsn = xpath.evaluate("*[local-name()='RuntimeWebserviceInterfaceName']/text()", wsb)
                rwsn = fqn + '/' + rwsn
                def docID = xpath.evaluate("*[local-name()='DocumentID']/text()", wsb)
                def name = xpath.evaluate("*[local-name()='Name']/text()", wsb)

                //Since the web service is found, we need to initialize the result matrix with this name with the default value 'NO_ACCESS'
                acl.each {
                    it.value.put(rwsn, "NO_ACCESS")
                }
                wsList.add(rwsn)

                //Figure out if runtime security is defined
                def NodeList nodes = xpath.evaluate("./*[local-name()='SecurityDescriptor']/*[local-name()='SecurityDescriptor']", wsb, XPathConstants.NODESET)
                def runtimeSec = 'No'
                if (nodes.length > 0) {
                    //Now we figure out if access is defined (presence of the 'rights' tag.
                    def directRoles = []
                    nodes.iterator().each {  node ->
                        def roleId = xpath.evaluate("*[local-name()='accesscontrolentries']/*[local-name()='ACE'][*[local-name()='rights']/*[local-name()='uri']]/*[local-name()='identity']/*[local-name()='uri']/@id", node)
                        if (roleId) {
                            def r = allRoles.get(roleId)
                            if (r) {
                                def lst = acl.get(r);
                                if (!lst) {
                                    println "--> ERROR: runtime security was defined on a non-exec role: ${r}!"
                                } else {
                                    lst.put(rwsn, 'DIRECT')
                                    //Add this role to the 'direct-access' list, so that it can be used to determine all the indirect lists.
                                    directRoles.add(r)
                                }
                            } else {
                                println "--> ERROR: no role found for ${rwsn}/${roleId}"
                            }
                        }
                    }

                    //Now all the direct access has been set. So now we need to check if there is indirect
                    //access because of the role hierarchy. For this we need to know the roles that have
                    directRoles.each { directRole ->
                        techRoles.values().each { parentRole ->
                            def subRole = allRoles.isNested(directRole, parentRole)
                            if (subRole != null)
                            {
                                if (acl.get(parentRole).get(rwsn) == 'NO_ACCESS') 
                                {
                                    acl.get(parentRole).put(rwsn, "(${subRole})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

delClos(folder);

// Output the authorization matrix
techRoles.each {
    print '\t' + it.value.shortName
}
println ''

def noAccess = []

wsList.each { wsName ->
    //Ouput the Runtime Web Service Name
    print wsName + '\t'
    
    def hasAccess = false
    
    acl.values().each {
        def access = it.get(wsName)

        if (access != 'NO_ACCESS') {
            print access
            hasAccess = true
        }
        print '\t'
    }
    println ''
    
    if (hasAccess == false) {
        noAccess << wsName
    }
}

println ''
println 'Web Service Interfaces with NO runtime security defined:'
noAccess.each {  
    println it
}