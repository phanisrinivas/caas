package org.kisst.cordys.caas.cm;

import static org.kisst.cordys.caas.main.Environment.error;
import static org.kisst.cordys.caas.main.Environment.info;
import static org.kisst.cordys.caas.main.Environment.warn;

import java.util.List;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.util.XmlNode;

public class EntryObjective implements Objective {

	private final ObjectiveBase parent;
	public final String propName;
	public final String packageName;
	public final String name;
	private final Organization org;

	public EntryObjective(Organization org, ObjectiveBase parent, String propName, XmlNode node) {
		this.org=org;
		this.parent=parent;
		this.propName=propName;
		this.packageName=node.getAttribute("package", null);
		this.name=node.getAttribute("name");
	}
	@Override public String toString() { return "package.\""+packageName+"\"."+propName+"."+name; } 
	public LdapObject findEntry() {
		LdapObject result=null;
		if (packageName==null || packageName.length()==0)
			result = ((ChildList<?>) org.getProp(propName)).getByName(name);
		else {
			CordysSystem system=org.getSystem();
			Package pkg = system.packages.getByName(packageName);
			if (pkg==null) {
				error(parent+" should refer to UNKNOWN package \""+packageName+"\" in entry "+this);
				return null;
			}
			if (pkg!=null)
				result=((ChildList<?>) pkg.getProp(propName)).getByName(name);
		}
		if (result==null)
			error(parent+" should refer to UNKNOWN entry "+this);
		return result;
	}
	
	private int status=OK;
	private String message=null;
	public String getMessage() { return message; }
	public List<Objective> getChildren() { return null;	}

	public int getStatus() { return status; }
	public CordysSystem getSystem() { return parent.getSystem(); }

	public int check(Ui ui) {
		ui.checking(this);
		LdapObject entry = findEntry();
		if (parent.contains(entry)) {
			ui.info(this,"target "+parent+" has entry "+this);
			message=null;
			status=OK;
		}
		else {
			message="target "+parent+" should have entry "+this;
			ui.error(this, message);
			status = ERROR;
		}
		ui.readyWith(this);
		return status;
	}
	
	public void configure(Ui ui) {
		ui.configuring(this);
		LdapObject entry=findEntry(); 
		if (entry==null)
			warn("target "+parent+" should have unknown entry "); // TODO: +entry.getVarName());
		else if (parent.contains(entry))
			info("target "+parent+" already has entry "+entry.getVarName());
		else
			parent.add(entry);
		ui.readyWith(this);
	}
	
	public void purge(Ui ui) {
		LdapObject entry=findEntry(); 
		if (entry==null)
			warn("target "+this+" should have unknown entry "); // TODO: +entry.getVarName());
		else if (! parent.contains(entry))
			warn("target "+this+" does not have entry "+entry.getVarName());
		else
			parent.remove(entry); 
	}
}
