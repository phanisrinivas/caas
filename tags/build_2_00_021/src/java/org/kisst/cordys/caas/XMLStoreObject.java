package org.kisst.cordys.caas;

import java.util.HashMap;
import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.support.CordysObjectList;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * Class to represent an object in XMLStore.
 */
public class XMLStoreObject extends CordysObject
{
    /** Holds the key. */
    private String key;
    /** Holds the version. */
    private String version;
    /** Holds the xml. */
    private XmlNode xml = null;
    /** Holds the org. */
    private Organization org;
    /** Holds the name. */
    private String name = null;
    /** Holds the last modified. */
    private String lastModified = null;
    /** Holds the system. */
    private final CordysSystem system;

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return key;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getName()
     */
    @Override
    public String getName()
    {
        return name;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getVarName()
     */
    @Override
    public String getVarName()
    {
        return name;
    }

    /**
     * This method gets the last modified.
     * 
     * @return The last modified
     */
    public String getLastModified()
    {
        return lastModified;
    }

    /**
     * This method gets the xml.
     * 
     * @return XML of the XMLStore object
     */
    public XmlNode getXML()
    {
        return xml;
    }

    /**
     * This method gets the version.
     * 
     * @return version of the XMLStore object
     */
    public String getVersion()
    {
        return version;
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getSystem()
     */
    @Override
    public CordysSystem getSystem()
    {
        return system;
    }

    /**
     * Constructs XMLStore object with given key and organization.
     * 
     * @param key The key
     * @param org The org
     */
    public XMLStoreObject(String key, Organization org)
    {
        this(key, "organization", org);
    }

    /**
     * Constructs XMLStore object with given key, version and organization.
     * 
     * @param key The key
     * @param version The version
     * @param org The org
     */
    public XMLStoreObject(String key, String version, Organization org)
    {
        this.key = key;
        this.version = version;
        this.org = org;
        this.system = org.getSystem();
        XmlNode response = getXMLObject(key, version);
        if (response.getChild("tuple/old") != null)
        {
            this.xml = response.getChild("tuple/old").getChildren().get(0);
            this.name = response.getChild("tuple").getAttribute("name");
            this.lastModified = response.getChild("tuple").getAttribute("lastModified");
        }
    }

    /**
     * This method gets the organization.
     * 
     * @return The organization
     * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
     */
    @Override
    public Organization getOrganization()
    {
        return org;
    }

    /**
     * Returns the XML of the XMLStore object with the given key and version.
     * 
     * @param key The key
     * @param version The version
     * @return XML of the object
     */
    public XmlNode getXMLObject(String key, String version)
    {
        XmlNode request = new XmlNode(Constants.GET_XML_OBJECT, Constants.XMLNS_XMLSTORE);
        request.add("key").setAttribute("version", version).setText(key);
        XmlNode response = call(request);
        return response;
    }

    /**
     * Appends the given XML to the XMLStore object.
     * 
     * @param node XML to be appended
     */
    public void appendXML(XmlNode node)
    {
        XmlNode request = new XmlNode(Constants.APPEND_XML_OBJECT, Constants.XMLNS_XMLSTORE);
        XmlNode tuple = request.add("tuple");
        tuple.setAttribute("key", key);
        tuple.setAttribute("version", version);
        if (node != null)
            tuple.add("new").add(node);
        call(request);
        XmlNode response = getXMLObject(key, version);
        this.xml = response.getChild("tuple/old").getChildren().get(0);
    }

    /**
     * Overwrites the existing XML of the object with the given XML.
     * 
     * @param newXml XML that would overwrite the existing one
     */
    public void overwriteXML(XmlNode newXml)
    {
        XmlNode request = new XmlNode(Constants.UPDATE_XML_OBJECT, Constants.XMLNS_XMLSTORE);
        XmlNode tuple = request.add("tuple");
        tuple.setAttribute("key", key);
        tuple.setAttribute("version", version);
        // Set 'unconditional' flag to true to overwrite the existing XML
        tuple.setAttribute("unconditional", "true");
        if (newXml != null)
            tuple.add("new").add(newXml);
        call(request);
        // Refresh the XML content of the XMLStoreObject after the update operation
        this.xml = newXml;
    }

    /**
     * Call.
     * 
     * @param request The request
     * @return The xml node
     */
    public XmlNode call(XmlNode request)
    {
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("organization", org.getDn());
        return getSystem().call(request, queryParams);
    }

    /**
     * Holds the Class List.
     * 
     * @author galoori
     */
    public static class List extends CordysObjectList<XMLStoreObject>
    {

        /** Holds the org. */
        private final Organization org;

        /**
         * Instantiates a new list.
         * 
         * @param org The org
         */
        public List(Organization org)
        {
            super(org.getSystem());
            this.org = org;
        }

        /**
         * Retrieve list.
         * 
         * @see org.kisst.cordys.caas.support.CordysObjectList#retrieveList()
         */
        @Override
        protected void retrieveList()
        {
            XmlNode method = new XmlNode(Constants.GET_COLLECTION, Constants.XMLNS_XMLSTORE);
            XmlNode folderNode = method.add("folder");
            folderNode.setAttribute("recursive", "true");
            folderNode.setAttribute("detail", "false");
            folderNode.setAttribute("version", "organization");
            HashMap<String, String> queryParams = new HashMap<String, String>();
            queryParams.put("timeout", "60000");
            queryParams.put("organization", org.getDn());
            XmlNode response = system.call(method, queryParams);
            for (XmlNode tuple : response.getChildren("tuple"))
            {
                // Ignore folders
                if (Boolean.valueOf(tuple.getAttribute("isFolder")).booleanValue())
                    continue;
                // Ignore WsApps runtime entries
                if (tuple.getAttribute("name").endsWith(".cmx"))
                    continue;
                // Ignore CAF files
                if (tuple.getAttribute("name").endsWith(".caf"))
                    continue;
                XMLStoreObject obj = new XMLStoreObject(tuple.getAttribute("key"), tuple.getAttribute("level"), org);
                grow(obj);
            }
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getKey()
         */
        @Override
        public String getKey()
        {
            return "XMLStoreObjects:" + org.getKey();
        }

        /**
         * @see org.kisst.cordys.caas.support.CordysObject#getOrganization()
         */
        @Override
        public Organization getOrganization()
        {
            return org;
        }
    };

    /**
     * Delete.
     */
    public void delete()
    {
        if ("isv".equals(version))
            throw new RuntimeException("Can not delete isv xmlstoreobject " + getVarName());
        XmlNode request = new XmlNode(Constants.UPDATE_XML_OBJECT, Constants.XMLNS_XMLSTORE);
        XmlNode tuple = request.add("tuple");
        tuple.setAttribute("key", key);
        tuple.setAttribute("level", version);
        tuple.setAttribute("lastModified", lastModified);
        tuple.setAttribute("recursive", "true");
        call(request);
    }

}
