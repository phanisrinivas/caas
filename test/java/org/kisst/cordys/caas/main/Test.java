package org.kisst.cordys.caas.main;

import org.kisst.cordys.caas.Caas;
import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.DsoType;
import org.kisst.cordys.caas.Isvp;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.XMLStoreObject;

public class Test {
	private static CordysSystem system=null;
	private static Organization org=null;
	private static final String SYS_NAME="cordysUAT";
	private static final String ORG_NAME="RMG";
	private static void init(){
		String[] args = {"help"};
		CaasMainCommand cmd = new CaasMainCommand();
		cmd.run(args);
		system = Caas.getSystem(SYS_NAME);
		org = system.organizations.getByName(ORG_NAME);
	}
	public void dsoTest(){
		init();
		System.out.println("Using dsotype....");
		for(DsoType dsotype:org.dsotypes){
			System.out.println("DsoType name::"+dsotype.getName());
			System.out.println("DsoType dn:: "+dsotype.getDn());
			System.out.println("DsoType impl:: "+dsotype.implementationclass);
			for(Dso dso:dsotype.dsos){
				System.out.println("Dso db:: "+dso.defaultdb);
				System.out.println("Dso connstring:: "+dso.connectionstring);
			}
		}
		System.out.println("Using org....");
		for(Dso dso:org.dsos){
			System.out.println("Dso db:: "+dso.defaultdb);
			System.out.println("Dso connstring:: "+dso.connectionstring);
			System.out.println(dso.config);
		}
	}
	public void stopStartTest(){
		init();
		org.sc.getByName("Event Handling_2").stop();
		org.sc.getByName("Event Handling").stop();
		org.sc.getByName("Event Handling_2").start();
		org.sc.getByName("Event Handling").start();
	}
	
	public void pmcheckTest(){
		String str = "pm check -s cordysUAT -o RMG D:/Users/galoori/Desktop/rmg_ebus_int_deploy/trunk/recipes/uat/config.caaspm";
		CaasMainCommand cmd = new CaasMainCommand();
		cmd.run(str.split(" "));
	}
	
	public void xsoTest(){
		init();
		for(Organization org: system.org){
			System.out.println(org.xso.getSize());
			for(XMLStoreObject xso:org.xso){
				System.out.println(xso.getKey());
				System.out.println(xso.getName());
				System.out.println(xso.getLastModified());
				System.out.println(xso.getXML());
				xso.delete();
			}
			org.xso.clear();
			System.out.println(org.xso.getSize());	
		}
	}
	
	public void sgDeleteTest(){
		init();
		System.out.println(org.sg.getSize());
		for(ServiceGroup sg:org.sg){
			System.out.println(sg.getName());
			sg.delete();
		}
		org.sg.clear();
		System.out.println(org.sg.getSize());
	}
	
	public void isvpsTest(){
		init();
		System.out.println(system.isvps.getSize());
		for(Isvp isvp:system.isvps){
			System.out.println(isvp.getName());
		}
		System.out.println(system.isvps.getByName("Cordys JMS Connector 1.1"));
	}
	
	public void userDeleteTest(){
		init();
		org.users.getByName("CORDYSADMIN").delete();
		
	}
	
	public static void main(String[] args){
		
		new Test().userDeleteTest();
	}

}
