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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import org.apache.commons.httpclient.util.URIUtil;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.util.StringUtil;

public class NativeCaller extends BaseCaller {
	private static class MyAuthenticator extends Authenticator {
		private String username=null;
		private String password=null;
		private MyAuthenticator () { Authenticator.setDefault(this); }
		public void setCredentials(String username, String password) {
			this.username=username;
			this.password=password;
		}
		@Override public PasswordAuthentication getPasswordAuthentication() {
			Environment.get().debug("getPasswordAuthentication"
					+"\n\t"+this.getRequestingHost()
					+"\n\t"+this.getRequestingPort()
					+"\n\t"+this.getRequestingPrompt()
					+"\n\t"+this.getRequestingProtocol()
					+"\n\t"+this.getRequestingScheme()
					+"\n\t"+this.getRequestingSite()
					+"\n\t"+this.getRequestingURL()
					+"\n\t"+this.getRequestorType()
			);
			if (username!=null)
				return new PasswordAuthentication(username, password.toCharArray());
			else
				return super.getPasswordAuthentication();
		}
	}
	protected static final MyAuthenticator myAuthenticator = new MyAuthenticator();
	

	public NativeCaller(String name) { super(name); }

	
	@Override public String httpCall(String baseGatewayUrl, String request, HashMap<String, String> queryStringMap) {		
		String completeGatewayUrl=null,line=null;
		HttpURLConnection connection=null;
		OutputStream out=null;
		InputStream in=null;
		BufferedReader reader=null;
		StringBuilder response = new StringBuilder();
		int statusCode = 0;
		try {
				if(queryStringMap!=null && queryStringMap.size()>0){
					completeGatewayUrl = baseGatewayUrl+"?"+StringUtil.mapToString(queryStringMap);
				}else{
					completeGatewayUrl = baseGatewayUrl;
				}
				URL url=new URL(URIUtil.encodeQuery(completeGatewayUrl));
				connection = (HttpURLConnection) url.openConnection();
				byte[] requestBytes = request.getBytes();
				connection.setRequestProperty("Content-Length", ""+requestBytes.length);
				connection.setRequestProperty("Content-Type","text/xml; charset=utf-8");
				connection.setRequestMethod("POST");
				connection.setDoOutput(true);
				connection.setDoInput(true);
				//Dangerous in multithreaded environments
				myAuthenticator.setCredentials(userName, password);
				//Write request data to server
				out = connection.getOutputStream();
				out.write(requestBytes);    
				//Read response data from server
				statusCode = connection.getResponseCode();
				if(statusCode == HttpURLConnection.HTTP_OK){
					in = connection.getInputStream();
				}else{
					in = connection.getErrorStream();
				}
				reader = new BufferedReader(new InputStreamReader(in));
				while ((line = reader.readLine())!=null)
					response.append(line);
			
		}catch(MalformedURLException e) {
	        e.printStackTrace();
	    }catch(ProtocolException e) {
	        e.printStackTrace();
	    }catch(IOException e) {
	        e.printStackTrace();
	    }catch(Exception e){
	    	e.printStackTrace();
	    }
		finally{
			connection.disconnect();
			out=null;
			reader=null;
			in=null;
			connection=null;
		}
		if (statusCode!=HttpURLConnection.HTTP_OK) {
			throw new CaasRuntimeException("\nWebService failed:: "+response.toString());
		}
		return response.toString();
	}
}
