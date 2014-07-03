/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas.soap;

import static org.kisst.cordys.caas.main.Environment.debug;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;

import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.util.StringUtil;

/**
 * Holds the Class NativeCaller.
 */
public class NativeCaller extends BaseCaller
{

    /**
     * Holds the Class MyAuthenticator.
     */
    private static class MyAuthenticator extends Authenticator
    {

        /** Holds the username. */
        private String username = null;

        /** Holds the password. */
        private String password = null;

        /**
         * Instantiates a new my authenticator.
         */
        private MyAuthenticator()
        {
            Authenticator.setDefault(this);
        }

        /**
         * This method sets the credentials.
         * 
         * @param username The username
         * @param password The password
         */
        public void setCredentials(String username, String password)
        {
            this.username = username;
            this.password = password;
        }

        /**
         * @see java.net.Authenticator#getPasswordAuthentication()
         */
        @Override
        public PasswordAuthentication getPasswordAuthentication()
        {
            debug("getPasswordAuthentication" + "\n\t" + this.getRequestingHost() + "\n\t" + this.getRequestingPort() + "\n\t"
                    + this.getRequestingPrompt() + "\n\t" + this.getRequestingProtocol() + "\n\t" + this.getRequestingScheme()
                    + "\n\t" + this.getRequestingSite() + "\n\t" + this.getRequestingURL() + "\n\t" + this.getRequestorType());
            if (username != null)
                return new PasswordAuthentication(username, password.toCharArray());
            else
                return super.getPasswordAuthentication();
        }
    }

    /** Holds the Constant myAuthenticator. */
    protected static final MyAuthenticator myAuthenticator = new MyAuthenticator();

    /**
     * Instantiates a new native caller.
     * 
     * @param name The name
     */
    public NativeCaller(String name)
    {
        super(name);
    }

    /**
     * @see org.kisst.cordys.caas.soap.SoapCaller#httpCall(java.lang.String, java.lang.String, java.util.HashMap)
     */
    @Override
    public String httpCall(String baseGatewayUrl, String request, HashMap<String, String> queryStringMap)
    {
        String completeGatewayUrl = null;
        HttpURLConnection connection = null;
        OutputStream out = null;
        InputStream in = null;
        BufferedReader reader = null;
        StringWriter responseWriter=new StringWriter();
        String responseString;
        int statusCode = 0;
        logStart();
        try
        {
            if (queryStringMap != null && queryStringMap.size() > 0)
            {
                completeGatewayUrl = baseGatewayUrl + "?" + StringUtil.mapToString(queryStringMap);
            }
            else
            {
                completeGatewayUrl = baseGatewayUrl;
            }
            completeGatewayUrl=completeGatewayUrl.replace(" ","%20");
            URL url = new URL(completeGatewayUrl);
            connection = (HttpURLConnection) url.openConnection();
            byte[] requestBytes = request.getBytes();
            connection.setRequestProperty("Content-Length", "" + requestBytes.length);
            connection.setRequestProperty("Content-Type", "text/xml; charset=utf-8");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);

            // Need to use the timeout if specified
            if (queryStringMap != null && queryStringMap.containsKey("timeout"))
            {
                String timeout = queryStringMap.get("timeout");
                connection.setReadTimeout(Integer.parseInt(timeout));
            }

            // Dangerous in multithreaded environments
            myAuthenticator.setCredentials(userName, password);
            // Write request data to server
            out = connection.getOutputStream();
            out.write(requestBytes);
            // Read response data from server
            statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_OK)
            {
                in = connection.getInputStream();
            }
            else
            {
                in = connection.getErrorStream();
            }
            reader = new BufferedReader(new InputStreamReader(in));
            copyLarge(reader, responseWriter);

        }
        catch (MalformedURLException e)
        {
            e.printStackTrace();
        }
        catch (ProtocolException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            responseString = responseWriter.toString();
            logEnd(baseGatewayUrl, request, responseString);
            connection.disconnect();
            out = null;
            reader = null;
            in = null;
            connection = null;
        }
        if (statusCode != HttpURLConnection.HTTP_OK)
        {
            throw new CaasRuntimeException("\nWebService failed:: " + responseString);
        }
        return responseString;
    }
    
    private long copyLarge(Reader input, Writer output) throws IOException {
        char[] buffer = new char[2048];
        long count = 0;
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }
}
