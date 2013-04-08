/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.soap;

import static org.kisst.cordys.caas.main.Environment.trace;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class BaseCaller.
 */
public abstract class BaseCaller implements SoapCaller
{
    /** Holds the regex to parse an old-style BOP 4.1 gateway URL */
    private static final Pattern GU_BOP41 = Pattern.compile("^([^/]+\\/\\/[^/]+\\/cordys)/(.+)$");
    /** Holds the regex to parse an old-style VOP 4.2+ gateway URL */
    private static final Pattern GU_BOP42 = Pattern.compile("^([^/]+\\/\\/[^/]+\\/home)\\/([^/]+)\\/(.+)$");
    /**
     * Holds the url base for the Cordys server. This is for pre 4.2: http://server/cordys and for 4.2 and up http://server/home
     */
    protected String urlBase;
    /** Holds the location of the web gateway. If not filled it is set to com.eibus.web.soap.Gateway.wcp. */
    protected String location;
    /** Holds the user name to use for connecting. */
    protected final String userName;
    /** Holds the password. */
    protected final String password;
    /** Holds the proxy host. */
    protected final String proxyHost;
    /** Holds the proxy port. */
    protected final String proxyPort;
    /** Holds the proxy user. */
    protected final String proxyUser;
    /** Holds the proxy password. */
    protected final String proxyPassword;
    /**
     * Holds whether or not organization-level-deployment is enabled on this server. This is typically every 4.2 and up instance.
     */
    protected boolean orgLevelDeployment = false;
    /** Holds the default query parameters that should ALWAYS be sent. */
    protected final HashMap<String, String> queryStringMap = new HashMap<String, String>();
    private long m_startTime;
    private long m_endTime;

    /**
     * Instantiates a new base caller.
     * 
     * @param name The name of the system we're connecting to.
     */
    public BaseCaller(String name)
    {
        urlBase = Environment.get().getProp("system." + name + ".gateway.url.base", null);
        // Read the location of the default web gateway.
        location = Environment.get().getProp("system." + name + ".gateway.location", "com.eibus.web.soap.Gateway.wcp");

        if (urlBase == null)
        {
            // The new way of specifying the connection details was not used. So let's see if they used the old way.
            String oldStyle = Environment.get().getProp("system." + name + ".gateway.url", null);
            if (StringUtil.isEmptyOrNull(oldStyle))
            {
                throw new RuntimeException("No gateway URL base is configured in the caas.conf file for property system." + name
                        + ".gateway.url");
            }

            // The old style of configuring is used. So we need to detect whether it's BOP 4.1 or a 4.2+
            Matcher m = GU_BOP41.matcher(oldStyle);
            if (!m.matches())
            {
                // Try to match the 4.2 variant
                m = GU_BOP42.matcher(oldStyle);
                if (!m.matches())
                {
                    throw new RuntimeException("Invalid gateway URL specified for system." + name + ".gateway.url");
                }

                urlBase = m.group(1);
                location = m.group(3);
                orgLevelDeployment = true;
            }
            else
            {
                // It's BOP 4.1
                urlBase = m.group(1);
                location = m.group(2);
                orgLevelDeployment = false;
            }
        }
        else
        {
            // Read whether or not Organizational level deployment is used.
            String tmp = Environment.get().getProp("system." + name + ".gateway.old", "false");

            if ("true".equalsIgnoreCase(tmp))
            {
                orgLevelDeployment = true;
            }
        }

        // Read additional default query string parameters from the location
        int pos = location.indexOf("?");

        if (pos > 0)
        {
            // Parse the query string into the default parameters
            StringUtil.stringToMap(location.substring(pos + 1), queryStringMap);

            // Fix the location without the query string parameters.
            location = location.substring(0, pos);
        }

        userName = Environment.get().getProp("system." + name + ".gateway.username", null);
        password = Environment.get().getProp("system." + name + ".gateway.password", null);
        proxyHost = Environment.get().getProp("system." + name + ".gateway.proxyhost", null);
        proxyPort = Environment.get().getProp("system." + name + ".gateway.proxyport", null);
        proxyUser = Environment.get().getProp("system." + name + ".gateway.proxyuser", null);
        proxyPassword = Environment.get().getProp("system." + name + ".gateway.proxypassword", null);
    }

    /**
     * This method gets the base URL for this server.
     * 
     * @return The base URL for this server.
     */
    public String getUrlBase()
    {
        return urlBase;
    }

    /**
     * This method gets whether or not organization-level-deployment is enabled on this server. This is typically every 4.2 and up
     * instance.
     * 
     * @return Whether or not organization-level-deployment is enabled on this server. This is typically every 4.2 and up
     *         instance.
     */
    public boolean isOLDEnabled()
    {
        return orgLevelDeployment;
    }

