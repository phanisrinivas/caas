// Shows runtime information about all SoapProcessors
// It is named after the ps command in Unix
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.main.Environment;

def sys = Caas.getDefaultSystem()
def name = args[0]

for (sc in sys.sc.like(name)) {
    if (sc.status=="Started") {
        println "Restarting ${sc}"
        sc.restart()
    }
}
