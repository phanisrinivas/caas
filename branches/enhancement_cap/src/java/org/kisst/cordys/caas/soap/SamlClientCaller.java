package org.kisst.cordys.caas.soap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.support.SamlClient;

/**
 * Responsible for executing the SOAP requests when Cordys is running in SSO mode
 *  
 * @author galoori
 */

public class SamlClientCaller extends BaseCaller {

	//A constant containing the SAML artifact name that need to be set in the query string
	private final String SAML_ARTIFACT_NAME= "SAMLart";
	//Contains the Cordys system name which is in turn passed to the SamlClient class
	private final String systemName;
	
	public SamlClientCaller(String systemName){
		super(systemName);
		this.systemName = systemName;
	}

	@Override 
	/**
	 * Delegates the incoming SOAP request to sendHttpRequest
	 */
	public String httpCall(String url, String inputSoapRequest) {
		return sendHttpRequest(url,inputSoapRequest,null);
	}

	/**
	 * Sends the input SOAP request to the Cordys Gateway after adding SAML ArtifactID. 
	 * It also has a support to send query string parameters like organization, timeout etc.
	 * 
	 * @param url - Cordys BaseGateway URL
	 * @param inputSoapRequest - SOAP Request XML string
	 * @param map - Query string parameters that need to added to the BaseGateway URL.
	 * @return response - SOAP Response XML string
	 */
	public String sendHttpRequest(String url, String inputSoapRequest, HashMap<String, String> map){
				
		int statusCode;
		String response;
		
		//Get the SamlClient instance for systemName and get its ArtifactID 
		String artifactId= SamlClient.getInstance(systemName).getArtifactID();		
		
		//Check if the artifactId is null
		if(artifactId==null) 
			throw new CaasRuntimeException("Unable to get the SAML ArtifactID for system "+systemName);
		
		//Create HashMap object to put the SAML ArtifactID name and value, if the incoming map is null
		if(map==null)
			map = new HashMap<String, String>();
		
		//Put the SAML Artifact Name and value in the map
		map.put(SAML_ARTIFACT_NAME, artifactId);
				
		Set<Entry<String, String>> set = map.entrySet();
		Iterator<Entry<String, String>> iterator = set.iterator();
		Map.Entry<String, String> entry;
		
		NameValuePair[] nvPairArray = new NameValuePair[map.size()]; 
		
		//Iterate through the HashMap and populate the NameValuePair array
		for(int i=0;iterator.hasNext();i++) {
			entry = iterator.next();
			nvPairArray[i] = new NameValuePair(entry.getKey(), entry.getValue());
		} 
	    
		//Create a PostMethod by passing the Cordys Gateway URL to its constructor
		PostMethod method=new PostMethod(url);
		method.setDoAuthentication(true);
		
		//Set the Query String
		method.setQueryString(nvPairArray);
				
		try {
			method.setRequestEntity(new StringRequestEntity(inputSoapRequest, "text/xml", "UTF-8"));
			HttpClient client = new HttpClient();
			statusCode = client.executeMethod(method);
			response=method.getResponseBodyAsString();
		}
		catch (HttpException e) { throw new CaasRuntimeException(e);}
		catch (IOException e) { throw new CaasRuntimeException(e);}
		if (statusCode != HttpStatus.SC_OK) {
			throw new CaasRuntimeException("Method failed: " + method.getStatusLine()+"\n"+response);
		}
		return response;
	}
}