    /**
     * This method is used when one of the call methods is called. It will make sure that it will construct the proper web gateway
     * URL obeying OLD if enabled. In case of OLD if no organization is specified it will use the system org.
     * 
     * @param input The request that needs to be sent.
     * @param map The query string parameters.
     * @return The response of the call.
     */
    private String httpCall(String input, HashMap<String, String> map)
    {
        // Copy the query string map. This is needed to filter the organization in case of OLD.
        HashMap<String, String> filtered = new LinkedHashMap<String, String>();

        if (map != null)
        {
            filtered.putAll(map);
        }

        // Add the default parameters that were specified in the config file.
        filtered.putAll(queryStringMap);

        String baseGatewayUrl = getFinalGatewayURL(filtered);

        // Execute the web service.
        return httpCall(baseGatewayUrl, input, filtered);
    }

    /**
     * This method gets the username to use for connecting.
     * 
     * @return The username to use for connecting.
     */
    public String getUsername()
    {
        return userName;
    }

    /**
     * This method gets the password to use for authenticating the user.
     * 
     * @return The password to use for authenticating the user.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * This method returns the gateway URL that should be used to post to. In case of OLD it will return the URL of the System
     * org.
     * 
     * @return The gateway URL that should be used.
     */
    public String getFinalGatewayURL()
    {
        return getFinalGatewayURL(null, null);
    }

    /**
     * This method returns the gateway URL that should be used to post to. In case of OLD it will return the URL of the System
     * org.
     * 
     * @return The gateway URL that should be used.
     */
    public String getFinalGatewayURL(String organization)
    {
        return getFinalGatewayURL(organization, null);
    }

    /**
     * This method returns the gateway URL that should be used to post to.
     * 
     * @param queryParameters The query parameters. These are needed to read the organization from that should be used. If null
     *            then it will default to the system org.
     * @return The gateway URL that should be used.
     */
    public String getFinalGatewayURL(HashMap<String, String> queryParameters)
    {
        return getFinalGatewayURL(null, queryParameters);
    }

    /**
     * This method returns the gateway URL that should be used to post to.
     * 
     * @param organization The name of the organization that should be used.
     * @param queryParameters The query parameters. These are needed to read the organization from that should be used. If null
     *            then it will default to the system org.
     * @return The gateway URL that should be used.
     */
    private String getFinalGatewayURL(String organization, HashMap<String, String> queryParameters)
    {
        // Construct the URL to use.
        String baseGatewayUrl = urlBase;

        if (!baseGatewayUrl.endsWith("/"))
        {
            baseGatewayUrl += "/";
        }

        // Add the org name if needed.
        String org = null;
        if (orgLevelDeployment == true)
        {
            // The organization should no longer be passed on as a parameter, but as part of the URL.
            if (queryParameters != null)
            {
                org = queryParameters.remove("organization");
            }

            if (organization != null)
            {
                org = organization;
            }

            if (org == null)
            {
                org = "system";
            }
        }
        else
        {
            // For non-OLD we need to add it to the query parameters.
            if (queryParameters != null)
            {
                queryParameters.put("organization", organization);
            }
        }

        // If applicable, add the organization
        if (org != null)
        {
            // If the organization name contains a space, we need to URL encode it.
            if (org.indexOf(' ') >= 0)
            {
                org = org.replaceAll(" ", "%20");
            }

            baseGatewayUrl += org + "/";
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
        return baseGatewayUrl;
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#call(java.lang.String)
     */
    public String call(String input)
    {
        return call(input, null);
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#call(java.lang.String, java.util.HashMap)
     */
    public String call(String input, HashMap<String, String> map)
    {
        String soap = "<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP:Body>" + input
                + "</SOAP:Body></SOAP:Envelope>";

        trace(soap);

        String response = httpCall(soap, map);

        trace(response);

        if (response.indexOf("SOAP:Fault") > 0)
        {
            throw new RuntimeException(response);
        }

        return response;
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#call(org.kisst.cordys.caas.util.XmlNode)
     */
    public XmlNode call(XmlNode method)
    {
        return call(method, null);
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#call(org.kisst.cordys.caas.util.XmlNode, java.util.HashMap)
     */
    public XmlNode call(XmlNode method, HashMap<String, String> map)
    {
        String xml = method.toString();
        String response = call(xml, map);
        XmlNode output = new XmlNode(response);

        if (output.getName().equals("Envelope"))
        {
            output = output.getChild("Body").getChildren().get(0);
        }

        return output;
    }

    /**
     * This method should be called right before making a call. It is used to record the start time of a request.
     */
    protected void logStart()
    {
        m_startTime = System.currentTimeMillis();
    }

    /**
     * This method should be called to record the end time of a request. It will log the time the request took and the request
     * itself
     * 
     * @param url The URL to with the call was posted.
     * @param request The request that was executed.
     */
    protected void logEnd(String url, String request)
    {
        m_endTime = System.currentTimeMillis();

        if (Environment.debug)
        {
            StringBuilder sb = new StringBuilder(1024);

            if (m_startTime == -1)
            {
                sb.append("No start time recorded.");
            }
            else
            {
                sb.append("Request took ").append((m_endTime - m_startTime)).append(" miliseconds.");
            }

            sb.append(" URL: ").append(url).append(". Request: ");
            sb.append(request.replaceAll("\r{0,1}\n", ""));

            Environment.debug(sb.toString());
        }

        m_startTime = -1;
    }
}
