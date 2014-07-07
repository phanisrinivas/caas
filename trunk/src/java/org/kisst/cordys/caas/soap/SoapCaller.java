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

import org.kisst.cordys.caas.util.XmlNode;

/**
 * Holds the Interface SoapCaller.
 */
public interface SoapCaller
{
    /**
     * This method should be implements by each sub class to actually implement the sending of the HTTP request.
     * 
     * @param url The full URL to which the post should be done.
     * @param input The input string (SOAP message) to send.
     * @param queryStringMap The query string map containing additional parameters.
     * @return The response as a string.
     */
    public String httpCall(String url, String input, HashMap<String, String> queryStringMap);
    public String httpCall(String input);
    public String httpCall(String input, HashMap<String, String> map);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @return The response
     */
    public String call(String request);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param timeout The timeout to use.
     * @return The response
     */
    public String call(String request, long timeout);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param queryParams The additional query parameters for the request.
     * @return The response
     */
    public String call(String request, HashMap<String, String> queryParams);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param queryParams The additional query parameters for the request.
     * @param timeout The timeout to use.
     * @return The response
     */
    public String call(String request, HashMap<String, String> queryParams, long timeout);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @return The response xml node
     */
    public XmlNode call(XmlNode request);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param timeout The timeout to use.
     * @return The response xml node
     */
    public XmlNode call(XmlNode request, long timeout);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param queryParams The additional query parameters for the request.
     * @return The response xml node
     */
    public XmlNode call(XmlNode request, HashMap<String, String> queryParams);

    /**
     * This method executes the soap request. The given request should be without the SOAP envelope.
     * 
     * @param request The plain request without the SOAP envelope.
     * @param queryParams The additional query parameters for the request.
     * @param timeout The timeout to use.
     * @return The response xml node
     */
    public XmlNode call(XmlNode request, HashMap<String, String> queryParams, long timeout);

    /**
     * This method gets the base URL for this server.
     * 
     * @return The base URL for this server.
     */
    public String getUrlBase();

    /**
     * This method gets whether or not organization-level-deployment is enabled on this server. This is typically every 4.2 and up
     * instance.
     * 
     * @return Whether or not organization-level-deployment is enabled on this server. This is typically every 4.2 and up
     *         instance.
     */
    public boolean isOLDEnabled();

    /**
     * This method gets the username to use for connecting.
     * 
     * @return The username to use for connecting.
     */
    public String getUsername();
}
