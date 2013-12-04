/**
 * This script will display the hierarchy of each role. It will examine the cws-role files and 
 * also the runtime references to the nested roles. 
 */
package org.kisst.cordys.caas.cws

import org.kisst.cordys.caas.helper.Role


if (args.length == 0) {
    println ("Missing parameter: foldername");
    System.exit 1
}

def folder = new File(args[0]);

def delClos

Map<String, Role> allRoles = new LinkedHashMap<String, Role>()
delClos = {
    it.eachDir(delClos);
    it.eachFile {
        if (it.name.endsWith("#cws-role#.cws") || it.name.endsWith("#cws-role-r#.cws")) {
            def r = new Role(it, folder);
            allRoles.put(r.id, r);
        }
    }
}

//Do the actual processing
delClos(folder);

allRoles.values().each {
    if (it.isLocal == true)
    {
        println it.parent + ' - ' + it.name
        it.subroles.each { subroleID ->
            def sr = allRoles.get(subroleID)

            if (sr == null) {
                println 'SR: ' + subroleID + ' not found'
            } else {
                println '\t' + sr.name + '\t' + sr.packageName
            }
        }
    }
}

println '\n\n'

allRoles.values().each {
    def m = it.name =~ "tech-([^-]+)-(.+)"
    if (m.find()) {
        print m[0][2] + '\t'
    }
    
}

