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
import java.util.LinkedHashMap;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Class BaseCaller.
 */
public abstract class BaseCaller implements SoapCaller
{
    /**
     * Holds the url base for the Cordys server. This is for pre 4.2: http://server/cordys and for 4.2 and up http://server/home
     */
    protected final String urlBase;
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

    /**
     * Instantiates a new base caller.
     * 
     * @param name The name of the system we're connecting to.
     */
    public BaseCaller(String name)
    {
        urlBase = Environment.get().getProp("system." + name + ".gateway.url.base", null);

        if (urlBase == null)
        {
            throw new RuntimeException("No gateway URL base is configured in the caas.conf file for property system." + name
                    + ".gateway.url.base");
        }

        // Read the location of the default web gateway.
        location = Environment.get().getProp("system." + name + ".gateway.location", "com.eibus.web.soap.Gateway.wcp");

        // Read whether or not Organizational level deployment is used.
        String tmp = Environment.get().getProp("system." + name + ".gateway.old", "false");

        if ("true".equalsIgnoreCase(tmp))
        {
            orgLevelDeployment = true;
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

        // Construct the URL to use.
        String baseGatewayUrl = urlBase;

        if (!baseGatewayUrl.endsWith("/"))
        {
            baseGatewayUrl += "/";
        }

        // Add the org name if needed.
        if (orgLevelDeployment == true)
        {
            // The organization should no longer be passed on as a parameter, but as part of the URL.
            String org = filtered.remove("organization");

            if (org == null)
            {
                org = "system";
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

        // Execute the web service.
        return httpCall(baseGatewayUrl, input, filtered);
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

        Environment.get().debug(soap);

        String response = httpCall(soap, map);

        Environment.get().debug(response);

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
}
