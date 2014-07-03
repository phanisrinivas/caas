/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import java.util.LinkedHashMap;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.EntryObjectList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class contains the configuration of a service group.
 */
public class ServiceGroup extends LdapObjectBase
{
    public final ChildList<ServiceContainer> serviceContainers = new ChildList<ServiceContainer>(this, ServiceContainer.class);
    public final ChildList<ServiceContainer> sc = serviceContainers;
    public final EntryObjectList<WebServiceInterface> webServiceInterfaces = new EntryObjectList<WebServiceInterface>(this,
            "busmethodsets", "wsi");
    public final EntryObjectList<WebServiceInterface> wsi = webServiceInterfaces;
    public final StringList namespaces = new StringList("labeleduri");
    public final StringList ns = namespaces;
    public final XmlProperty config = new XmlProperty("bussoapnodeconfiguration");
    public final XmlSubProperty ui_algorithm = new XmlSubProperty(config, "routing/@ui_algorithm");
    public final XmlSubProperty ui_type = new XmlSubProperty(config, "routing/@ui_type");
    public final XmlSubProperty numprocessors = new XmlSubProperty(config, "routing/numprocessors");
    public final XmlSubProperty algorithm = new XmlSubProperty(config, "routing/algorithm");
    public final XmlBoolProperty protocolValidation = new XmlBoolProperty(config, "validation/protocol", false);
    public final XmlBoolProperty payloadValidation = new XmlBoolProperty(config, "validation/payload", false);
    public final XmlBoolProperty requestValidation = new XmlBoolProperty(config, "validation/request", false);
    public final XmlBoolProperty payloadTrim = new XmlBoolProperty(config, "IgnoreWhiteSpaces", false);

