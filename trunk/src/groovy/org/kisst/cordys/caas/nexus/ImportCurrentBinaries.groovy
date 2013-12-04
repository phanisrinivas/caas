package org.kisst.cordys.caas.nexus

/**
 * This script will import all the binaries that are on a certain system into the Nexus repository.
 * 
 * Note that this script is needed only once!
 * 
 * Note 2: Change the logic around line 57 to determine the groupId and artifactId.
 */

import org.kisst.cordys.caas.helper.*;

def cli = new CliBuilder(usage: 'run.cmd org/kisst/cordys/caas/nexus/ImportCurrentBinaries [-h] [-f "capfolder" | -c "capfile"] -s "system"')
cli.h(longOpt: 'help'  , 'usage information', required: false)
cli.c(longOpt: 'cordyshome', 'The folder containing the caps that should be imported.', required: false, args: 1 )

OptionAccessor opt = cli.parse(args);
if (!opt) {
    return
}

if (!opt.c && !opt.f) {
    System.err.println "Either the folder with CAPs must be specified or a specific cap should be entered"
    System.exit(1)
}
//Connect to the Cordys folder
def cordys_root = new File(opt.c)
assert cordys_root.exists()
def capContent = new File(cordys_root, 'capcontent/packages')

assert capContent.exists()
Log.info("Location of cap packages: " + capContent.absolutePath)

//Declare the namespace
def ns = new groovy.xml.Namespace("http://schemas.cordys.com/ApplicationPackage")

def statusFile = new File(capContent, "status.txt")

def processed = ""
if (statusFile.exists())
{
    processed = statusFile.text
}

Log.info("Content of processed: $processed")

boolean append = true
FileWriter fileWriter = new FileWriter(statusFile, append)
BufferedWriter buffWriter = new BufferedWriter(fileWriter)

buffWriter.write(processed)
buffWriter.flush();

new File(capContent, "status.txt").withWriter { out ->
    // Get all the packages
    //Change the logic here to determine which packages you want in Nexus
    capContent.eachDirMatch(~/^Customer ([a-z_]+) (.+) v\d{6}.*/) { folder ->
        //Determine the logic to generate the groupId and artifactId based on the name of 
        def m = folder.name =~ /^Customer ([a-z_]+) (.+) v\d{6}.*/
        def groupId = m[0][1].replaceAll("\\_", ".")
        def artifactId = m[0][2].replaceAll("[^a-zA-Z0-9]+", "")

        Log.info("Found folder $folder.name as group $groupId and $artifactId")

        if (processed.indexOf(folder.name) == -1)
        {
            folder.eachFileMatch(~/.+\.cap/) { file ->
                Log.debug("Found cap file " + file.name)

                CAPPackage cp = new CAPPackage(file: file)

                //Now we have to do something tricky: we need to read the CAP package metadata to figure out the packacge name (the DN.
                def metadata = cp.getMetadata()

                //Find the package DN, version and build number of this package to be able to validate later on what version
                //is on the target system and if we can upload this package and/or deploy it.
                def name = metadata.'@name'
                def version = metadata[ns.Header][ns.Version].text()
                def buildNumber = metadata[ns.Header][ns.BuildNumber].text()
                def realVersion = "$version.$buildNumber".toString()

                if (version.matches("\\d+\\.\\d{1,2}\\.\\d{3}")) {
                    Log.debug ("New style: " + version)
                    realVersion = version
                } else {
                    Log.debug ("Old style: " + realVersion)
                }

                def cmd = [
                    "C:/development/apache-maven-3.0.5/bin/mvn.bat",
                    "-Durl=http://localhost:8081/nexus/content/repositories/customer",
                    "-DrepositoryId=customer",
                    "-DgroupId=$groupId",
                    "-DartifactId=$artifactId",
                    "-Dversion=$realVersion",
                    "-Dpackaging=cap",
                    "-Dfile=$file.absolutePath",
                    "deploy:deploy-file"
                ]

                Log.info("Uploading artifact $groupId:$artifactId version $realVersion using command " + cmd.toString())
                def p = cmd.execute();
                Thread.sleep(1000)
                p.outputStream.close();
                p.inputStream.close();
                p.waitForOrKill(60000)

                if (p.exitValue() != 0) {
                    Log.error("StdErr: " + p.err.text + "\nCommand:\n" + cmd.toString())
                }
            }

            buffWriter.write(folder.name)
            buffWriter.write("\n");
            buffWriter.flush();
        } else {
            Log.info("Skipping folder " + folder)
        }


    }
}
