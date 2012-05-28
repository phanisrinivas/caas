package org.kisst.cordys.caas.pm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.kisst.cordys.caas.AuthenticatedUser;
import org.kisst.cordys.caas.Configuration;
import org.kisst.cordys.caas.ConnectionPoint;
import org.kisst.cordys.caas.Dso;
import org.kisst.cordys.caas.Isvp;
import org.kisst.cordys.caas.Machine;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Role;
import org.kisst.cordys.caas.ServiceGroup;
import org.kisst.cordys.caas.ServiceContainer;
import org.kisst.cordys.caas.User;
import org.kisst.cordys.caas.XMLStoreObject;
import org.kisst.cordys.caas.exception.CaasRuntimeException;
import org.kisst.cordys.caas.main.Environment;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;
import org.kisst.cordys.caas.support.CordysObject;

public class Template {
	private static final Environment env=Environment.get();
	private final String template;
	public Template(String template) {this.template=template; }
	
	public Template(Organization org, String targetIsvpName) {
		
		System.out.println("Exporting template for "+org.getName()+" organization");
		XmlNode result=new XmlNode("org");
		//result.setAttribute("isvp", isvpName);
		result.setAttribute("org", org.getName());

		System.out.print("Exporting "+org.dsos.getSize()+" dso objects ... ");
		for(Dso dso:org.dsos){
			XmlNode node=result.add("dso");
			node.setAttribute("name", dso.getName());
			node.setAttribute("desc", dso.getProp("desc").toString());
			node.setAttribute("type", "Relational");
			XmlNode configNode = dso.config.getXml().clone();
			node.add("datasourceconfiguration").add(configNode);
		}
		System.out.println("OK");
		System.out.print("Exporting "+org.xmlStoreObjects.getSize()+" xmlstore objects ... ");
		for(XMLStoreObject xso: org.xmlStoreObjects){
			XmlNode node=result.add("xmlstoreobject");
			node.setAttribute("key", xso.getKey());
			node.setAttribute("version", xso.getVersion());
			node.setAttribute("name", xso.getName());
			//XmlNode temp = parameterize(xso,org.getSystem().getProperties());
			//node.add(temp);
			node.add(xso.getXml().clone());
		}
		System.out.println("OK");
		System.out.print("Exporting "+org.roles.getSize()+" roles ... ");
		for (Role role : org.roles) {
			XmlNode node=result.add("role");
			node.setAttribute("name", role.getName());
			for (Role subRole: role.roles) {
				String isvpName=null;
				if (subRole.getParent() instanceof Organization) {
					if (subRole.getName().equals("everyoneIn"+org.getName()))
						continue;
					isvpName=targetIsvpName;
				}
				else
					isvpName=subRole.getParent().getName();
				XmlNode child=node.add("role");
				child.setAttribute("name", subRole.getName());
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
		}
		System.out.println("OK");
		System.out.print("Exporting "+org.users.getSize()+" users ... ");
		for (User user : org.users) {
			if ("SYSTEM".equals(user.getName().toUpperCase()))
				continue; // SYSTEM user should not be part of the template
			XmlNode node=result.add("user");
			node.setAttribute("name", user.getName());
			node.setAttribute("au", user.au.getRef().getName());
			for (Role role: user.roles) {
				String isvpName=null;
				if (role.getParent() instanceof Organization) {
					if (role.getName().equals("everyoneIn"+org.getName()))
						continue;
					isvpName=targetIsvpName;
				}
				else
					isvpName=role.getParent().getName();
				XmlNode child=node.add("role");
				child.setAttribute("name", role.getName());
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
		}
		System.out.println("OK");		
		System.out.print("Exporting "+org.serviceGroups.getSize()+" service groups ... ");
		for (ServiceGroup serviceGroup : org.serviceGroups) {
			XmlNode node=result.add("soapnode");
			node.setAttribute("name", serviceGroup.getName());
			XmlNode configNode = serviceGroup.config.getXml().clone();
			XmlNode keyStoreNode = configNode.getChild("soapnode_keystore");
			if(keyStoreNode!=null)
				configNode.remove(keyStoreNode);
			node.add("bussoapnodeconfiguration").add(configNode);
			for (WebServiceInterface ms: serviceGroup.webServiceInterfaces) {
				XmlNode child=node.add("ms");
				child.setAttribute("name", ms.getName());
				String isvpName=null;
				if (ms.getParent() instanceof Organization)
					isvpName=targetIsvpName;
				else
					isvpName=ms.getParent().getName();
				if (isvpName!=null)
					child.setAttribute("isvp", isvpName);
			}
			for (ServiceContainer serviceContainer: serviceGroup.serviceContainers) {
				XmlNode child=node.add("sp");
				child.setAttribute("name", serviceContainer.getName());
				child.setAttribute("automatic", ""+serviceContainer.automatic.getBool());
				child.add("bussoapprocessorconfiguration").add(serviceContainer.config.getXml().clone());
				for (ConnectionPoint cp: serviceContainer.connectionPoints) {
					XmlNode cpNode=child.add("cp");
					cpNode.setAttribute("name", cp.getName());
				}
			}
		}
		System.out.println("OK");
		
		String str=result.getPretty();
		this.template=str.replace("$", "${dollar}");
	}//End of Template constructor

	/*private XmlNode parameterize(XMLStoreObject xso, Properties props){
		XmlNode node = xso.getXml().clone();
		String name = xso.getName();
		//Web services customizations
		if(name.equals("webservicecustomization")){
			List<XmlNode> configs = node.getChildren("accessconfiguration");
			for(XmlNode config:configs){
				String dn = config.getAttribute("methodsetdn");
				if(dn==null) throw  new CaasRuntimeException("Invalid entry in webservicecustomization.xml file");
				XmlNode urlNode = config.getChild("urlmappings/urlmapping/customizedurl");
				if(urlNode!=null){
					props.setProperty(dn.concat("#url").toUpperCase(), urlNode.getText());
					urlNode.setText("${"+dn.concat("#url").toUpperCase()+"}");
				}
			}
		}
		return node;
	}*/
	
	public void save(String filename) { 
		FileUtil.saveString(new File(filename), template);
		System.out.println("Template successfully exported to "+filename);
	}

	public XmlNode xml( Map<String, String> vars) {
		String str=template;
		if (vars!=null)
			str=StringUtil.substitute(str, vars);
		str=str.replace("${dollar}", "$");
		return new XmlNode(str);
	}
	public void apply(Organization org, Configuration conf) { apply(org, conf.getProps()); }
	public void apply(Organization org, Map<String, String> vars) {
		XmlNode template=xml(vars);
		for (XmlNode node : template.getChildren()){
			if (node.getName().equals("dso"))
				processDso(org, node);
			else if (node.getName().equals("xmlstoreobject"))
				processXMLStoreObject(org, node);
			else if (node.getName().equals("role"))
				processRole(org, node);			
			else if (node.getName().equals("user"))
				processUser(org, node);
			else if ((node.getName().equals("soapnode"))|| (node.getName().equals("servicegroup")))
				processServiceGroup(org, node);
			else
				System.out.println("Unknown organization element "+node.getPretty());
		}
	}

	/**
	 * Processes the XMLStore operations 
	 * 
	 * @param org
	 * @param node
	 */
	private void processXMLStoreObject(Organization org, XmlNode node) {
			String operationFlag = node.getAttribute("operation");
			String key = node.getAttribute("key");
			String version = node.getAttribute("version");
			String name = node.getAttribute("name");
			env.info("Processing "+name+" xmlstore object");
			XmlNode newXml = node.getChildren().get(0);
			XMLStoreObject obj = new XMLStoreObject(key,version,org);
			if(operationFlag!=null && operationFlag.equals("overwrite"))
				obj.overwriteXML(newXml.clone());
			else if(operationFlag!=null && operationFlag.equals("append"))
				obj.appendXML(newXml.clone());
			//By default overwrite the XMStore object
			else
				obj.overwriteXML(newXml.clone());
			env.info("Processing "+name+" xmlstore object is completed");
	}
	
	private void processDso(Organization org, XmlNode dsoNode){
		String name = dsoNode.getAttribute("name");
		String desc = dsoNode.getAttribute("desc");
		String type = dsoNode.getAttribute("type");
		Dso dso = org.dsos.getByName(name);
		XmlNode config=dsoNode.getChild("datasourceconfiguration").getChildren().get(0).clone();		
		if(dso==null){		//Create DSO
			env.info("creating dso "+name);
			org.createDso(name, desc, type,config);
			env.info("dso "+name+" created successfully");
		}else{				//Update DSO
			env.info("updating dso "+name);
			org.updateDso(dso, config);
			env.info("dso "+name+" updated successfully");
		}
	}

	private void processServiceGroup(Organization org, XmlNode serviceGroupNode) 
	{	
		//Process SG
		String name=serviceGroupNode.getAttribute("name");
		ServiceGroup serviceGroup=org.serviceGroups.getByName(name);
		if (serviceGroup==null){	//Create SG
				env.info("creating servicegroup "+name);
				XmlNode config=serviceGroupNode.getChild("bussoapnodeconfiguration").getChildren().get(0).clone();
				org.createServiceGroup(name, config, getWebServiceInterfaces(org,serviceGroupNode));
				serviceGroup=org.serviceGroups.getByName(name);
				env.info("servicegroup "+name+" created successfully");
		}else{						//Update SG
				env.info("updating servicegroup "+name);
				WebServiceInterface[] newWebServiceInterfaces = getWebServiceInterfaces(org,serviceGroupNode);
				if ((newWebServiceInterfaces!=null)&&(newWebServiceInterfaces.length > 0))
				{
					serviceGroup.webServiceInterfaces.update(newWebServiceInterfaces);
					ArrayList<String> namepsaces = new ArrayList<String>();
					for (WebServiceInterface webServiceInterface : newWebServiceInterfaces) {				
						for (String namespace : webServiceInterface.namespaces.get()) {
							namepsaces.add(namespace);
						}
					}			
					serviceGroup.namespaces.update(namepsaces);
					env.info("servicegroup "+name+" updated successfully");
				}
		}
		//Process SCs
		CordysObjectList<Machine> machines = org.getSystem().machines; 		int i=0;
		if(serviceGroupNode.getChildren("sp").size()!=machines.getSize()){
			env.warn("Template says "+serviceGroupNode.getChildren("sp").size()+" servicecontainers for "+name+" but the no of machines are "+org.getSystem().machines.getSize());
		}
		for (XmlNode serviceContainerNode:serviceGroupNode.getChildren("sp")){			
					String scName=serviceContainerNode.getAttribute("name");
					String machineName = machines.get(i++).getName();
					XmlNode configsNode = serviceContainerNode.getChild("bussoapprocessorconfiguration/configurations");
					ServiceContainer serviceContainer = serviceGroup.serviceContainers.getByName(scName);
					if (serviceContainer==null) {	//Create SC
						env.info("creating servicecontainer "+scName+"' for machine '"+machineName+"'");
						boolean automatic="true".equals(serviceContainerNode.getAttribute("automatic"));						
						serviceGroup.createServiceContainer(scName, machineName, automatic, configsNode.clone());
						for (XmlNode subchild:serviceContainerNode.getChildren()) {
							if (subchild.getName().equals("cp")) {
								ServiceContainer newSC=serviceGroup.serviceContainers.getByName(scName);
								newSC.createConnectionPoint(subchild.getAttribute("name"),machineName);
							}	
						}						
					}else{							//Update SC
						env.info("updating servicecontainer '"+scName+"' for machine '"+machineName+"'");
						boolean automatic="true".equals(serviceContainerNode.getAttribute("automatic"));						
						serviceGroup.updateServiceContainer(scName, machineName, automatic, configsNode.clone(),serviceContainer);
					}
		}
	}

	private WebServiceInterface[] getWebServiceInterfaces(Organization org, XmlNode serviceGroupNode) {
		ArrayList<WebServiceInterface> result=new ArrayList<WebServiceInterface>();
		for (XmlNode child:serviceGroupNode.getChildren()) {
			if ((child.getName().equals("ms")) ) {
				WebServiceInterface newWSI=null;
				String isvpName=child.getAttribute("isvp");
				String wsiName=child.getAttribute("name");
				if (isvpName==null) {
					newWSI=org.webServiceInterfaces.getByName(wsiName);
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						newWSI=isvp.webServiceInterfaces.getByName(wsiName);
				}
				if (newWSI!=null) {
					result.add(newWSI);
				}
				else {
					env.error("Skipping unknownn methodset "+wsiName);
				}
			}
		}
		return result.toArray(new WebServiceInterface[result.size()]);
	}
	private void processUser(Organization org, XmlNode node) 
	{
		String name=node.getAttribute("name");
		if ("SYSTEM".equals(name.toUpperCase())) {
			/*Whenever I had a SYSTEM user in my template, Cordys would crash pretty hard.
			It would not be possible to start the monitor anymore.
			I had to use the CMC to remove the organization before the Monitor would start again.*/
			env.error("Ignoring user "+name+" because the SYSTEM user should not be modified from a template");
			return;
		}
		if (org.users.getByName(name)==null) {	//Create User 
			AuthenticatedUser authUser=org.getSystem().authenticatedUsers.getByName(node.getAttribute("au"));
			env.info("creating user '"+name+"'");
			org.createUser(name, authUser);	//Create Org User
		}
		else
			env.info("User '"+name+"' is already existing. Configuring user with roles");

		//Assigning roles
		User user=org.users.getByName(name);
		ArrayList<String> newRoles = new ArrayList<String>();
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role role=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("Adding role '"+roleName+"' to the user '"+user.getName()+"'");
				String dnRole=null;
				if (isvpName==null) {	//Assign organizational role if the isvp name is not mentioned
					role=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}else {					//Assign ISVP role
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						role=isvp.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn(); 
				}
				if (role!=null)
					newRoles.add(role.getDn());
				else
					newRoles.add(dnRole);
			}
			else
				System.out.println("Unknown user subelement "+child.getPretty());
		}
		//Assign all the roles to the user at once
		if(newRoles!=null && newRoles.size()>0)
			user.roles.add(newRoles.toArray(new String[newRoles.size()]));
	}
	
		
	private void processRole(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		if (org.roles.getByName(name)==null) {
			env.info("creating role "+name);
			org.createRole(name);
		}
		else
			env.info("configuring role "+name);
		Role role=org.roles.getByName(name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role subRole=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  adding role "+roleName);

				String dnRole=null;
				if (isvpName==null) {
					subRole=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						subRole=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (subRole!=null)
					role.roles.add(subRole);
				else
					role.roles.add(dnRole);
			}
			else
				System.out.println("Unknown role subelement "+child.getPretty());
		}
	}
	/*
	private Role[] getRoles(Organization org, XmlNode node) {
		ArrayList<Role> result=new ArrayList<Role>();
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role r=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				String dnRole=null;
				if (isvpName==null) {
					r=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						r=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (r!=null)
					result.add(r);
				else
					env.error("  skipping unknown role "+roleName);
			}
		}
		return result.toArray(new Role[result.size()]);
	}
	*/
	
