package org.kisst.cordys.caas.helper

class RoleList extends LinkedHashMap<String, Role> {
    public RoleList(File folder) {
        def delClose

        delClose = {
            it.eachDir(delClose);
            it.eachFile {
                if (it.name.endsWith("#cws-role#.cws") || it.name.endsWith("#cws-role-r#.cws")) {
                    def r = new Role(it, folder);
                    this.put(r.id, r);
                }
            }
        }

        //Do the actual processing
        delClose(folder);
    }

    public String isNested(Role direct, Role parentRole) {
        def retVal = null

        for (sr in parentRole.subroles) {
            def tmp = this.get(sr);

            if (tmp && tmp.fqn == direct.fqn) {
                retVal = tmp.shortName
                break
            }

            if (!retVal && !tmp.subroles.empty) {
                def tmpRole = this.isNested(direct, tmp)
                if (tmpRole) {
                    //Return the parent, not the name of the nested role.
                    retVal = tmp.shortName
                    break
                }
            }
        }

        retVal
    }
}
