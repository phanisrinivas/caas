package org.kisst.cordys.caas.isvdev;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.kisst.cordys.caas.Organization;
import org.kisst.cordys.caas.WebService;
import org.kisst.cordys.caas.WebServiceInterface;
import org.kisst.cordys.caas.Xsd;
import org.kisst.cordys.caas.util.FileUtil;
import org.kisst.cordys.caas.util.XmlNode;

public class WsiDef
{
    public class WsDef {
        public final String name;
        public final XmlNode implementation;
        public final XmlNode busmethodsignature;
        public WsDef(XmlNode node) {
            this.name=node.getAttribute("name");
            this.implementation = node.getChild("methodimplementation/implementation");
            this.busmethodsignature=node.getChild("busmethodsignature");
        }
        public WsDef(WebService ws) {
            this.name=ws.getName();
            this.implementation=ws.implementation.getXml();
            this.busmethodsignature=ws.signature.getXml();
        }
        private void update(WebServiceInterface wsi)
        {
            WebService ws= wsi.webService.get(name);
            if (ws==null) {
                wsi.createWebService(name);
                ws=wsi.webService.get(name);
            }
            ws.implementation.set(implementation);
        }
        public XmlNode toXml() {
            XmlNode result = new XmlNode("method");
            result.setAttribute("name", name);
            //XmlNode child = new XmlNode("methodimplementation");
            //child.add(this.implementation.clone());
            //result.add(child);
            result.add(new XmlNode("methodimplementation").add(implementation.clone()));
            result.add(new XmlNode("methodreturntype"));
            result.add(new XmlNode("methodwsdl"));
            result.add(new XmlNode("methodinterface"));
            result.add(new XmlNode("busmethodsignature"));
            return result;
        }
    }
    
    public class XsdDef {
        public final String name;
        public final XmlNode methodxsd;
        public XsdDef(XmlNode node) {
            this.name=node.getAttribute("name");
            this.methodxsd = node.getChild("methodxsd");
        }
        public XsdDef(Xsd xsd) {
            this.name=xsd.getName();
            this.methodxsd=xsd.content.getXml();
        }
        public void update(WebServiceInterface wsi)
        {
            Xsd xsd= wsi.xsds.get(name);
            if (xsd==null) {
                wsi.createXsd(name, methodxsd.toString());
                xsd=wsi.xsds.get(name);
            }
        }
        public XmlNode toXml() {
            XmlNode result = new XmlNode("xsd");
            result.setAttribute("name", name);
            result.add(new XmlNode("methodxsd").add(methodxsd.clone()));
            return result;
        }
    }
    
    private final String name;
    private final String implementationclass;
    private final ArrayList<String> namespaces=new ArrayList<String>();
    private final String description;
    private final LinkedHashMap<String, WsDef> methods = new LinkedHashMap<String,WsDef>();
    private final LinkedHashMap<String, XsdDef> xsds= new LinkedHashMap<String,XsdDef>();
    
    public WsiDef(String filename) {
        XmlNode xml = new XmlNode(FileUtil.loadString(filename));
        this.name=xml.getAttribute("name");
        this.implementationclass = xml.getAttribute("implementationclass");
        this.description= xml.getAttribute("description");
        for (XmlNode ns: xml.getChild("namespaces").getChildren("namespace"))
            this.namespaces.add(ns.getText());
        for (XmlNode child : xml.getChildren("method")) {
            WsDef m=new WsDef(child);
            methods.put(m.name, m);
        }
        for (XmlNode child : xml.getChildren("xsd")) {
            XsdDef xsd=new XsdDef(child);
            xsds.put(xsd.name, xsd);
        }
    }
    
    public WsiDef(Organization o, String wsiName) { 
        this(o.webServiceInterfaces.getByName(wsiName));
        if (! wsiName.equals(this.name))
            throw new RuntimeException("Found WSI with name "+name+", but should be "+wsiName);
    }

    public WsiDef(WebServiceInterface wsi) {
        this.name=wsi.getName();
        this.implementationclass=wsi.implementationclass.get();
        for (String ns : wsi.namespaces.get())
            this.namespaces.add(ns);
        this.description=wsi.description.get();
        for (WebService ws: wsi.webServices)
            methods.put(ws.getName(), new WsDef(ws));
        for (Xsd xsd: wsi.xsds)
            xsds.put(xsd.getName(), new XsdDef(xsd));
    }

    public void update(Organization o, boolean clearExisting ) {
        WebServiceInterface wsi = findWsi(o, clearExisting);
        for (XsdDef xsd: xsds.values())
            xsd.update(wsi);
        for (WsDef md: methods.values())
            md.update(wsi);
    }

    /*
    public void update(Organization o, String webServiceName) {
        WebServiceInterface wsi = findWsi(o, true);
        WsDef md = methods.get(webServiceName);
        md.update(wsi);
    }
*/
    
    private WebServiceInterface findWsi(Organization o, boolean clearExisting )
    {
        WebServiceInterface wsi = o.webServiceInterfaces.get(name);
        if (clearExisting && wsi!=null) {
            wsi.delete();
            wsi.getParent().clear();
            wsi=null;
        }
        if (wsi==null) {
            o.createWebServiceInterface(name, namespaces.get(0), implementationclass);
            wsi = o.webServiceInterfaces.get(name);
            // add the other namespaces as well
            for (int i=1; i<namespaces.size(); i++)
                wsi.namespaces.add(namespaces.get(i));
        }
        return wsi;
    }
    
    public XmlNode toXml() {
        XmlNode result = new XmlNode("methodset");
        result.setAttribute("name", name);
        result.setAttribute("description", nullValue(description,""));
        result.setAttribute("implementationclass", implementationclass);
        for (XsdDef xsd: xsds.values())
            result.add(xsd.toXml());
        for (WsDef wsd: methods.values())
            result.add(wsd.toXml());
        XmlNode nsnode = new XmlNode("namespaces");
        for (String ns: namespaces)
            nsnode.add(new XmlNode("namespace").setText(ns));
        result.add(nsnode);
        return result;
    }
    
    private String nullValue(String var, String defaultValue) { return var==null? defaultValue : var; } 
    
}
