/**
 * This script looks at the cws source folder and will find all roles that are defined.
 */
package org.kisst.cordys.caas.cws

if (args.length == 0) {
    println ("Missing parameter: foldername");
    System.exit 1
}

def folder = new File(args[0]);

def delClos

delClos = {
    it.eachDir(delClos);
    it.eachFile {
        if (it.name.endsWith("#cws-role#.cws")) {
            def role = new XmlParser().parseText(it.text)

            //Get the fqn
            def fqn = it.parentFile.canonicalPath.substring(folder.canonicalPath.length() + 1).replaceAll('\\\\', '/')
            println role.Name.text() + '\t' + role.type.text() + '\t' + role.DocumentID.text() + '\t' + fqn + "/" + role.Name.text()
        }
    }
}

delClos(folder);
