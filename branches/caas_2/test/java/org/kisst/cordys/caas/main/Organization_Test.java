package org.kisst.cordys.caas.main;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.ProcessModel;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.WebService;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.XMLStoreObject;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.StringUtil;

public class Organization_Test {

	private static CordysSystem system=null;
	private static Organization org=null;
	private static final String SYS_NAME="cordysVM";
	private static final String ORG_NAME="system";
	
	@BeforeClass
	public static void oneTimeSetUp() {
		String[] args = {"help"};
		CaasMainCommand cmd = new CaasMainCommand();
		cmd.run(args);
		system = Caas.getSystem(SYS_NAME);
		org = system.organizations.getByName(ORG_NAME);
		
	}
	
	@AfterClass
	public static void oneTimeTearDown() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Assert.assertNotNull("Connecting to "+SYS_NAME+" failed", system);
		Assert.assertNotNull(ORG_NAME+" doesn't exist in "+SYS_NAME, org);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Test
	public void assertUsers(){
		ChildList<User> users = org.users;
		Assert.assertNotNull(null,users);
		Assert.assertTrue(null,users.getSize()>0);
		for(User user:users){
			Assert.assertNotNull("User doesn't have authuser", user.authenticatedUser);
			Assert.assertFalse("User description cannot be null or empty", StringUtil.isEmptyOrNull(user.description.get()));
			Assert.assertNotNull("User roles cannot be null", user.roles);
			Assert.assertFalse("User dn cannot be null or empty", StringUtil.isEmptyOrNull(user.getDn()));
			Assert.assertFalse("User name cannot be null or empty", StringUtil.isEmptyOrNull(user.getName()));
			Assert.assertNotNull("User props be null", user.getProps());
			
		}
	}
	
	@Test
	public void assertDsos(){
		ChildList<Dso> dsos = org.dsos;
		Assert.assertNotNull(null,dsos);
		for(Dso dso:dsos){
			Assert.assertNotNull("Dso config cannot be null", dso.config);
			Assert.assertFalse("Dso description cannot be null or empty", StringUtil.isEmptyOrNull(dso.description.get()));
			Assert.assertFalse("Dso dn cannot be null or empty", StringUtil.isEmptyOrNull(dso.getDn()));
			Assert.assertFalse("Dso name cannot be null or empty", StringUtil.isEmptyOrNull(dso.getName()));
			Assert.assertNotNull("Dso props cannot be null", dso.getProps());
			Assert.assertFalse("Dso connectionstring cannot be null or empty", StringUtil.isEmptyOrNull(dso.connectionstring.get()));
			Assert.assertFalse("Dso defaultdb cannot be null or empty", StringUtil.isEmptyOrNull(dso.defaultdb.get()));
			Assert.assertFalse("Dso username cannot be bull or empty", StringUtil.isEmptyOrNull(dso.username.get()));
			Assert.assertFalse("Dso password cannot be null or empty", StringUtil.isEmptyOrNull(dso.password.get()));
		}
	}
	
	@Test
	public void assertRoles(){
		ChildList<Role> roles = org.roles;
		Assert.assertNotNull(null,roles);
		for(Role role:roles){
			Assert.assertFalse("Role description cannt be null or empty", StringUtil.isEmptyOrNull(role.description.get()));
			Assert.assertFalse("Role dn cannot be null or empty", StringUtil.isEmptyOrNull(role.getDn()));
			Assert.assertNotNull("Subroles cannot be null", role.roles);
			Assert.assertTrue("Subroles count must be min 0", role.roles.getSize()>=0);
			Assert.assertNotNull("Role properties cananot be null", role.getProps());
		}
	}
	
