/**
Copyright 2008, 2009 Mark Hooijkaas

This file is part of the Caas tool.

The Caas tool is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

The Caas tool is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with the Caas tool.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.kisst.cordys.caas.soap;

import java.util.HashMap;

import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

public abstract class BaseCaller implements SoapCaller 
{
	protected final String baseGatewayUrl;
	protected final String userName;
	protected final String password;
	protected final String proxyHost;
	protected final String proxyPort;
	protected final String proxyUser;
	protected final String proxyPassword;
	protected final HashMap<String, String> queryStringMap = new HashMap<String, String>();
	
	public abstract String httpCall(String baseGatewayUrl, String input, HashMap<String, String> queryStringMap);

	public BaseCaller(String name)
	{
		String completeGatewayUrl = Environment.get().getProp("system."+name+".gateway.url", null);
		if (completeGatewayUrl==null)
			throw new RuntimeException("No gateway URL is configured in the caas.conf file for property system."+name+".gateway.url");
		int pos=completeGatewayUrl.indexOf("?");
		if (pos>0)
		{
			baseGatewayUrl=completeGatewayUrl.substring(0,pos);
			//Fill up the query string map
			StringUtil.stringToMap(completeGatewayUrl.substring(pos+1),queryStringMap);
		}
		else
		{
			baseGatewayUrl=completeGatewayUrl;
		}
		userName = Environment.get().getProp("system."+name+".gateway.username", null);
		password = Environment.get().getProp("system."+name+".gateway.password", null);
		proxyHost = Environment.get().getProp("system."+name+".gateway.proxyhost", null);	
		proxyPort = Environment.get().getProp("system."+name+".gateway.proxyport", null);
		proxyUser = Environment.get().getProp("system."+name+".gateway.proxyuser", null);	
		proxyPassword = Environment.get().getProp("system."+name+".gateway.proxypassword", null);
	}
	
	private String httpCall(String input, HashMap<String, String> map)
	{
		if(map!=null)
		{
			map.putAll(queryStringMap);
			return httpCall(baseGatewayUrl, input, map);
		}
		else
		{
			return httpCall(baseGatewayUrl, input,queryStringMap);
		}
	}

	public String call(String input) 
	{
		return call(input,null);
	}
	
	public String call(String input, HashMap<String, String> map) 
	{
		String soap="<SOAP:Envelope xmlns:SOAP=\"http://schemas.xmlsoap.org/soap/envelope/\"><SOAP:Body>"
					+ input
					+ "</SOAP:Body></SOAP:Envelope>";
		Environment.get().debug(soap);
		String response = httpCall(soap, map);
		Environment.get().debug(response);
		if (response.indexOf("SOAP:Fault")>0)
			throw new RuntimeException(response);
		return response;
	}

	public XmlNode call(XmlNode method) 
	{
		return call(method, null);
	}

	public XmlNode call(XmlNode method, HashMap<String, String> map) 
	{
		Environment env=Environment.get();
		if (env.debug) { env.debug(method.getPretty()); }
		String xml = method.toString();
		String response= call(xml, map);
		XmlNode output=new XmlNode(response);
		if (output.getName().equals("Envelope"))
			output=output.getChild("Body").getChildren().get(0);
		if (env.debug)
			env.debug(output.getPretty());
		return output;
	}
}
