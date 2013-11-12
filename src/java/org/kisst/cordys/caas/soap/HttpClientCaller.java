/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.soap;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NTCredentials;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.util.URIUtil;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;

public class HttpClientCaller extends BaseCaller
{
    private final HttpClient client = new HttpClient();
    private final String ntlmhost;
    private final String ntlmdomain;

    public HttpClientCaller(String name)
    {
        super(name);
        ntlmhost = Environment.get().getProp("system." + name + ".gateway.ntlmhost", null);
        ntlmdomain = Environment.get().getProp("system." + name + ".gateway.ntlmdomain", null);
        if (ntlmdomain == null)
            client.getState().setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(userName, password));
        else
            client.getState().setCredentials(AuthScope.ANY, new NTCredentials(userName, password, ntlmhost, ntlmdomain));

    }

    @Override
    public String httpCall(String baseGatewayUrl, String input, HashMap<String, String> queryStringMap)
    {
        int statusCode;
        String response;
        PostMethod method = new PostMethod(baseGatewayUrl);

        logStart();

        try
        {
            method.setDoAuthentication(true);

            if (queryStringMap != null && queryStringMap.size() > 0)
            {
                method.setQueryString(URIUtil.encodeQuery(StringUtil.mapToString(queryStringMap)));
            }

            method.setRequestEntity(new StringRequestEntity(input, "text/xml", "UTF-8"));
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
            if (queryStringMap != null && queryStringMap.containsKey("timeout"))
            {
                String timeout = queryStringMap.get("timeout");
                client.getParams().setSoTimeout(Integer.parseInt(timeout));
                client.getParams().setConnectionManagerTimeout(Integer.parseInt(timeout));
            }
            
            statusCode = client.executeMethod(method);
            response = method.getResponseBodyAsString();
        }
        catch (HttpException e)
        {
            throw new RuntimeException(e);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        finally
        {
            logEnd(baseGatewayUrl, input);
        }

        if (statusCode != HttpStatus.SC_OK)
        {
            throw new RuntimeException("WebService failed: " + method.getStatusLine() + "\n" + response);
        }
        
        return response;
    }
}
