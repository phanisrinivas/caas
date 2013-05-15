package org.kisst.cordys.caas.main;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.util.URIUtil;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

@SuppressWarnings("unused")
public class Sample {

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}
	
	private static void reverseSubstituteTest(){
					String jreconfig = "<jreconfig>"+
					"<param value='-cp CORDYS_INSTALL_DIR/Immediate/Immediate.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/axis.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/commons-discovery-0.2.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/commons-logging-1.0.4.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/jaxrpc.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/wsdl4j-1.5.1.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/wss4j-1.5.8.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/xalan-2.7.1.jar:CORDYS_INSTALL_DIR/rmg/SalesOrderReports/lib/xmlsec-1.4.3.jar' />"+
					"<param value='-Xmx256M' />"+
					"<param value='-XX:PermSize=5m' />"+
					"<param value='-DKeyStoreInfoProp=CORDYS_INSTALL_DIR/rmg/SalesOrderReports/keystore/keystore.properties' />"+
					"<param value='-Dcircuitbreaker.enabled=false' />"+
					"</jreconfig>";
					XmlNode jreNode = new XmlNode(jreconfig);
					jreNode.compact();
					
					String xml2 =   "<xmlstoreobject>"+
					"<RMLVendaConfiguration>"+
					"<Services>"+
					"<RetrieveUser>"+
					"<Endpoint>https://royalmail.uat.venda.com/soap/doclit/3</Endpoint>"+
					"<TimeoutInMillis />"+
					"</RetrieveUser>"+
					"<UpdatedOrders>"+
					"<MaxResults>50</MaxResults>"+
					"<ChangeLogRef>7300</ChangeLogRef>"+
					"<FromDate />"+
					"<Endpoint>https://royalmail.uat.venda.com/soap/rpcenc/3</Endpoint>"+
					"<TimeoutInMillis />"+
					"</UpdatedOrders>"+
					"<VendaRetrieveOrder dn='cn=CoBOC,cn=soap nodes,o=system,cn=cordys,cn=BOP,o=uk-prv.attenda.net'>"+
					"<Endpoint>https://royalmail.uat.venda.com/soap/rpcenc/3</Endpoint>"+
					"<TimeoutInMillis />"+
					//"<hello>http://xia.sap.point:9975/XISOAPAdapter/MessageServlet?channel=:BS_SAP_OCA:CC_OS_EMAILID&amp;amp;version=3.0&amp;amp;Sender.Service=x&amp;amp;Interface=x%5Ex</hello>"+
					"<hello>http://xia.sap.point:9975/XISOAPAdapter?hello</hello>"+
					"</VendaRetrieveOrder>"+
					"</Services>"+
					"</RMLVendaConfiguration>"+
					"</xmlstoreobject>";
					
					try{
					
					Properties props = new Properties();
					props.load(new FileInputStream("D:/Users/galoori/config/caas/cordysSIT.properties"));
					//System.out.println(props.size());
		           @SuppressWarnings({ "rawtypes", "unchecked" })
					Map<String,String> map = new HashMap<String, String>((Map) props);
					
					System.out.println("export::: "+StringUtil.reverseSubstitute(xml2, map));
					System.out.println("import::: "+StringUtil.substitute(xml2, map));
					
					
					}catch(Exception e){e.printStackTrace();}
		
	}
	
	public void testRemoveComments(){
		String xml="<configuration xmlns='http://schemas.cordys.com/1.0/xmlstore'>"+
	"<!--Sample AON Header"+	  
	  "Header record is built as follows:"+
		"· Record type – always H"+
		"· Interface file name – same as above - CCYYMMDD_0000000000_TR_P.dat"+
		"· Date of file creation – DD/MM/CCYY"+
		"· Interface file product indicator – always TR"+
		
		"Input XML"+ 
		"<Header>"+
		"<FileName>20110610_0000000000_TR_P</FileName>"+
		"<CurrentDate>2004-01-31T20:10:01.200</CurrentDate>"+
		"</Header>"+
	"-->"+
  "<filetype name='AON-HEADER'>"+
    "<select path='Header'>"+
    "hello"+
    "</select>"+
	"</filetype>"+
	"</configuration>";
		XmlNode node = new XmlNode(xml);
		System.out.println(StringUtil.removeXmlComments(node.compact()));
		
	}
	
	@Test
	public void testMain() {

		reverseSubstituteTest();
		//testRemoveComments();
			
		//System.out.println(new XmlNode(replacedStr).getPretty());
		
		//System.out.println(new XmlNode(xml).getChildren("sp").size());
		
	/*	Properties props = new Properties();
		try {
			props.load(new FileInputStream("D:/Users/galoori/config/caas/cordysVM.properties"));
			System.out.println(props.size());
			System.out.println(props.entrySet());
			
			Map<String,String> map = new HashMap<String, String>((Map) props);
			System.out.println("ACTUAL:: "+map);
			System.out.println("ACTUAL SIZE:: "+map.size());
			Map<String,String> rev = reverse(map);
			System.out.println("REVERSE:: "+rev);
			System.out.println("REVERSE SIZE:: "+rev.size());
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		/*CordysSystem vm = Caas.getSystem("cordysVM");
		
		System.out.println(vm.o.get(0).dso.getSize());
		System.out.println(vm.o.get(0).proc.getSize());
		*/
		//Caas.connect("D:/Users/galoori/config/caas/caas.conf").o.get(0).proc.getSize();
		
		//Assert.assertEquals(true, true);
		//Assert.assertEquals(true, caaspmExport());
		//Assert.assertEquals(true, caaspmImport());
		
		//joinTest();
	}
	
	
	private void joinTest(){
		Map<String,String> tokens = new HashMap<String,String>();
		tokens.put("key1", "value1");
		tokens.put("key2", "value2");

		String template = "${value1} really needs some ${value2}.";

		
		
		// Create pattern of the format "%(cat|beverage)%"
		String patternString = "\\$\\{(" + StringUtil.join(tokens.values().iterator(), "|") + ")\\}";
		System.out.println("Pattern::: "+patternString);
		Pattern pattern = Pattern.compile(patternString);
		Matcher matcher = pattern.matcher(template);

		StringBuffer sb = new StringBuffer();
		while(matcher.find()) {
			System.out.println(matcher.group(1));
			
		    matcher.appendReplacement(sb, StringUtil.getKeyByValue(tokens, matcher.group(1)));
		    
		}
		matcher.appendTail(sb);

		System.out.println(sb.toString());
	}
	
	
    private boolean caaspmExport(){
		//String cmd = "pm template -o RMG_Release3 -s cordysDev D:/Users/galoori/config/caas/dev_test.caaspm";
		//String cmd_sit = "pm template -o RMG -s cordysSIT D:/Users/galoori/config/caas/rmg_SIT.caaspm";
		//String cmd_vm = "pm template -o system -s cordysVM D:/Users/galoori/config/caas/test.caaspm";
		String cmd_uat = "pm template -o RMG -s cordysUAT D:/Users/galoori/config/caas/rmg_UAT.caaspm";
		CaasMain.main(cmd_uat.split(" "));
		return true;
	}
    private boolean caaspmImport(){
		//String cmd="pm create -o Practice -s cordysVM D:/Users/galoori/config/caas/vm.caaspm";
		String cmd_vm="pm create -o CaasOrg -s cordysVM D:/Users/galoori/config/caas/rmg_SIT.caaspm";
		
		//String cmd_vm2="pm create -o CaasOrg -s cordysVM D:/Users/galoori/config/caas/onlydso.caaspm";
		
		CaasMain.main(cmd_vm.split(" "));
		return true;
	}
	
	//map must be a bijection in order for this to work properly
	public static <K,V> HashMap<V,K> reverse(Map<K,V> map) {
	    HashMap<V,K> rev = new HashMap<V, K>();
	    for(Map.Entry<K,V> entry : map.entrySet())
	        rev.put(entry.getValue(), entry.getKey());
	    return rev;
	}
	
    private void test(){
		String xml="<accessconfigurations applyToAllMethodSets='false'>"+
		"<accessconfiguration methodsetdn='cn=Prepay Web Service.apiBinding,cn=webService sets,o=RMG,cn=cordys,cn=BOP,o=uk-prv.attenda.net'>"+
			"<urlmappings>"+
				"<urlmapping existingurl='http://schemas.rmg.com/ebusiness/prepay/1.0'>"+
					"<customizedurl>http://wwwsit3.royalmail.com/sites/all/modules/custom/smartstamp/modules/prepay/prepay_server/soapserver/server.php</customizedurl>"+
					"<usermappings>"+
						"<usermapping roledn='cn=externaluser,cn=organizational roles,o=RMG,cn=cordys,cn=BOP,o=uk-prv.attenda.net'>"+
							"<authenticationtype>Anonymous</authenticationtype>"+
							"<username />"+
							"<password>AA==</password>"+
						"</usermapping>"+
					"</usermappings>"+
				"</urlmapping>"+
			"</urlmappings>"+
		"</accessconfiguration>"+
		"<accessconfiguration methodsetdn='cn=UpdateFulfillOrderWeb Service.UpdateFufillOrderBinding,cn=webService sets,o=RMG,cn=cordys,cn=BOP,o=uk-prv.attenda.net'>"+
			"<urlmappings>"+
				"<urlmapping existingurl='http://st.capgemini.royalmail.com/olp_server/server'>"+
					"<customizedurl>wwwsit3.royalmail.com/olp_server/server</customizedurl>"+
					"<usermappings>"+
						"<usermapping roledn='cn=externaluser,cn=organizational roles,o=RMG,cn=cordys,cn=BOP,o=uk-prv.attenda.net'>"+
							"<authenticationtype>Anonymous</authenticationtype>"+
							"<username />"+
							"<password>AA==</password>"+
						"</usermapping>"+
					"</usermappings>"+
				"</urlmapping>"+
			"</urlmappings>"+
		"</accessconfiguration>"+
	"</accessconfigurations>";
			
			XmlNode root = new XmlNode(xml);
			XmlNode temp = root.clone();
			List<XmlNode> configs = temp.getChildren("accessconfiguration");
			System.out.println(configs.size());
			for(XmlNode config:configs){
				System.out.println("BEFORE:: "+config.getChild("urlmappings/urlmapping/customizedurl").getText());
				config.getChild("urlmappings/urlmapping/customizedurl").setText("");
				System.out.println("AFTER:: "+config.getChild("urlmappings/urlmapping/customizedurl").getText());
			}
			
	}
	
}
