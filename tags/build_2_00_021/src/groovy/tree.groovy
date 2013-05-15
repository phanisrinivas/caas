import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.main.Environment;

def sys = Caas.getDefaultSystem()

for (i in sys.isvps) {
    println "" + i +" ==> " + i.dn
    for (ms in i.webServiceInterfaces) {
        println "    " +ms
        for (m in ms.webServices)
            println "        " +m
    }
    for (r in i.roles)
        println "    " +r
}
for (o in sys.organizations) {
    println "" + o + " ==> " + o.dn
    for (u in o.users)
        println "    " +u
    for (ms in o.webServiceInterfaces) {
        println "    " +ms
        for (m in ms.webServices)
            println "        " +m
    }
    for (r in o.roles)
        println "    " +r
    for (s in o.serviceGroups) {
        println "    " +s
        for (p in s.serviceContainers)
            println "        " +p
    }
}