	public void check(Organization org, Configuration conf) { check(org, conf.getProps()); }
	public void check(Organization org, Map<String, String> vars) {
		XmlNode template=xml(vars);
		for (XmlNode node : template.getChildren()){
			if (node.getName().equals("soapnode"))
				checkServiceGroup(org, node);
			else if (node.getName().equals("user"))
				checkUser(org, node);
			else if (node.getName().equals("role"))
				checkRole(org, node);
			else
				System.out.println("Unknown organization element "+node.getPretty());
		}
	}

	private void checkServiceGroup(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		ServiceGroup serviceGroup=org.serviceGroups.getByName(name);
		if (serviceGroup==null) {
			env.error("Missing ServiceGroup "+name);
			return;
		}
		env.info("Checking configuration of ServiceGroup "+name);
		WebServiceInterface[] target = getWebServiceInterfaces(org,node);
		for (WebServiceInterface wsi : target){
			if (! serviceGroup.webServiceInterfaces.contains(wsi))
				env.error("ServiceGroup "+serviceGroup+" does not contain WebServiceInterface "+wsi);
		}
		for (WebServiceInterface wsi : serviceGroup.webServiceInterfaces){
			boolean found=false;
			for (WebServiceInterface wsi2: target) {
				if (wsi.getDn().equals(wsi2.getDn())) 
					found=true;
			}
			if (!found)
				env.error("ServiceGroup "+serviceGroup+" contains WebServiceInterface "+wsi+" that is not in template");
		}
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("ms"))
				continue;
			else if (child.getName().equals("sp")) {
				String scName=child.getAttribute("name");
				ServiceContainer serviceContainer = serviceGroup.serviceContainers.getByName(scName);
				if (serviceContainer==null) {
					env.error("Missing ServiceContainer "+scName);
					continue;
				}
				boolean automatic="true".equals(child.getAttribute("automatic"));
				if (serviceContainer.automatic.getBool() != automatic)
					env.error("  "+serviceContainer+" property automatic, template says "+automatic+" while current value is "+serviceContainer.automatic.get());
				XmlNode config=child.getChild("bussoapprocessorconfiguration").getChildren().get(0);
				XmlNode configsp=serviceContainer.config.getXml();
				for (String msg: config.diff(configsp)) 
					env.error(msg);
			}
			else if (child.getName().equals("bussoapnodeconfiguration")) {}
			else
				env.error("Unknown soapnode subelement "+child.getPretty());
		}
	}

	private void checkUser(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		User user=org.users.getByName(name);
		if (user==null) {
			env.info("Unknown user "+name);
			return;
		}
		env.info("Checking roles of user "+name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role role=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("Checking role "+roleName);
				String dnRole=null;
				if (isvpName==null) {
					role=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}else{
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						role=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (role==null)
					env.error("User "+user+" should have unknown role "+dnRole);
				else if (! user.roles.contains(role))
					env.error("User "+user+" does not have role "+role);
			}
			else
				env.error("Unknown user subelement "+child.getPretty());
		}
	}

	private void checkRole(Organization org, XmlNode node) {
		String name=node.getAttribute("name");
		Role role=org.roles.getByName(name);
		if (role==null) {
			env.info("Unknowm role "+name);
			return;
		}
		env.info("Checking roles of role "+name);
		for (XmlNode child:node.getChildren()) {
			if (child.getName().equals("role")) {
				Role subRole=null;
				String isvpName=child.getAttribute("isvp");
				String roleName=child.getAttribute("name");
				env.info("  adding role "+roleName);

				String dnRole=null;
				if (isvpName==null) {
					subRole=org.roles.getByName(roleName);
					dnRole="cn="+roleName+",cn=organizational roles,"+org.getDn();
				}
				else {
					Isvp isvp=org.getSystem().isvp.getByName(isvpName);
					if (isvp!=null)
						subRole=isvp.roles.getByName(roleName);
					else
						dnRole="cn="+roleName+",cn="+isvpName+","+org.getSystem().getDn();
				}
				if (subRole==null)
					env.error("Role "+role+" should have unknown role "+dnRole);
				else if (! role.roles.contains(subRole))
					env.error("Role "+role+" does not have role "+subRole);
			}
			else
				env.error("Unknown role subelement "+child.getPretty());
		}
	}
	
	//NOTE: Please suggest a better way of doing it
	/**
	 * This webService replaces the string 'CORDYS_INSTALL_DIR' from the classpath <param> node in <jreconfig>
					<jreconfig>
                        <param value="-cp ${CORDYS_INSTALL_DIR}\Immediate\immediate.jar" />
                    </jreconfig>
	 * with its corresponding value.
	 * 
	 * @param configNode The <configurations> node of the <sc>
	 * @param cordysInstallDir Path of the Cordys installation directory
	 */
	private boolean resolveCordysInstallDir(XmlNode configNode, String cordysInstallDir){
				
		XmlNode jreConfigNode = configNode.getChild("jreconfig");
		//Check if the node is null or not to avoid NullPointerException
		if(jreConfigNode==null) return false;
		for(XmlNode param:jreConfigNode.getChildren()){
			String attrValue = param.getAttribute("value");
			if(attrValue.contains("-cp")){
				attrValue = StringUtil.getUnixStyleFilePath(attrValue);
				cordysInstallDir = StringUtil.getUnixStyleFilePath(cordysInstallDir);
				attrValue = attrValue.replaceAll("CORDYS_INSTALL_DIR", cordysInstallDir);
				//Overwrite the existing value with the replaced one
				param.setAttribute("value", attrValue);
			}
		}
		return true;
	}

}
