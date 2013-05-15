// Shows runtime information about all SoapProcessors
// It is named after the ps command in Unix
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.main.Environment;

def sys = Caas.getDefaultSystem()

sys.refreshServiceContainers()
print String.format('%1$15.15s\t%2$31.31s\t%3$7.7s\t%4$7.7s', "ORGANIZATION", "SOAP PROCESSOR", "STARTUP","STATUS")
println String.format('\t%1$7s\t%2$7s\t%3$15s\t%4$9s', "PID", "BUSDOCS", "TOTAL", "LAST")
for (sc in sys.sc.sort()) {
    auto=""
    if (sc.automatic.bool)
        auto="auto"
    print String.format('%1$15.15s\t%2$31.31s\t%3$7.7s\t%4$7.7s', sc.parent.parent.name, sc.name, auto, sc.status)
    if (sc.pid==-1)
        println()
    else
        println String.format('\t%1$7d\t%2$7d\t%3$13dms\t%4$7dms', sc.pid, sc.busdocs, sc.processingTime, sc.lastTime)
}
