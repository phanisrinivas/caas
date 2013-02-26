package org.kisst.cordys.caas.support;

import java.util.LinkedHashMap;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.soap.HttpClientCaller;
import org.kisst.cordys.caas.util.DateUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class represents the SAMLClient object It returns a singleton SamlClient object per Cordys System. The properties of the
 * singleton SamlClient object are refreshed seamlessly whenever they are expired
 * 
 * @author galoori
 */
public class SamlClient
{

    // Contains the Cordys BaseGateway URL value
    private static String baseGatewayUrl;
    // Contains the Cordys Username value as mentioned in the caas.conf file
    private static String userName;
    // Contains the Cordys User password value mentioned in the caas.conf file.
    private static String password;
    // Contains the due time of the SamlClient in Minutes
    private static final long EXPIRY_DUE_IN_MINUTES = 5; //
    // Stores the <samlp:AssertionArtifact> node value from the Saml response
    private String artifactID;
    // Stores the 'NotBefore' attribute value of the <saml:Conditions > node from the Saml response
    private String issueTime;
    // Stores the 'NotOnOrAfter' attribute value of the <saml:Conditions > node from the Saml response
    private String expiryTime;
    // Stores the Cordys system name for which the SamlClient is created
    private String systemName;
    // This LinkedHashMap acts as a cache of SamlClient objects. It contains one SamlClient object per one Cordys system
    private static final LinkedHashMap<String, SamlClient> samlCache = new LinkedHashMap<String, SamlClient>();

    protected SamlClient()
    {
    }

    /**
     * This webService returns the SamlClient object of the given system from the Saml cache. If the SamlClient object for the
     * given system is not present in the cache, then it creates a new SamlClient for the given system and stores it in the cache
     * for further use
     * 
     * @param systemName - Cordys system name mentioned in the caas.conf file,for which the SamlClient object is needed
     * @return singleton - Singleton SamlClient object for the given system name
     */
    public static synchronized SamlClient getInstance(String systemName)
    {
        SamlClient singleton = null;
        // Check whether the system name is null
        if (systemName == null)
            throw new CaasRuntimeException("System Name can not be blank");
        systemName = systemName.trim();

        // Check whether the system name is empty
        if (systemName.length() == 0)
            throw new CaasRuntimeException("System Name can not be empty");

        // Check whether a SamlClient object is present in the cache for the systemName
        boolean keyflag = samlCache.containsKey(systemName);
        if (keyflag)
        {
            // Get the SamlClient from the cache
            singleton = samlCache.get(systemName);
        }
        else
        {
            // Create a new SamlClient object for the systemName and put in cache for later use
            singleton = new SamlClient(systemName);
            samlCache.put(systemName, singleton);
        }
        return singleton;
    }

    // This constructor populates the values for the properties of the SamlClient object after creating it
    private SamlClient(String systemName)
    {
        this.systemName = systemName;
        sendSamlRequest();
    }

