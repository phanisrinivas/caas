package org.kisst.cordys.caas.helper

import groovy.transform.ToString;

import java.io.File;
import java.util.List;

@ToString
class Role
{
    def String id;
    def roleType;
    def String name;
    def String packageName;
    def String fqn;
    def parent;
    def isLocal;
    // The shortName and isTechnical assume the tech-Package-Name naming convention
    def String shortName;
    def isTechnical;
    def List<String> subroles = new ArrayList<String>();

    public Role(File f, File folder) {
        def r = new XmlParser().parseText(f.text)

        this.parent = f.parentFile.name;
        //Designtime role
        this.fqn = f.parentFile.canonicalPath.substring(folder.canonicalPath.length() + 1).replaceAll('\\\\', '/') + '/'

        if (r.name() == "RoleRuntime") {
            //Runtime reference
            this.id = r.DocumentID.text();
            this.roleType = r.type.text();
            this.name = r.Description.text();
            this.packageName = r.PackageInformation.CordysBinaryReferenceISVPInfo.ISVPName.text()
            this.isLocal = false;
        }
        else {
            this.id = r.DocumentID.text();
            this.roleType = r.type.text();
            this.name = r.Name.text();
            this.packageName = this.fqn.substring(0, this.fqn.indexOf('/'))
            this.isLocal = true;

            def tmp = r.Subroles.uri;

            tmp.each {
                this.subroles.add(it.'@id');
            }
        }
        
        def m = this.name =~ /tech-([^-]+)-(.+)/
        if (m && m[0]) {
            this.shortName = m[0][2]
            this.isTechnical = true
        } else {
            this.shortName = this.name
            this.isTechnical = false
        }
        
        this.fqn += '/' + this.name
    }
}