    /**
     * Constructs the service group as child of the given parent and with the given dn
     * 
     * @param parent
     * @param dn
     */
    protected ServiceGroup(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    @Override
    protected String prefix()
    {
        return "sg";
    }

    /**
     * Re-attaches the web service interfaces to the service group
     */
    public void recalcNamespaces()
    {
        LinkedHashMap<String, String> all = new LinkedHashMap<String, String>();
        for (WebServiceInterface wsi : webServiceInterfaces)
        {
            if (wsi != null)
            {
                for (String namespace : wsi.namespaces.get())
                    all.put(namespace, namespace);
            }
        }
        XmlNode newEntry = getEntry().clone();
        XmlNode msNode = newEntry.getChild("labeleduri");
        if (msNode == null)
            msNode = newEntry.add("labeleduri");
        for (XmlNode child : msNode.getChildren())
            msNode.remove(child);
        for (String namespace : all.keySet())
            msNode.add("string").setText(namespace);
        updateLdap(newEntry);
    }

    /*
     * &lt;configurations&gt; &lt;cancelReplyInterval&gt;30000&lt;/cancelReplyInterval&gt;
     * &lt;gracefulCompleteTime&gt;15&lt;/gracefulCompleteTime&gt; &lt;abortTime&gt;5&lt;/abortTime&gt; &lt;jreconfig&gt;&lt;param
     * value="-Xmx64M"/&gt;&lt;/jreconfig&gt; &lt;routing ui_type="loadbalancing" ui_algorithm="failover"&gt;
     * &lt;preference&gt;1&lt;/preference&gt; &lt;/routing&gt;
     * &lt;loggerconfiguration&gt;&lt;systempolicy&gt;true&lt;/systempolicy&gt;&lt;log4j:configuration
     * xmlns:log4j="http://jakarta.apache.org/log4j"&gt;&lt;renderer
     * renderedClass="com.eibus.util.logger.internal.LocalizableLogMessage"
     * renderingClass="com.eibus.util.logger.internal.TextRenderer"/&gt;&lt;renderer
     * renderedClass="com.eibus.util.logger.internal.LogMessage"
     * renderingClass="com.eibus.util.logger.internal.TextRenderer"/&gt;&lt;root&gt;&lt;priority
     * value="error"/&gt;&lt;appender-ref ref="DailyRollingFileAppender"/&gt;&lt;/root&gt;&lt;category
     * name="com.eibus.security.acl"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.license"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.directory"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.soap"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.transport.SOAPMessage"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.transport"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.tools"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="com.eibus.util"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="org.kisst.cordys.relay.RelayTrace"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="org.kisst.cordys.relay.RelayTimer"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;category
     * name="httpclient.wire"&gt;&lt;priority value="error"/&gt;&lt;/category&gt;&lt;appender name="DailyRollingFileAppender"
     * class="org.apache.log4j.DailyRollingFileAppender"&gt;&lt;param name="File"
     * value="esb#relay_service2#relayconnector_processor.xml"/&gt;&lt;param name="DatePattern" value=".yyyy-MM-dd"/&gt;&lt;layout
     * class="org.apache.log4j.xml.XMLLayout"&gt;&lt;param name="locationInfo"
     * value="true"/&gt;&lt;/layout&gt;&lt;/appender&gt;&lt;/log4j:configuration&gt;&lt;/loggerconfiguration&gt;
     * &lt;spyPublish&gt;
     * false&lt;/spyPublish&gt;&lt;spyFile&gt;&lt;/spyFile&gt;&lt;spyLogger&gt;&lt;/spyLogger&gt;&lt;spyLevels&gt
     * ;&lt;/spyLevels&gt;&lt;spyCategories&gt;&lt;/spyCategories&gt; &lt;configuration
     * implementation="org.kisst.cordys.relay.RelayConnector" htmfile="/cordys/kisst.org/RelayConnector-1.0/config.html"&gt;
     * &lt;classpath xmlns="http://schemas.cordys.com/1.0/xmlstore"&gt;
     * &lt;location&gt;/kisst.org/RelayConnector-1.0/commons-logging-1.0.4.jar&lt;/location&gt;
     * &lt;location&gt;/kisst.org/RelayConnector-1.0/backport-util-concurrent.jar&lt;/location&gt;
     * &lt;location&gt;/kisst.org/RelayConnector-1.0/ehcache-1.5.0.jar&lt;/location&gt;
     * &lt;location&gt;/kisst.org/RelayConnector-1.0/groovy-all-1.6.2.jar&lt;/location&gt;
     * &lt;location&gt;/kisst.org/RelayConnector-1.0/RelayConnector-1.0.jar&lt;/location&gt; &lt;/classpath&gt;
     * &lt;ConfigLocation&gt;D:/config/RelayConnector.properties&lt;/ConfigLocation&gt; &lt;/configuration&gt;
     * &lt;/configurations&gt;
     */

    /**
     * Creates a service container for this service group
     * 
     * @param name Service container name
     * @param machineName Machine name where the service container needs to be created
     * @param automatic Start up type of the service container
     * @param config Configuration xml of the service container
     */
    public void createServiceContainer(String name, String machineName, boolean automatic, XmlNode config)
    {
        XmlNode newEntry = createServiceContainerEntryNode(name, machineName, automatic, config);
        createInLdap(newEntry);
        serviceContainers.clear();
    }

    public void createServiceContainer(String name, Connector connector)
    {
        XmlNode config = new XmlNode("configurations");
        config.add("cancelReplyInterval").setText("30000");
        config.add("gracefulCompleteTime").setText("15");
        config.add("abortTime").setText("5");
        config.add("jreconfig").add("param").setAttribute("value", "-Xmx64M");
        config.add("loggerconfiguration");
        XmlNode config2 = config.add("configuration");
        config2.setAttribute("implementation", connector.getData().getChildText(("implementation")));
        config2.setAttribute("htmfile", connector.getData().getChildText(("url")));
        config2.add(connector.getData().getChild("classpath").clone());
        createServiceContainer(name, getSystem().machines.get(0).getName(), false, config);
    }

    /**
     * Updates the service container of this service group
     * 
     * @param name Service container name
     * @param machineName Machine name
     * @param automatic Startup type
     * @param config New XML configuration of the service container
     * @param oldServiceContainer
     */
    public void updateServiceContainer(String name, String machineName, boolean automatic, XmlNode config,
            ServiceContainer oldServiceContainer)
    {
        XmlNode newEntry = createServiceContainerEntryNode(name, machineName, automatic, config);
        XmlNode oldEntry = oldServiceContainer.getEntry();
        updateLdap(oldEntry, newEntry);
    }

    /**
     * Creates an XmlNode representing the service container entry in the LDAP
     * 
     * @param name
     * @param machineName
     * @param automatic
     * @param config
     * @return
     */
    private XmlNode createServiceContainerEntryNode(String name, String machineName, boolean automatic, XmlNode config)
    {
        XmlNode newEntry = newEntryXml("", name, "bussoapprocessor");
        newEntry.add("description").add("string").setText(name);
        newEntry.add("computer").add("string").setText(machineName);
        newEntry.add("busosprocesshost");
        newEntry.add("automaticstart").add("string").setText("" + automatic);
        newEntry.add("bussoapprocessorconfiguration").add("string").setText(config.compact());
        return newEntry;
    }

    /**
     * This method updates the current configuration XML. The tricky part here is that the the bus soap node keystore must be
     * retained. Otherwise the container won't start anymore.
     * 
     * @param newConfig The new configuration XML.
     */
    public void updateConfiguration(XmlNode newConfig)
    {
        XmlNode nc = newConfig.clone();

        // Remove the soap node from the close
        XmlNode keystore = nc.getChild("soapnode_keystore");
        if (keystore != null)
        {
            nc.remove(keystore);
        }

        // Now get the keystore from the current config
        XmlNode current = config.getXml();
        keystore = current.getChild("soapnode_keystore");
        if (keystore != null)
        {
            nc.add(keystore.clone().detach());
        }

        config.set(nc.compact());
    }
}