    // This is private webService so should be accessed within this class only
    private void setExipryTime(String expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    private void setIssueTime(String issueTime)
    {
        this.issueTime = issueTime;
    }

    private void setArtifactID(String artifactID)
    {
        this.artifactID = artifactID;
    }

    private String getSystemName()
    {
        return systemName;
    }

    public String getExpiryTime()
    {
        return expiryTime;
    }

    public String getIssueTime()
    {
        return issueTime;
    }

    /**
     * Gets the artifactID of the SamlClient which requested this webService. If the SamlClient is expired while it is calling
     * this webService, then it refreshes all the properties of it including artifactID and returns the new artifactID
     * 
     * @return artifactID
     */
    public String getArtifactID()
    {
        // Check if the SamlClient is expired. If it is, fire a new request and refresh its properties
        if (isExpired())
            sendSamlRequest();
        // Return the current artifactID of the SamlClient as it is not expired
        return artifactID;
    }

    /**
     * Checks whether the SamlClient is expired or not
     * 
     * @return true if the SamlClient is about to expire in another EXPIRY_DUE_IN_MINUTES, false otherwise
     */
    public boolean isExpired()
    {
        if (DateUtil.getDifference(DateUtil.getCurrentUTCDate(), this.expiryTime, 'M') > EXPIRY_DUE_IN_MINUTES)
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
        String urlBase = Environment.get().getProp("system." + systemName + ".gateway.url.base", null);

        if (urlBase == null)
        {
            throw new RuntimeException("No gateway URL base is configured in the caas.conf file for property system."
                    + systemName + ".gateway.url.base");
        }

        // Read the location of the default web gateway.
        String location = Environment.get().getProp("system." + systemName + ".gateway.location",
                "com.eibus.web.soap.Gateway.wcp");

        // Read whether or not Organizational level deployment is used.
        String tmp = Environment.get().getProp("system." + systemName + ".gateway.old", "false");

        boolean orgLevelDeployment = false;
        if ("true".equalsIgnoreCase(tmp))
        {
            orgLevelDeployment = true;
        }

        // Read additional default query string parameters from the location
        int pos = location.indexOf("?");

        if (pos > 0)
        {
            // Parse the query string into the default parameters
            // Fix the location without the query string parameters.
            location = location.substring(0, pos);
        }

        // Construct the URL to use.
        baseGatewayUrl = urlBase;

        if (!baseGatewayUrl.endsWith("/"))
        {
            baseGatewayUrl += "/";
        }

        // Add the org name if needed.
        if (orgLevelDeployment == true)
        {
            baseGatewayUrl += "system/";
        }

        // Now we can add the location
        if (location.startsWith("/"))
        {
            baseGatewayUrl += location.substring(1);
        }
        else
        {
            baseGatewayUrl += location;
        }

        userName = Environment.get().getProp("system." + systemName + ".gateway.username", null);
        // It is assumed that, always the password is stored in plain text
        password = Environment.get().getProp("system." + systemName + ".gateway.password", null);

        // Check if the UserName or Password is null
        if (userName == null || password == null)
            throw new CaasRuntimeException("UserName or Password is not configured for system " + systemName + " UserName:: "
                    + userName + " Password:: " + password);

        userName = userName.trim();
        password = password.trim();

        // Check if the UserName or Password is empty
        if (userName.length() == 0 || password.length() == 0)
            throw new CaasRuntimeException("UserName or Password is blank in the config file for system " + systemName
                    + ". UserName:: " + userName + " Password:: " + password);
    }

    /**
     * Sends the SAML request to the Cordys WebGateway It calls the readAuthDetails() webService to read the config details from
     * the caas.conf file and sends the request to the Cordys gateway using HttpClientCaller class.
     */
    private void sendSamlRequest()
    {
        String sysName = this.getSystemName();
        readAuthDetails(sysName);
        HttpClientCaller caller = new HttpClientCaller(sysName);
        // Fire the SAML request and read the response
        String response = caller.httpCall(baseGatewayUrl, buildSamlRequest(sysName), null);
        handleSamlResponse(response);
    }

    /**
     * Reads the SAML response and sets the SamlClient properties
     */
    private void handleSamlResponse(String response)
    {
        Environment env = Environment.get();
        if (env.debug)
            env.debug(response);
        XmlNode soapBodyNode = null, soapFaultNode = null;
        XmlNode output = new XmlNode(response);

        if (output.getName().equals("Envelope"))
            soapBodyNode = output.getChild("Body");

        // Check for the SOAP Fault in the response
        if (response.contains("SOAP:Fault"))
        {
            soapFaultNode = soapBodyNode.getChildren().get(0);
            throw new CaasRuntimeException(soapFaultNode.getChildText("faultstring"));
        }

        // Read the response
        String artifactId = soapBodyNode.getChildText("Response/AssertionArtifact");
        XmlNode conditionsNode = soapBodyNode.getChild("Response/Assertion/Conditions");
        String issueTime = conditionsNode.getAttribute("NotBefore");
        String expiryTime = conditionsNode.getAttribute("NotOnOrAfter");

        // Check the values of issueTime, expiryTime, artifactId
        if (issueTime == null || expiryTime == null || artifactId == null)
            throw new CaasRuntimeException("Invalid SAML Response. artifactId:: " + artifactId + " issueTime:: " + issueTime
                    + " expiryTime:: " + expiryTime);

        // Strip out the milliseconds part from the issueTime and expiryTime
        issueTime = issueTime.substring(0, issueTime.indexOf(".")) + "Z";
        expiryTime = expiryTime.substring(0, expiryTime.indexOf(".")) + "Z";

        // Fill the properties of the current SamlClient object
        this.setIssueTime(issueTime);
        this.setExipryTime(expiryTime);
        this.setArtifactID(artifactId);
    }

    /**
     * Builds the SAML request XML It populates the 'IssueInstant' attribute value with the currentUTCDate and 'RequestID' with a
     * random string
     * 
     * @param systemName - Cordys system name whose details are mentioned in caas.conf file
     * @return requestXML - Formatted request XML string
     */
    private String buildSamlRequest(String systemName)
    {
        String requestXML = "";
        StringBuilder builder = new StringBuilder();
        builder = builder.append("<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\">");
        builder = builder
                .append("<SOAP:Header><wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\"><wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">");
        builder = builder.append("<wsse:Username>").append(userName).append("</wsse:Username>");
        builder = builder.append("<wsse:Password>").append(password).append("</wsse:Password>");
        builder = builder.append("</wsse:UsernameToken></wsse:Security></SOAP:Header><SOAP:Body>");
        builder = builder
                .append("<samlp:Request xmlns:samlp=\"urn:oasis:names:tc:SAML:1.0:protocol\" MajorVersion=\"1\" MinorVersion=\"1\" IssueInstant=\"")
                .append(DateUtil.getCurrentUTCDate()).append("\" RequestID=\"").append(StringUtil.generateUUID()).append("\">");
        builder = builder
                .append("<samlp:AuthenticationQuery><saml:Subject xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\">");
        builder = builder.append("<saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:unspecified\">")
                .append(userName).append("</saml:NameIdentifier>");
        builder = builder.append("</saml:Subject></samlp:AuthenticationQuery></samlp:Request></SOAP:Body></SOAP:Envelope>");
        requestXML = builder.toString();
        builder.setLength(0);

        Environment env = Environment.get();
        if (env.debug)
            env.debug(requestXML);
        return requestXML;
    }
}