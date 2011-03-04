package org.kisst.cordys.caas.soap;

import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedHashMap;
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

	private final HttpClient client = new HttpClient();
	private final String SAML_ART= "SAMLart";
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
	public String sendHttpRequest(String url, String inputSoapRequest, LinkedHashMap<String, String> map){
				
		int statusCode;
		String response;
		String artifactId= SamlClient.getInstance(systemName).getArtifactID();		
		if(artifactId==null) 
			throw new CaasRuntimeException("Unable to get the SAML ArtifactID for system "+systemName);
		
		//Create LinkedHashMap object if the incoming map is null
		if(map==null)
			map = new LinkedHashMap<String, String>();
		
		//Put the SAML ArtifactID in the map
		map.put(SAML_ART, artifactId);
				
		Set<Entry<String, String>> set = map.entrySet();
		Iterator<Entry<String, String>> iterator = set.iterator();
		Map.Entry<String, String> entry;
		
		NameValuePair[] nvPairArray = new NameValuePair[map.size()]; 
		
		//Iterate through the LinkedHashMap and populate the NameValuePair array
		for(int i=0;iterator.hasNext();i++) {
			entry = iterator.next();
			nvPairArray[i] = new NameValuePair(entry.getKey(), entry.getValue());
		} 
	    
		PostMethod method=new PostMethod(url);
		method.setDoAuthentication(true);
		
		//Set the Query String
		method.setQueryString(nvPairArray);
				
		try {
			method.setRequestEntity(new StringRequestEntity(inputSoapRequest, "text/xml", "UTF-8"));
			statusCode = client.executeMethod(method);
			response=method.getResponseBodyAsString();
		}
		catch (HttpException e) { throw new RuntimeException(e);}
		catch (IOException e) { throw new RuntimeException(e);}
		if (statusCode != HttpStatus.SC_OK) {
			throw new RuntimeException("Method failed: " + method.getStatusLine()+"\n"+response);
		}
		return response;
	}
}