/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.soap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.NTCredentials;
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
import org.kisst.cordys.caas.main.Environment;

/**
 * This is the basic caller that is used when using a NTLM based connection.
 */
public class HttpClientCaller extends BaseCaller
{
    /** Holds the client. */
    private final DefaultHttpClient client;
    /** Holds the ntlmhost. */
    private final String ntlmhost;
    /** Holds the ntlmdomain. */
    private final String ntlmdomain;

    /**
     * Instantiates a new http client caller.
     * 
     * @param name The name
     */
    public HttpClientCaller(String name)
    {
        super(name);

        ntlmhost = Environment.get().getProp("system." + name + ".gateway.ntlmhost", null);
        ntlmdomain = Environment.get().getProp("system." + name + ".gateway.ntlmdomain", null);

        CredentialsProvider cp = new BasicCredentialsProvider();

        // Add the proxy user if it is set
        if (this.proxyUser != null)
        {
            cp.setCredentials(new AuthScope(proxyHost, Integer.parseInt(proxyPort)), new UsernamePasswordCredentials(
                    this.proxyUser, this.proxyPassword));
        }

        // Add the username/password
        if (ntlmdomain == null)
        {
            cp.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        }
        else
        {
            cp.setCredentials(AuthScope.ANY, new NTCredentials(userName, password, ntlmhost, ntlmdomain));
        }

        // Create the HttpClient that should be used.
        client = new DefaultHttpClient();
        client.setCredentialsProvider(cp);

        // Set the proxy server if defined.
        if (this.proxyPort != null)
        {
            HttpHost proxy = new HttpHost(proxyHost, Integer.parseInt(proxyPort));
            client.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
        }

    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#httpCall(java.lang.String, java.lang.String, java.util.HashMap)
     */
    @Override
    public String httpCall(String baseGatewayUrl, String input, HashMap<String, String> extraRequestParameters)
    {
        int statusCode;
        String response;

        logStart();

        try
        {
            Map<String, String> qp = createRequestParameters(extraRequestParameters);
            
            // Build up the URI.
            URIBuilder ub = new URIBuilder(baseGatewayUrl);
            for (Entry<String, String> e : qp.entrySet())
            {
                ub.addParameter(e.getKey(), e.getValue());
            }

            HttpPost method = new HttpPost(ub.build());

            // Set the XML data for the request
            method.setEntity(new StringEntity(input, ContentType.create("text/xml", "UTF-8")));

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
            if (e instanceof RuntimeException)
            {
                throw (RuntimeException) e;
            }

            throw new RuntimeException(e);
        }
        finally
        {
            logEnd(baseGatewayUrl, input);
        }

        if (statusCode != HttpStatus.SC_OK)
        {
            throw new RuntimeException("WebService failed: " + statusCode + "\n" + response);
        }

        return response;
    }
}
