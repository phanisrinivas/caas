package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Class to represent a Business Process Model
 *
 */
public class ProcessModel extends CordysObject 
{
	private final String name;
	private final Organization org;
	private final CordysSystem system;
	private final String modelSpace;
	
	
	/**
	 * Constructs the Process Model in the given organization and with the given configuration
	 * 
	 * @param org
	 * @param tuple
	 */
	public ProcessModel(Organization org, XmlNode tuple) 
	{
		this.org=org;
		this.system=org.getSystem();
		this.name=tuple.getChildText("old/bizprocess/processname");
		this.modelSpace=tuple.getChildText("old/bizprocess/processname/?@modelSpace");
	}
	
	/**
	 * Class to retrieve list of all process models in the organization
	 *
	 */
	public static class List extends CordysObjectList<ProcessModel> 
	{
		private final Organization org;
		public List(Organization org) 
		{ 
			super(org.getSystem());
			this.org=org;
		}
		@Override protected void retrieveList() 
		{
			XmlNode  request=new XmlNode(Constants.GET_ALL_PROCESS_MODELS, Constants.XMLNS_COBOC);
			XmlNode response = org.call(request);
			for (XmlNode tuple :response.getChild("data").getChildren("tuple")) 
			{
				ProcessModel processModel=new ProcessModel(org, tuple);
				grow(processModel);
			}
		}
		@Override public String getKey() 
		{ 
			return "processModels:"+org.getKey(); 
		}
	};

	@Override public String getName() 
	{ 
		return name; 
	}
	@Override public String getKey() 
	{ 
		return "processmodel:"+name; 
	}
	@Override public CordysSystem getSystem() 
	{ 
		return system;	
	}
	@Override public String getVarName() 
	{ 
		return org.getVarName()+".proc."+StringUtil.quotedName(name); 
	}

	public void delete() 
	{
		if ("isv".equals(modelSpace))
			throw new RuntimeException("Can not delete isv processModel "+getVarName());
		XmlNode  request=new XmlNode(Constants.DELETE_PROCESS_MODEL, Constants.XMLNS_COBOC);
		request.add("processname").setText(name);
		org.call(request);
	}
}
