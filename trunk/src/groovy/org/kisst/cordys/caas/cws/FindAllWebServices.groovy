/**
 * This script looks at the cws source folder and will find all the web services that are defined. It 
 * will also check and output if the runtime security has been defined. Note that this check is very 
 * simple. It justs checks IF there is any security defined. It does not check if ALL webservices have 
 * runtime security defined.
 */
package org.kisst.cordys.caas.cws

import javax.xml.xpath.*
import javax.xml.parsers.DocumentBuilderFactory

import org.w3c.dom.NodeList;

if (args.length == 0) {
    println ("Missing parameter: foldername");
    System.exit 1
}

def folder = new File(args[0]);

def builder     = DocumentBuilderFactory.newInstance().newDocumentBuilder()
def xpath = XPathFactory.newInstance().newXPath()

def delClos

delClos = {
    it.eachDir(delClos);
    it.eachFile {
        if (it.name.endsWith("#cws-wsds#.cws")) {

            def inputStream = new ByteArrayInputStream(it.text.bytes)
            def wsd     = builder.parse(inputStream).documentElement

            def wsiName = xpath.evaluate("*[local-name()='Name']/text()", wsd)
            
            def wstype = xpath.evaluate("*[local-name()='Connectors']/*[local-name()='ApplicationConnectorRuntime']/*[local-name()='Name']/text()", wsd)

            def bindings = xpath.evaluate(".//*[local-name()='WebserviceBindings']/*[local-name()='WebserviceBinding']", wsd, XPathConstants.NODESET)
            bindings.each { wsb ->
                //Get the fqn
                def fqn = it.parentFile.canonicalPath.substring(folder.canonicalPath.length() + 1).replaceAll('\\\\', '/')

                //Figure out if runtime security is defined
                def NodeList nodes = xpath.evaluate(".//*[local-name()='SecurityDescriptor']/*", wsb, XPathConstants.NODESET)
                def runtimeSec = 'No'
                if (nodes.length > 0)
                {
                    runtimeSec = 'Yes'
                }

                def rwsn = xpath.evaluate("*[local-name()='RuntimeWebserviceInterfaceName']/text()", wsb)
                def docID = xpath.evaluate("*[local-name()='DocumentID']/text()", wsb)


                def name = xpath.evaluate("*[local-name()='Name']/text()", wsb)

                println rwsn + '\t' + wstype + '\t' + fqn + "/" + wsiName + "/" + name + '\t' + runtimeSec
            }
        }
    }
}

delClos(folder);
