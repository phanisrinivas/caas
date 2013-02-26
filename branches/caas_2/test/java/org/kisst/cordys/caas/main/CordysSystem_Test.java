package org.kisst.cordys.caas.main;


import java.io.File;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.util.StringUtil;


public class CordysSystem_Test{  

	private static CordysSystem system=null;
	private static final String SYS_NAME="cordysVM";
	private static String ISVP_PATH="D:/Users/galoori/Desktop/isvps/RMG OrderPublisher.isvp";
	private static String NEW_ISVP_PATH=null;
	private static String NOPROMPTSET_ISVP_PATH="D:/Users/galoori/Desktop/isvps/Capgemini Immediate.isvp";
	private static String NEW_NOPROMPTSET_ISVP_PATH=null;
	private static String PROMPTSET_FILE_PATH="D:/buildframework/deploy/caas/config/promptsets.xml";
	private static long TIMEOUT_IN_MINUTES=10;
	
	@BeforeClass
	public static void oneTimeSetUp() {
		String[] args = {"help"};
		CaasMainCommand cmd = new CaasMainCommand();
		cmd.run(args);
		system = Caas.getSystem(SYS_NAME);
		
	}
	@AfterClass
	public static void oneTimeTearDown() throws Exception {
	}

	@Before
	public void setUp() throws Exception {
		Assert.assertNotNull("Connecting to "+SYS_NAME+" failed", system);
	}

	@After
	public void tearDown() throws Exception {
	}
	
	@Ignore
	@Test
	public void loadIsvpTest(){
		File file = new File(NOPROMPTSET_ISVP_PATH);
		Assert.assertTrue("ISVP doesn't exist at "+NOPROMPTSET_ISVP_PATH,file.exists());
		String isvpName = file.getName().substring(0,file.getName().indexOf(".isvp"));
		int beforeCount = system.isvps.getSize();
		Assert.assertNull(isvpName+" ISVP is installed already", system.isvps.getByName(isvpName));
		system.loadIsvp(NOPROMPTSET_ISVP_PATH, TIMEOUT_IN_MINUTES);
		Assert.assertNotNull(isvpName+" ISVP is not installed", system.isvps.getByName(isvpName));
		int afterCount = system.isvps.getSize();
		Assert.assertTrue("ISVPs count must be incremented by 1 after successful installation", beforeCount+1==afterCount);
	}
	
	@Ignore
	@Test
	public void upgradeIsvpTest(){
		File file = new File(NEW_NOPROMPTSET_ISVP_PATH);
		Assert.assertTrue("ISVP doesn't exist at "+NEW_NOPROMPTSET_ISVP_PATH,file.exists());
		String isvpName = file.getName().substring(0,file.getName().indexOf(".isvp"));		
		Assert.assertNotNull(isvpName+" Can not upgrade ISVP as it is not installed already", system.isvps.getByName(isvpName));
		system.upgradeIsvp(NEW_NOPROMPTSET_ISVP_PATH);
		Assert.assertNotNull(isvpName+" ISVP is not installed", system.isvps.getByName(isvpName));
		//unload
		system.unloadIsvp(isvpName,true);
	}

	@Ignore
	@Test
	public void loadIsvpWithPromptSetTest(){
		File file = new File(ISVP_PATH);
		Assert.assertTrue("ISVP doesn't exist at "+ISVP_PATH,file.exists());
		String isvpName = file.getName().substring(0,file.getName().indexOf(".isvp"));		
		int beforeCount = system.isvps.getSize();
		Assert.assertNull(isvpName+" ISVP is installed already", system.isvps.getByName(isvpName));
		system.loadIsvp(ISVP_PATH, PROMPTSET_FILE_PATH, TIMEOUT_IN_MINUTES);
		Assert.assertNotNull(isvpName+" ISVP is not installed", system.isvps.getByName(isvpName));
		int afterCount = system.isvps.getSize();
		Assert.assertTrue("ISVPs count must be incremented by 1 after successful installation", beforeCount+1==afterCount);
	}
	
	@Ignore
	@Test
	public void upgradeIsvpWithPromptSetTest(){
		File file = new File(NEW_ISVP_PATH);
		Assert.assertTrue("ISVP doesn't exist at "+NEW_ISVP_PATH,file.exists());
		String isvpName = file.getName().substring(0,file.getName().indexOf(".isvp"));			
		Assert.assertNotNull(isvpName+" Can not upgrade ISVP as it is not installed already", system.isvps.getByName(isvpName));
		system.upgradeIsvp(NEW_ISVP_PATH, PROMPTSET_FILE_PATH);
		Assert.assertNotNull(isvpName+" ISVP is not installed", system.isvps.getByName(isvpName));
		//unload
		system.unloadIsvp(isvpName,true);
	}
	
