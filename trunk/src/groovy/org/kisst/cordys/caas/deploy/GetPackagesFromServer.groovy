/**
 * This script is used to get all packages that are installed on the Cordys Test server and export them to the given folder location
 * 
 * Arguments:
 * - output folder
 * - Cordys root to get the packages from
 * - name of the system to get the packages from
 */
package org.kisst.cordys.caas.deploy

import org.kisst.cordys.caas.Caas;

if (args.length < 3 || args.length > 4) {
    println ''
    println "Usage: caas run GetPackagesFromServer.groovy <output> <cordys_home> <system_name> <owner>"
    println ' Note: the <owner> is optional. If not specified then all packages not owned by Cordys will be exported'
    println ''

    return 1
}

// Where to get the packages from
def folder = args[0]
def cordys_root = new File(args[1])
def cordys_system = args[2];
def owner = ''
if (args.length == 4)
{
    owner = args[3]
}

// Copy method to copy a file from 1 location to another
copy = { File src,File dest->

    def input = src.newDataInputStream()
    def output = dest.newDataOutputStream()

    output << input

    input.close()
    output.close()
}

assert cordys_root.exists();

// Check the location where the cap packages are located
def capContent = new File(cordys_root, 'capcontent/packages');
assert capContent.exists();
println("Location of cap packages: " + capContent.absolutePath);

//Initialize Caas
def caas = new Caas();
def system = caas.getSystem(cordys_system);

//Iterate over all packages to figure out the custom ones.
for (pkg in system.packages)
{
    //Check if the owner is not Cordys
    if (pkg.pd != null && ((owner == '' && pkg.pd.owner != 'Cordys') || (owner.length() > 0 && pkg.pd.owner == owner )))
    {
        def filename = "ver" + pkg.pd.version + "build" + pkg.pd.buildnumber + ".cap"

        //We have found a package to export. Let's find its location
        def source = new File(capContent, pkg.pd.name + "/" + filename)
        assert source.exists();

        //Create the destination filename
        def dest = pkg.pd.name.replaceAll("[^a-zA-Z0-9]+", "_")

        //Remove the last digit
        dest = dest.substring(0, dest.lastIndexOf("_") + 1)
        dest = dest + pkg.pd.version + "." + pkg.pd.buildnumber + ".cap";
        def destFile = new File(new File(folder), dest)

        destFile.parentFile.mkdirs()

        copy(source, destFile)

        println "Copied package " + pkg.pd.name + " to " + destFile.getAbsolutePath()
    }
}

println 'Done'