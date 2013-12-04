package org.kisst.cordys.caas.cws

/**
 * This script will connect to all known environments to check whether or not packages are installed and if 
 * so, which version is installed.
 */
import org.kisst.cordys.caas.*
import org.kisst.cordys.caas.main.Environment;

def systems = ["dev", "test", "acc", "prod"]
def connectedSystems = [:]
Environment.verbose = true

//Connect to the configured systems
systems.each { name ->
    connectedSystems << ["${name}" : Caas.getSystem(name)] 
}

//Iterate over all packages to figure out which packages we's like to see in the overview.
def allPackages = new LinkedHashMap<String, Map<String, String>>()
connectedSystems.each {sysName, connection ->
    Environment.info("Getting packages for system " + sysName)
    for (pkg in connection.packages)
    {
        Environment.info("Found package " + pkg.name + " on system " + sysName)
        //Check if the package is already mentioned
        def systemInfo = allPackages.get(pkg.name)
        if (systemInfo == null) {
            systemInfo = new LinkedHashMap<String, String>()
            systems.each { tmp ->
                systemInfo << ["${tmp}" : '']
            }
            allPackages.put(pkg.name, systemInfo)
        }
        
        systemInfo.put(sysName, pkg.version + "." + pkg.buildnumber)
    }
}

//Now print out the result
systems.each {
    print '\t' + it
}
println ''

allPackages.each {packageName, sysInfo ->
    print packageName
    
    sysInfo.each {sysName, version ->
        print '\t' + version
    }
    println ''
}
