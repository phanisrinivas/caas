// Scripts checks the health of the Cordys system by using the health check URL

import static groovyx.net.http.ContentType.*
import static groovyx.net.http.Method.*

import java.util.ArrayList;
import java.util.List;

import groovyx.net.http.HTTPBuilder

import org.apache.http.auth.params.AuthPNames;
import org.apache.http.client.params.AuthPolicy;
import org.apache.http.params.BasicHttpParams
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.kisst.cordys.caas.Caas
import org.kisst.cordys.caas.CordysSystem
import org.kisst.cordys.caas.main.Environment
import org.kisst.cordys.caas.soap.HttpClientCaller
import org.kisst.cordys.caas.soap.SoapCaller

Environment.verbose = true;

CordysSystem sys

if (args.length > 0) {
    sys = Caas.getSystem(args[0])
} else {
    sys = Caas.getDefaultSystem()
}

//Now we need to get the basic host name of the server, since we'd like to call the URL:
//http://server/cordys/com.eibus.web.tools.healthCheck.HealthCheckURL.wcp

def SoapCaller sc = sys.soapCaller;
def url = new URL(sys.soapCaller.urlBase);

def finalUrl = url.protocol + "://" + url.host + (url.port != -1 ? ":" + url.getPort() : "") + "/cordys/com.eibus.web.tools.healthCheck.HealthCheckURL.wcp"

println finalUrl

List<String> authpref = new ArrayList<String>();

def http = new HTTPBuilder(finalUrl)

if (sc instanceof HttpClientCaller) {
    if (sc.ntlmdomain != null) {
        http.auth.ntlm(sc.userName, sc.password, url.host, sc.ntlmdomain)
        
        authpref.add(AuthPolicy.NTLM);
        authpref.add(AuthPolicy.DIGEST);
    } else {
        http.auth.basic(sc.userName, sc.password);
        
        authpref.add(AuthPolicy.BASIC);
    }
}

http.request(GET, HTML) {
    HttpParams p = it.getParams()
    if (p == null) {
        p = new BasicHttpParams()
    }
    p.setParameter(AuthPNames.TARGET_AUTH_PREF, authpref);
    it.setParams(p)
    
    response.success = { resp, html ->
        println resp.statusLine
        println html
    }

    response.failure = { resp -> println "Unexpected error: ${resp.statusLine.statusCode} : ${resp.statusLine.reasonPhrase}" }
}


