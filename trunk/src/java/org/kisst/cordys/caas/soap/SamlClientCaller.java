package org.kisst.cordys.caas.soap;

import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.SamlClient;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * Responsible for executing the SOAP requests when Cordys is running in SSO mode.
 * 
 * @author galoori
 */
public class SamlClientCaller extends BaseCaller
{
    /** Holds the constant containing the SAML artifact name that need to be set in the query string. */
    private final String SAML_ARTIFACT_NAME = "SAMLart";
    /** Holds the Contains the Cordys system name which is in turn passed to the SamlClient class. */
    private final String systemName;

    /**
     * Instantiates a new saml client caller.
     * 
     * @param systemName The system name
     */
    public SamlClientCaller(String systemName)
    {
        super(systemName);
        this.systemName = systemName;
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#httpCall(java.lang.String, java.lang.String, java.util.HashMap)
     */
    @Override
    public String httpCall(String baseurl, String inputSoapRequest, HashMap<String, String> map)
    {
        return sendHttpRequest(baseurl, inputSoapRequest, map, true);
    }

    /**
     * Sends the input SOAP request to the Cordys Gateway after adding SAML ArtifactID. It also has a support to send query string
     * parameters like organization, timeout etc.
     * 
     * @param url - Cordys BaseGateway URL
     * @param inputSoapRequest - SOAP Request XML string
     * @param queryParams - Query string parameters that need to added to the BaseGateway URL.
     * @param addArtifact The add artifact
     * @return response - SOAP Response XML string
     */
    public String sendHttpRequest(String url, String inputSoapRequest, HashMap<String, String> queryParams, boolean addArtifact)
    {
        int statusCode, pos;
        String response, baseURL, queryString, aString = null;

        // Get the SamlClient instance for systemName and get its ArtifactID
        String artifactID = SamlClient.getInstance(systemName, this).getArtifactID();
        // Check if the artifactId is null
        if (artifactID == null)
        {
            throw new CaasRuntimeException("Unable to get the SAML ArtifactID for system '" + systemName + "'");
        }
        // Check if the url already contains any query string parameters
        baseURL = url;
        pos = url.indexOf("?");
        if (pos > 0)
        {
            baseURL = url.substring(0, pos);
            aString = url.substring(pos + 1);
        }
        if (queryParams == null)
        {
            queryParams = new HashMap<String, String>();
        }

        // Add the SAML artifact Id to the map of query string parameters
        if (addArtifact)
        {
            queryParams.put(SAML_ARTIFACT_NAME, artifactID);
        }

        // Convert the map of query string parameters to string
        if (aString == null)
        {
            queryString = StringUtil.mapToString(queryParams);
        }
        else
        {
            queryString = StringUtil.mapToString(queryParams) + "&" + aString;
        }
        // Create a PostMethod by passing the Cordys Gateway URL to its constructor
        PostMethod method = new PostMethod(baseURL);
        method.setDoAuthentication(true);

        logStart();
        try
        {
            // Set the query string after encoding it
            method.setQueryString(URIUtil.encodeQuery(queryString));
            method.setRequestEntity(new StringRequestEntity(inputSoapRequest, "text/xml", "UTF-8"));

            HttpClient client = new HttpClient();
            if (this.proxyPort != null)
            {
                client.getHostConfiguration().setProxy(proxyHost, Integer.parseInt(proxyPort));
                if (this.proxyUser != null)
                {
                    client.getState().setProxyCredentials(new AuthScope(proxyHost, Integer.parseInt(proxyPort)),
                            new UsernamePasswordCredentials(this.proxyUser, this.proxyPassword));
                }
            }

            // Need to use the timeout if specified
            if (queryParams != null && queryParams.containsKey("timeout"))
            {
                String timeout = queryParams.get("timeout");
                client.getParams().setSoTimeout(Integer.parseInt(timeout));
            }

            statusCode = client.executeMethod(method);

            response = method.getResponseBodyAsString();
        }
        catch (Exception e)
        {
            throw new CaasRuntimeException(e);
        }
        finally
        {
            logEnd(baseURL, inputSoapRequest);
        }

        if (statusCode != HttpStatus.SC_OK)
        {
            throw new CaasRuntimeException("\nWebService failed: " + method.getStatusLine() + "\n" + response);
        }
        return response;
    }
}