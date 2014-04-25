// Scripts checks the health of the Cordys system by using the health check URL

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.main.Environment;

Environment.verbose = true;

CordysSystem sys

if (args.length > 0) {
    sys = Caas.getSystem(args[0])
} else {
    sys = Caas.getDefaultSystem()
}

//Now we need to get the basic host name of the server, since we'd like to call the URL:
//http://server/cordys/com.eibus.web.tools.healthCheck.HealthCheckURL.wcp

def url = new URL(sys.soapCaller.urlBase);

println url 

