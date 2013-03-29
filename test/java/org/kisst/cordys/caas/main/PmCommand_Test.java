package org.kisst.cordys.caas.main;


import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;

//@RunWith(value = Parameterized.class)
public class PmCommand_Test{  

	private static CordysSystem system=null;
	private static final String SYS_NAME="cordysUAT";
	private static final String ORG_NAME="RMG";
	
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
	

	@Test
	public void templateCommandTest() {
		String template_cmd="template -o "+ORG_NAME+" -s "+SYS_NAME+" D:/Users/galoori/config/caas/uat.caaspm";
		CmCommand pm = new CmCommand("cm");
		pm.run(template_cmd.split(" "));
	}
	

	@Ignore
	@Test
	public void createCommandTest() {
		String create_cmd="create -o "+ORG_NAME+" -s "+SYS_NAME+" D:/Users/galoori/config/caas/uat.caaspm";
		CmCommand pm = new CmCommand("cm");
		pm.run(create_cmd.split(" "));
	}
	
	@Test
	public void checkCommandTest() {
	}
	
	@Test
	public void configureCommandTest() {
	}
	
	@Test
	public void purgeCommandTest() {
	}	
}