package org.kisst.cordys.caas.deploy

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.helper.CAPPackage;
import org.kisst.cordys.caas.helper.Log;

/**
 * This script will upload an deploy the given package to the given Cordys environment using CAAS
 */

def cli = new CliBuilder(usage: 'run.cmd com/tatasteel/tse/groovy/deploy/DeployPackage [-h] [-f "capfolder" | -c "capfile"] -s "system"')
cli.h(longOpt: 'help'  , 'usage information', required: false)
cli.c(longOpt: 'cap', 'The CAP file that should be deployed', required: false, args: 1 )
cli.f(longOpt: 'folder', 'The folder containing the caps that should be deployed.', required: false, args: 1 )
cli.s(longOpt: 'system', 'The name of the parameter for the property file', required: true  , args: 1 )

OptionAccessor opt = cli.parse(args);
if (!opt) {
    return
}

if (!opt.c && !opt.f) {
    System.err.println "Either the folder with CAPs must be specified or a specific cap should be entered"
    System.exit(1)
}

// Connect to the system
CordysSystem system = Caas.getSystem(opt.s)

def Map<String, CAPPackage> toBeDeployed = new LinkedHashMap<String, CAPPackage>()

// Figure out the packages that should be loaded and figure out the sequence
if (opt.c) {
    def cf = new File(opt.c)
    if (!cf.exists()) {
        System.err.println "Defined cap file " + cf.absolutePath + " does not exist."
        System.exit(2)
    }

    //Add the CAP to the list of the to-be-deployed caps if it hasn't been deployed already.
    processCap(system, cf, toBeDeployed)
}

//Add all the caps if a folder was specified
if (opt.f) {
    def folder = new File(opt.f)

    for (srcCap in folder.listFiles())
    {
        processCap(system, srcCap, toBeDeployed)
    }
}

Log.info '= Going to deploy ' + toBeDeployed.size() + ' packages'
Log.debug 'Original order: ' + toBeDeployed.keySet()

toBeDeployed = CAPPackage.fixOrder(toBeDeployed);
Log.debug "Loading sequence:"
toBeDeployed.keySet().each { Log.debug it }

// First upload all packages
toBeDeployed.values().each {
    Log.info 'Uploading package ' + it.name + ' from file ' + it.file
    system.uploadCap(it.file.absolutePath)
}

// Upload and deploy the packages
toBeDeployed.values().each {
    Log.info 'Deploying package ' + it.name + ' using 60 minutes as the timeout'
    system.deployCap(it.name, 60)
}


/**
 * This method processes the CAP file 
 */
def processCap(CordysSystem system, File srcCap, Map<String, CAPPackage> toBeDeployed) {
    def ns = new groovy.xml.Namespace("http://schemas.cordys.com/ApplicationPackage")

    def cp = new CAPPackage(file: srcCap)

    //Now we have to do something tricky: we need to read the CAP package metadata to figure out the packacge name (the DN.
    def metadata = cp.getMetadata()

    //Find the package DN, version and build number of this package to be able to validate later on what version
    //is on the target system and if we can upload this package and/or deploy it.
    def name = metadata.'@name'
    def version = metadata[ns.Header][ns.Version].text()
    def buildNumber = metadata[ns.Header][ns.BuildNumber].text()
    def realVersion = "$version.$buildNumber".toString()

    Log.debug(name + " version $version.$buildNumber")
    Log.debug 'Dependencies: ' + cp.dependencies

    //Now we need to get the information on the target system (if the package is already loaded)
    def pkg = system.packages.getByName(name);
    if (pkg == null)
    {
        // Package is not loaded, so it is safe to load
        Log.debug 'Package ' + name + ' is not yet loaded'
        toBeDeployed.put(cp.name, cp);
    }
    else
    {
        // Package is already loaded. Need to double check the version numbers.
        if (pkg.info == null)
        {
            Log.info 'Package ' + name + ' is incomplete'
            toBeDeployed.put(cp.name, cp);
        }
        else
        {
            def loadedVersion = pkg.fullVersion
            Log.info 'Package ' + name + ' already loaded. Checking version ' + realVersion + " against loaded version " + loadedVersion
            if (loadedVersion == realVersion)
            {
                Log.info '--> Package ' + name + ' is up-to-date'
            }
            else
            {
                //Check if the version is greater then the
                def mostRecent = mostRecentVersion([realVersion, loadedVersion])
                if (mostRecent == loadedVersion)
                {
                    Log.info '--> Package ' + name + ' version ' + loadedVersion + ' is more recent then the version of the package that has to be loaded: ' + realVersion;
                }
                else
                {
                    //The version in the file is more recent, so we should load the package
                    Log.info '--> Package ' + name + ' version ' + realVersion+ ' is newer. Going to upload and deploy the package.';
                    toBeDeployed.put(cp.name, cp);
                }
            }
        }
    }
}

/**
 * This method returns whichversion is the most recent version. Note that it does NOT use a string compare, but it splits the version by 
 * the dots and compares the versions as integers.
 * 
 * @param versions The versions to compare.
 * @return The most recent version in the list
 */
String mostRecentVersion(List versions) {
    def sorted = versions.sort(false) { a, b ->

        List verA = a.tokenize('.')
        List verB = b.tokenize('.')

        def commonIndices = Math.min(verA.size(), verB.size())

        for (int i = 0; i < commonIndices; ++i) {
            def numA = verA[i].toInteger()
            def numB = verB[i].toInteger()

            if (numA != numB) {
                return numA <=> numB
            }
        }

        // If we got this far then all the common indices are identical, so whichever version is longer must be more recent
        verA.size() <=> verB.size()
    }

    sorted[-1]
}