	@Test
	public void assertWebServiceInterfaces(){
		ChildList<WebServiceInterface> wsis = org.webServiceInterfaces;
		Assert.assertNotNull(null,wsis);
		for(WebServiceInterface wsi:wsis){
			//Assert.assertFalse("WebServiceInterface description cannot be null or empty", StringUtil.isEmptyOrNull(wsi.description.get()));
			Assert.assertFalse("WebServiceInterface implementationclass cannot be null or empty", StringUtil.isEmptyOrNull(wsi.implementationclass.get()));
			Assert.assertFalse("WebServiceInterface dn cannot be null or empty", StringUtil.isEmptyOrNull(wsi.getDn()));
			Assert.assertFalse("WebServiceInterface name cannot be null or empty", StringUtil.isEmptyOrNull(wsi.getName()));
			Assert.assertNotNull("No namespaces for WebServiceInterface",wsi.namespaces);
			Assert.assertNotNull("No webservices in WebServiceInterface",wsi.webServices);
			Assert.assertNotNull("WebServiceInterface props cannot be null",wsi.getProps());
			Assert.assertNotNull("WebServiceInterface system cannot be null",wsi.getSystem());
			for(WebService ws:wsi.webServices){
				//Assert.assertFalse("WebService description cannot be null or empty", StringUtil.isEmptyOrNull(ws.description.get()));
				//Assert.assertFalse("WebService iface cannot be null or empty", StringUtil.isEmptyOrNull(ws.iface.get()));
				Assert.assertFalse("WebService implementation cannot be null or empty", StringUtil.isEmptyOrNull(ws.implementation.get()));
				//Assert.assertFalse("WebService wsdl cannot be null or empty", StringUtil.isEmptyOrNull(ws.wsdl.get()));
				Assert.assertFalse("WebService signature cannot be null or empty", StringUtil.isEmptyOrNull(ws.signature.get()));
				Assert.assertFalse("WebService dn cannot be null",StringUtil.isEmptyOrNull(ws.getDn()));
				Assert.assertFalse("WebService name cannot be null",StringUtil.isEmptyOrNull(ws.getName()));
				Assert.assertNotNull("WebService props cannot be null",ws.getProps());
			}
			
		}
	}
	
	@Test
	public void assertServiceGroups(){
		ChildList<ServiceGroup> serviceGroups = org.serviceGroups;
		Assert.assertNotNull(null,serviceGroups);
		for(ServiceGroup sg:serviceGroups){
			//Assert.assertFalse("ServiceGroup description cannot be null or empty", StringUtil.isEmptyOrNull(sg.description.get()));
			Assert.assertFalse("ServiceGroup algorithm cannot be null or empty", StringUtil.isEmptyOrNull(sg.algorithm.get()));
			Assert.assertFalse("ServiceGroup config cannot be null or empty", StringUtil.isEmptyOrNull(sg.config.get()));
			Assert.assertFalse("ServiceGroup numprocessors cannot be null or empty", StringUtil.isEmptyOrNull(sg.numprocessors.get()));
			Assert.assertFalse("ServiceGroup payloadTrim cannot be null or empty", StringUtil.isEmptyOrNull(sg.payloadTrim.get()));
			Assert.assertFalse("ServiceGroup payloadValidation cannot be null or empty", StringUtil.isEmptyOrNull(sg.payloadValidation.get()));
			Assert.assertFalse("ServiceGroup protocolValidation cannot be null or empty", StringUtil.isEmptyOrNull(sg.protocolValidation.get()));
			Assert.assertFalse("ServiceGroup ui_algorithm cannot be null or empty", StringUtil.isEmptyOrNull(sg.ui_algorithm.get()));
			Assert.assertFalse("ServiceGroup ui_type cannot be null or empty", StringUtil.isEmptyOrNull(sg.ui_type.get()));
			Assert.assertFalse("ServiceGroup dn cannot be null or empty", StringUtil.isEmptyOrNull(sg.getDn()));
			Assert.assertFalse("ServiceGroup name cannot be null or empty", StringUtil.isEmptyOrNull(sg.getName()));
			Assert.assertTrue("Namespaces implemented by ServiceGroup should be minimum of 1",sg.namespaces.get().size()>0);
			Assert.assertTrue("ServiceGroup should have atleast 1 webserviceinterface attached",sg.webServiceInterfaces.getSize()>0);
			Assert.assertTrue("ServiceGroup should have atleast 1 servicecontainer",sg.serviceContainers.getSize()>0);
			Assert.assertNotNull("ServiceGroup props cannot be null",sg.getProps());
		}
	}
	
