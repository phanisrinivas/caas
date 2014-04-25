package org.kisst.cordys.caas.soap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.SamlClient;

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
        String response, baseURL = null;

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
        }

        // Build up the list of query parameters that shuld be sent. This is a combination of the default ones and the custom
        // ones. Note that the custom ones can override the default ones.
        Map<String, String> qp = createRequestParameters(queryParams);

        // Add the SAML artifact Id to the map of query string parameters
        if (addArtifact)
        {
            queryParams.put(SAML_ARTIFACT_NAME, artifactID);
        }

        CredentialsProvider cp = new BasicCredentialsProvider();

        // Add the proxy user if it is set
        if (this.proxyUser != null)
        {
            cp.setCredentials(new AuthScope(proxyHost, Integer.parseInt(proxyPort)), new UsernamePasswordCredentials(
                    this.proxyUser, this.proxyPassword));
        }

        logStart();
        try
        {
            // Build up the URI.
            URIBuilder ub = new URIBuilder(baseURL);
            for (Entry<String, String> e : qp.entrySet())
            {
                ub.addParameter(e.getKey(), e.getValue());
            }

            HttpPost method = new HttpPost(ub.build());

            // Set the XML data for the request
            method.setEntity(new StringEntity(inputSoapRequest, ContentType.create("text/xml", "UTF-8")));

            // Create the HttpClient that should be used.
            DefaultHttpClient client = new DefaultHttpClient();
            client.setCredentialsProvider(cp);

            // Set the proxy server if defined.
            if (this.proxyPort != null)
            {
                HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
                client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }

            // Need to use the timeout if specified
            if (qp.containsKey("timeout"))
            {
                String timeout = qp.get("timeout");

                HttpParams params = client.getParams();

                HttpConnectionParams.setConnectionTimeout(params, Integer.parseInt(timeout));
                HttpConnectionParams.setSoTimeout(params, Integer.parseInt(timeout));
            }

            HttpResponse hr = client.execute(method);
            statusCode = hr.getStatusLine().getStatusCode();

            response = EntityUtils.toString(hr.getEntity());
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
            throw new CaasRuntimeException("\nWebService failed: " + statusCode + "\n" + response);
        }

        return response;
    }
}