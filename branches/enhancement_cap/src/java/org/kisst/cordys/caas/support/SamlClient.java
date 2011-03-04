package org.kisst.cordys.caas.support;

import java.util.LinkedHashMap;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.soap.HttpClientCaller;
import org.kisst.cordys.caas.util.DateUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class represents the SAMLClient object
 * It returns a singleton SamlClient object per Cordys System. The properties of 
 * the singleton SamlClient object are refreshed seamlessly whenever they are expired
 * 
 * @author galoori
 */
public class SamlClient{

	private static String baseurl, username, password;
	private static HttpClientCaller caller;
	private static final long DIFF_MINUTES = 5;
	private String artifactID;
	private String issueTime;
	private String expiryTime;
	private String systemName;
	private static SamlClient singleton = null;
	private static final LinkedHashMap<String, SamlClient> samlCache = new LinkedHashMap<String, SamlClient>();
	protected SamlClient() {}
	
	
	public static synchronized SamlClient getInstance(String systemName)
	{
		singleton = samlCache.get(systemName);
		if(singleton == null){
			singleton = new SamlClient(systemName);
			samlCache.put(systemName, singleton);
		}
		return singleton;
	}
	
	//This constructor populates the values for the properties of the SamlClient object after creating it 
	private SamlClient(String systemName){
		this.systemName = systemName;
		sendSamlRequest();
	}
	private void setExipryTime(String expiryTime){
		this.expiryTime = expiryTime;
	}
	private void setIssueTime(String issueTime){
		this.issueTime = issueTime;
	}
	private void setArtifactID(String artifactID){
		this.artifactID = artifactID;
	}
	private String getSystemName(){
		return systemName;
	}
	public String getExpiryTime(){
		return expiryTime;
	}
	public String getIssueTime(){
		return issueTime;
	}
	public String getArtifactID()
	{
		//Check if the SamlClient is expired. If it is, fire a new request and refresh its properties 
		if(isExpired())
			sendSamlRequest();
		//Return the current artifactID of the SamlClient as it is not expired
		return artifactID;
	}
	
	/**
	 * Checks whether the SamlClient is expired or not
	 * 
	 * @return true if the SamlClient is about to expire in another DIFF_MINUTES, false otherwise
	 */
	public boolean isExpired()
	{
		if(DateUtil.getDifference(DateUtil.getCurrentUTCDate(),this.expiryTime,'M')>DIFF_MINUTES)
			return false;
		return true;
	}
	
	/**
	 * Reads the configuration details mentioned in caas.conf file 
	 * 
	 * @param systemName - Cordys system name whose details need to be read
	 */
	private static void readAuthDetails(String systemName)
	{
		String url =Environment.get().getProp("system."+systemName+".gateway.url", null);
		if (url==null) throw new RuntimeException("No url configured in property system."+systemName+".gateway.url");
		int pos=url.indexOf("?");
		if (pos>0) baseurl=url.substring(0,pos);
		else baseurl=url;
		username   = Environment.get().getProp("system."+systemName+".gateway.username", null);
		password   = Environment.get().getProp("system."+systemName+".gateway.password", null);
	}
	
	/**
	 * Sends the SAML request to the Cordys WebGateway 
	 * 
	 */
	private void sendSamlRequest()
	{
			String sysName = this.getSystemName();
			readAuthDetails(sysName);
		    caller = new HttpClientCaller(sysName);
			String response = caller.httpCall(baseurl, buildSamlRequest(sysName));
			handleSamlResponse(response);
	}
	
	/**
	 * Reads the SAML response and sets the SamlClient properties 
	 * 
	 */
	private void handleSamlResponse(String response)
	{
	    	Environment env = Environment.get();
	    	if(env.debug) env.debug(response);
	    	XmlNode responseNode=null, soapBodyNode=null, soapFaultNode=null;
	    	XmlNode output=new XmlNode(response);
	    	if (output.getName().equals("Envelope"))
	    		soapBodyNode = output.getChild("Body");
			if (response.indexOf("SOAP:Fault")>0){
				soapFaultNode = soapBodyNode.getChildren().get(0);
				throw new CaasRuntimeException(soapFaultNode.getChildText("faultstring"));
			}
						
			responseNode=soapBodyNode.getChildren().get(0);
			
			XmlNode assertionArtifactNode = responseNode.getChildren().get(3);	//TODO: Use XPath expression
			String artifactId = assertionArtifactNode.getText();
			XmlNode samlConditionsNode = responseNode.getChildren().get(2).getChildren().get(0); //TODO: Use XPath expression
			String issueTime = samlConditionsNode.getAttribute("NotBefore");
			String expiryTime = samlConditionsNode.getAttribute("NotOnOrAfter");
			
			issueTime = issueTime.substring(0, issueTime.indexOf("."))+"Z";		//Strip out the milliseconds part
			expiryTime = expiryTime.substring(0, expiryTime.indexOf("."))+"Z";	//Strip out the milliseconds part
			
			this.setIssueTime(issueTime);
			this.setExipryTime(expiryTime);
			this.setArtifactID(artifactId);
	}
	
	/**
	 * Builds the SAML request XML 
	 * 
	 * @param systemName - Cordys system name whose details are mentioned in caas.conf file
	 * @return requestXML - Formatted request XML string
	 */
	private String buildSamlRequest(String systemName)
	{
		String requestXML = "";
		StringBuilder builder = new StringBuilder();
		builder = builder.append("<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\">");
		builder = builder.append("<SOAP:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
		builder = builder.append("<wsse:Username>").append(username).append("</wsse:Username>");
		builder = builder.append("<wsse:Password>").append(password).append("</wsse:Password>");
		builder = builder.append("</wsse:UsernameToken></wsse:Security></SOAP:Header><SOAP:Body>");
		builder = builder.append("<samlp:Request xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\" MajorVersion=\"1\" MinorVersion=\"1\" IssueInstant=\"").append(DateUtil.getCurrentUTCDate()).append("\" RequestID=\"").append(StringUtil.generateUUID()).append("\">");
		builder = builder.append("<samlp:AuthenticationQuery><saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">");
		builder = builder.append("<saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">").append(username).append("</saml:NameIdentifier>");
		builder = builder.append("</saml:Subject></samlp:AuthenticationQuery></samlp:Request></SOAP:Body></SOAP:Envelope>");
		requestXML = builder.toString();
		builder.setLength(0);
		
    	Environment env = Environment.get();
    	if(env.debug) env.debug(requestXML);
    	return requestXML;
	}
}