	@Test
	public void assertServiceContainers(){
		CordysObjectList<ServiceContainer>scs = org.serviceContainers;
		for(ServiceContainer sc:scs){
			Assert.assertFalse("ServiceContainer config cannot be null or empty", StringUtil.isEmptyOrNull(sc.config.get()));
			Assert.assertFalse("ServiceContainer computer cannot be null or empty", StringUtil.isEmptyOrNull(sc.computer.get()));
			Assert.assertFalse("ServiceContainer automatic cannot be null or empty", StringUtil.isEmptyOrNull(sc.automatic.get()));
			//Assert.assertFalse("ServiceContainer abortTime cannot be null or empty", StringUtil.isEmptyOrNull(sc.abortTime.get()));
			//Assert.assertFalse("ServiceContainer description cannot be null or empty", StringUtil.isEmptyOrNull(sc.description.get()));
			//Assert.assertFalse("ServiceContainer gracefulCompleteTime cannot be null or empty", StringUtil.isEmptyOrNull(sc.gracefulCompleteTime.get()));
			Assert.assertFalse("ServiceContainer implementation cannot be null or empty", StringUtil.isEmptyOrNull(sc.implementation.get()));
			//Assert.assertFalse("ServiceContainer preference cannot be null or empty", StringUtil.isEmptyOrNull(sc.preference.get()));
			//Assert.assertFalse("ServiceContainer host cannot be null or empty", StringUtil.isEmptyOrNull(sc.host.get()));
			Assert.assertFalse("ServiceContainer requestTimeout cannot be null or empty", StringUtil.isEmptyOrNull(sc.requestTimeout.get()));
			//Assert.assertFalse("ServiceContainer useSystemLogPolicy cannot be null or empty", StringUtil.isEmptyOrNull(sc.useSystemLogPolicy.get()));
			Assert.assertFalse("ServiceContainer dn cannot be null or empty", StringUtil.isEmptyOrNull(sc.getDn()));
			Assert.assertFalse("ServiceContainer name cannot be null or empty", StringUtil.isEmptyOrNull(sc.getName()));
			Assert.assertTrue("Atleast 1 connectionpoint must be there for a ServiceContainer", sc.connectionPoints.getSize()>0);
			Assert.assertNotNull("ServiceContainer props cannot be null", sc.getProps());
			if(sc.getStatus().toUpperCase()=="STARTED"){
				Assert.assertTrue("No PID ofr ServiceContainer", sc.getPid()>0);
				Assert.assertFalse("ServiceContainer CpuTime cannot be null or empty", StringUtil.isEmptyOrNull(sc.getCpuTime()));
				Assert.assertTrue("ServiceContainer soapdocs shouldn't be negative", sc.getBusdocs()>=0);		
				Assert.assertTrue("ServiceContainer LastTime value shouldn't be negative", sc.getLastTime()>=0);
				Assert.assertTrue("ServiceContainer NomMemory shouldn't be negative", sc.getNomMemory()>0);
				Assert.assertTrue("ServiceContainer VirtualMemory shouldn't be negative", sc.getVirtualMemory()>=0);
				Assert.assertTrue("ServiceContainer ProcessingTime shouldn't be negative", sc.getProcessingTime()>=0);
				Assert.assertTrue("ServiceContainer NomNodesMemory shouldn't be negative", sc.getNomNodesMemory()>0);
			}
		}
	}
	
	@Test
	public void assertXMLStoreObjects(){
		CordysObjectList<XMLStoreObject> xsos = org.xmlStoreObjects;
		Assert.assertNotNull(null,xsos);
		for(XMLStoreObject xso:xsos){
			Assert.assertFalse("XSO name cannot be empty or null ", StringUtil.isEmptyOrNull(xso.getName()));
			Assert.assertFalse("XSO key cannot be empty or null ", StringUtil.isEmptyOrNull(xso.getKey()));
			Assert.assertFalse("XSO version cannot be empty or null ", StringUtil.isEmptyOrNull(xso.getVersion()));
			Assert.assertNotNull("XSO props cannot be null", xso.getProps());
			Assert.assertNotNull("XSO xml cannot be null", xso.getXML());
		}
	}
	
	@Test
	public void assertProcessModels(){
		CordysObjectList<ProcessModel> processes = org.processes;
		Assert.assertNotNull(null,processes);
		for(ProcessModel process:processes){
			Assert.assertFalse("Process name cannot be empty or null ", StringUtil.isEmptyOrNull(process.getName()));
			Assert.assertNotNull("Process props cannot be null", process.getProps());
		}
	}
}
