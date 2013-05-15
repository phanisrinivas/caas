/**
 * This script will load all packages that are in the tmp/tobeloaded folder
 */
package org.kisst.cordys.caas

import groovy.io.FileType;

import org.kisst.cordys.caas.helper.CAPPackage
import org.kisst.cordys.caas.helper.Caas
import org.kisst.cordys.caas.helper.Log

if (args.length != 2) {
    println ''
    println "Usage: caas run LoadAllPackages.groovy <source_folder> <system_name>"
    println ''

    return 1
}

//Main configuration
def source = new File(args[0]);

//Let the user enter the system to deploy the packages to
def target_system = args[1]


//Initialize Caas
def caas = new Caas();
def system = caas.getSystem(target_system);

//Initialize logging
Log.basic();

//Some sanity checks
assert source.exists()

//Declare the namespace
def ns = new groovy.xml.Namespace("http://schemas.cordys.com/ApplicationPackage")

def Map<String, CAPPackage> toBeDeployed = new LinkedHashMap<String, CAPPackage>()

source.traverse(
        type:FileType.FILES,
        nameFilter:~/.*\.cap/
        ) { srcCap -> 
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
        if (pkg.pd == null)
        {
            Log.info 'Package ' + name + ' is incomplete'
            toBeDeployed.put(cp.name, cp);
        }
        else
        {
            def loadedVersion = pkg.pd.version + '.' + pkg.pd.buildnumber
            Log.debug 'Package ' + name + ' already loaded. Checking version ' + realVersion + " against loaded version " + loadedVersion
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

Log.info '= Going to deploy ' + toBeDeployed.size() + ' packages'

Log.debug 'Original order: ' + toBeDeployed.keySet()

toBeDeployed = CAPPackage.fixOrder(toBeDeployed);
Log.debug "Loading sequence:"
toBeDeployed.keySet().each { Log.debug it }

// Upload and deploy the packages
toBeDeployed.values().each {
    Log.info 'Uploading package ' + it.name + ' from file ' + it.file
    system.uploadCap it.file.getAbsolutePath()

    Log.info 'Deploying package ' + it.name
    system.deployCap it.name
}

Log.info 'All done'

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