	@Test
	public void refreshSoapProcessorsTest(){
		system.refreshServiceContainers();
	}
	
	@Test
	public void assertIsvps(){
		ChildList<Package> isvps = system.isvps;
		Assert.assertNotNull(null,isvps);
		Assert.assertTrue(null,isvps.getSize()>0);
		for(Package isvp:isvps){
			if(isvp.owner.get().toLowerCase().contains("RMG")){
				System.out.println(isvp.getName());
				Assert.assertFalse("ISVP description cannot be null or empty",StringUtil.isEmptyOrNull(isvp.description.get()));
				Assert.assertFalse("ISVP filename cannot be null or empty",StringUtil.isEmptyOrNull(isvp.member.get()));
				Assert.assertFalse("ISVP owner cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getOwner2()));
				Assert.assertNotNull("No roles in ISVP",isvp.roles);
				Assert.assertNotNull("No webservices in ISVP",isvp.webServiceInterfaces);
				Assert.assertFalse("ISVP basename cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getBasename()));
				Assert.assertFalse("ISVP version cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getVersion()));
				Assert.assertFalse("ISVP buildnumber cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getBuildnumber()));
				Assert.assertNotNull("ISVP content cannot be null",isvp.getContent());
				Assert.assertNotNull("ISVP definition cannot be null",isvp.getDefinition());
				Assert.assertFalse("ISVP dn cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getDn()));
				Assert.assertFalse("ISVP name cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getName()));
				Assert.assertFalse("ISVP name2 cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getName2()));
				Assert.assertFalse("ISVP ownser2 cannot be null or empty",StringUtil.isEmptyOrNull(isvp.getOwner2()));
				Assert.assertNotNull("ISVP props cannot be null",isvp.getProps());
				Assert.assertNotNull("ISVP system cannot be null",isvp.getSystem());
			}
		}
	}
	
	@Test 
	public void assertOrganizations(){
		ChildList<Organization> orgs = system.organizations;
		Assert.assertNotNull(null,orgs);
		Assert.assertTrue(null,orgs.getSize()>0);
		for(Organization org:orgs){
			Assert.assertFalse("Org descripton cannot be null",StringUtil.isEmptyOrNull(org.description.get()));
			Assert.assertNotNull("Org dsos cannot be null",org.dsos);
			Assert.assertNotNull("Org processes cannot be null",org.processes);
			Assert.assertNotNull("Org roles cannot be null",org.roles);
			Assert.assertNotNull("Org servicecontainers cannot be null",org.serviceContainers);
			Assert.assertNotNull("Org servicegroups cannot be null",org.serviceGroups);
			Assert.assertNotNull("Org users cannot be null",org.users);
			Assert.assertNotNull("Org xmlstoreobjects cannot be null",org.xmlStoreObjects);
			Assert.assertFalse("Org dn cannot be null",StringUtil.isEmptyOrNull(org.getDn()));
			Assert.assertFalse("Org name cannot be null",StringUtil.isEmptyOrNull(org.getName()));
			Assert.assertNotNull("Org props cannot be null",org.getProps());
			Assert.assertNotNull("Org system cannot be null",org.getSystem());
		}
	}
	
	@Test
	public void assertAuthUsers(){
		ChildList<AuthenticatedUser> authUsers = system.authenticatedUsers;
		Assert.assertNotNull(null,authUsers);
		Assert.assertTrue(null,authUsers.getSize()>0);
		for(AuthenticatedUser authUser:authUsers){
			Assert.assertFalse("Authuser defaultorg cannot be null or empty",StringUtil.isEmptyOrNull(authUser.defaultOrg.get()));
			Assert.assertFalse("Authuser description cannot be null or empty",StringUtil.isEmptyOrNull(authUser.description.get()));
			Assert.assertTrue("No osidentity for Authuser",authUser.osidentity.get().size()>0);
			Assert.assertFalse("Authuser dn cannot be null or empty",StringUtil.isEmptyOrNull(authUser.getDn()));
			Assert.assertFalse("Authuser name cannot be null or empty",StringUtil.isEmptyOrNull(authUser.getName()));
			Assert.assertNotNull("Authuser props cannot be null",authUser.getProps());
		}
	}
}