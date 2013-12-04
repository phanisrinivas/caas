/**
 * This script will parse the output of the svnadmin lslocks <repository> to figure out 
 * which workspaces hold locks on the repository.
 * 
 * Execute the command 'svnadmin lslocks c:\SVNRepository > ls_locks_output.txt' to get the locks. Then feed the output
 * to this script.
 */
package org.kisst.cordys.caas.svn

def cli = new CliBuilder(usage: 'run.cmd org/kisst/cordys/caas/svn/ParseSVNLsLocks [-h] -l "ls_locks_output.txt"')
cli.h(longOpt: 'help'  , 'usage information', required: false)
cli.l(longOpt: 'lsfile', 'The locks file that should be parsed.', required: true, args: 1 )

OptionAccessor opt = cli.parse(args);
if (!opt) {
    return
}
def sourceFilename = opt.l

//Will hold all the lock information
def lockList = []

File source = new File(sourceFilename);
if (!source.exists()) {
    println "File " + sourceFilename + " does not exist"
}
else {
    // Split the text of the file into blocks separated by \n\n
    // Then, starting with an empty list go through each block of text in turn
    lockList = source.text.split( '\r{0,1}\n\\s*\r{0,1}\n' ).inject( []) { list, block ->
        // Split the current block into lines (based on the newline char)
        // Then starting with an empty map, go through each line in turn
        // when done, add this map to the list we created in the line above
        def comment = []

        list << block.split( '\n' ).inject( [:] ) { map, line ->
            // Split the line up into a key and a value (trimming each element)
            def (key,value) = line.split( ': ' ).collect { it.trim() }
            // Then, add this key:value mapping to the map we created 2 lines above
            if (key != null && value != null) {
                map << [ (key): value ]
            } else {
                comment.add line
            }

            if (comment.size() == 2) {
                map << [('Comment'): line]
                comment = []
            }
            map
        }
    }
}

def organizations = [:]
def ws = ~/\<Workspace>'([^']+)' from organization '([^']+)'<\/Workspace>/

lockList.each {
    //Parse the SCM comment
    def res = it.Comment =~ /\<Workspace>'([^']+)' from organization '([^']+)'<\/Workspace>/

    def workspaces = [:]

    if (!organizations.containsKey(res[0][2])) {
        organizations.put(res[0][2], workspaces);
    } else {
        workspaces = organizations.get(res[0][2]);
    }
    
    //Check if the workspace is already registered
    def wsID = res[0][1] + '(' + it.Owner + ')'
    def paths = []
    if (!workspaces.containsKey(wsID)) {
        workspaces.put(wsID, paths);
    } else {
        paths = workspaces.get(wsID);
    }
    
    paths.add(it.Path)
}

//Do the sorting
println "Workspaces containing locks (organization, workspace):"
organizations.each { org, workspaces ->
    println org
    workspaces.sort().each { wsID, paths ->
        println '\t' + wsID
        paths.sort().each {
            println '\t\t' + it
        }
    }
}