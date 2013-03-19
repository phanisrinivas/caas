/**
 * Copyright 2008, 2009 Mark Hooijkaas This file is part of the Caas tool. The Caas tool is free software: you can redistribute it
 * and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version
 * 3 of the License, or (at your option) any later version. The Caas tool is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of the GNU General Public License along with the Caas
 * tool. If not, see <http://www.gnu.org/licenses/>.
 */

package org.kisst.cordys.caas;

import java.util.HashMap;
import java.util.Random;
import org.kisst.cordys.caas.support.ChildList;
import org.kisst.cordys.caas.support.LdapObject;
import org.kisst.cordys.caas.support.LdapObjectBase;
import org.kisst.cordys.caas.util.Constants;
import org.kisst.cordys.caas.util.StringUtil;
import org.kisst.cordys.caas.util.XmlNode;

public class ServiceContainer extends LdapObjectBase
{

    public final ChildList<ConnectionPoint> connectionPoints = new ChildList<ConnectionPoint>(this, ConnectionPoint.class);
    public final ChildList<ConnectionPoint> cp = connectionPoints;
    public final StringProperty computer = new StringProperty("computer");
    public final StringProperty host = new StringProperty("busoshost");
    public final BooleanProperty automatic = new BooleanProperty("automaticstart");
    public final XmlProperty config = new XmlProperty("bussoapprocessorconfiguration");
    public final XmlSubProperty ui_algorithm = new XmlSubProperty(config, "routing/@ui_algorithm");
    public final XmlSubProperty ui_type = new XmlSubProperty(config, "routing/@ui_type");
    public final XmlSubProperty preference = new XmlSubProperty(config, "routing/preference");
    public final XmlSubProperty gracefulCompleteTime = new XmlSubProperty(config, "gracefulCompleteTime");
    public final XmlSubProperty abortTime = new XmlSubProperty(config, "abortTime");
    public final XmlSubProperty requestTimeout = new XmlSubProperty(config, "cancelReplyInterval");
    public final XmlSubProperty implementation = new XmlSubProperty(config, "configuration/@implementation");
    public final XmlBoolProperty useSystemLogPolicy = new XmlBoolProperty(config, "loggerconfiguration/systempolicy", true);

    private XmlNode workerprocess;
    private static Random random = new Random();
    private static XmlNode inactiveWorkerProcess = new XmlNode("<dummy><status></status></dummy>");

    /**
     * Constructs the service container as a child of the given parent and with the given dn
     * 
     * @param parent
     * @param dn Service container DN
     */
    protected ServiceContainer(LdapObject parent, String dn)
    {
        super(parent, dn);
    }

    @Override
    protected String prefix()
    {
        return "sc";
    }

    @Override
    public void myclear()
    {
        super.myclear();
        workerprocess = null;
    }

    /**
     * Sends the SOAP request to this service container
     * 
     * @param request SOAP request
     * @return SOAP response
     */
    public String call(String request)
    {
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", getDn());
        return getSystem().call(request, queryParams);
    }

    public void setWorkerprocess(XmlNode workerprocess)
    {
        this.workerprocess = workerprocess;
    }

    public XmlNode getWorkerprocess()
    {
        if (workerprocess != null && useCache())
            return this.workerprocess;
        XmlNode request = new XmlNode(Constants.LIST, Constants.XMLNS_MONITOR);
        ServiceContainer monitor = getSystem().sc.getByName("monitor@" + computer);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        XmlNode response = call(request, queryParams);
        for (XmlNode tuple : response.getChildren("tuple"))
        {
            XmlNode workerprocess = tuple.getChild("old/workerprocess");
            String dn = workerprocess.getChildText("name");
            if (dn.equals(getDn()))
            {
                this.workerprocess = workerprocess;
                return workerprocess;
            }
        }
        this.workerprocess = inactiveWorkerProcess;
        return workerprocess;
        // throw new RuntimeException("Could not find processor details for "+this.dn);
    }

    private int getIntChild(XmlNode node, String name)
    {
        String result = node.getChildText(name);
        if (result == null || result.length() == 0)
            return -1;
        else
            return Integer.parseInt(result);
    }

    public String getStatus()
    {
        return getWorkerprocess().getChildText("status");
    }

    public String getCpuTime()
    {
        return getWorkerprocess().getChildText("totalCpuTime");
    }

    public int getPid()
    {
        return getIntChild(getWorkerprocess(), "process-id");
    }

    public int getNomMemory()
    {
        return getIntChild(getWorkerprocess(), "totalNOMMemory");
    }

    public int getNomNodesMemory()
    {
        return getIntChild(getWorkerprocess(), "totalNOMNodesMemory");
    }

    public int getVirtualMemory()
    {
        return getIntChild(getWorkerprocess(), "virtualMemoryUsage");
    }

    public int getResidentMemory()
    {
        return getIntChild(getWorkerprocess(), "residentMemoryUsage");
    }

    public int getSequenceNumber()
    {
        return getIntChild(getWorkerprocess(), "sequence-number");
    }

    public int getPreference()
    {
        return getIntChild(getWorkerprocess(), "preference");
    }

    public int getBusdocs()
    {
        return getIntChild(getWorkerprocess(), "busdocs");
    }

    public int getProcessingTime()
    {
        return getIntChild(getWorkerprocess(), "processing-time");
    }

    public int getLastTime()
    {
        return getIntChild(getWorkerprocess(), "last-time");
    }

    /**
     * Starts this service container
     */
    public void start()
    {
        XmlNode request = new XmlNode(Constants.START, Constants.XMLNS_MONITOR);
        request.add("dn").setText(getDn());
        ServiceContainer monitor = getSystem().sc.getByName("monitor@" + computer);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        call(request, queryParams);
    }

    /**
     * Stops this service container
     */
    public void stop()
    {
        XmlNode request = new XmlNode(Constants.STOP, Constants.XMLNS_MONITOR);
        request.add("dn").setText(getDn());
        ServiceContainer monitor = getSystem().sc.getByName("monitor@" + computer);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        call(request, queryParams);
    }

    /**
     * Restarts this service container
     */
    public void restart()
    {
        XmlNode request = new XmlNode(Constants.RESTART, Constants.XMLNS_MONITOR);
        request.add("dn").setText(getDn());
        ServiceContainer monitor = getSystem().sc.getByName("monitor@" + computer);
        HashMap<String, String> queryParams = new HashMap<String, String>();
        queryParams.put("receiver", monitor.getDn());
        call(request, queryParams);
    }

    /**
     * Creates a connection point for this service container with the given name
     * 
     * @param name Connection point name
     */
    public void createConnectionPoint(String name)
    {
        createConnectionPoint(name, "socket", getMachine().getName());
    }

    /**
     * Creates a connection point for this service container with the given name and on the given machine
     * 
     * @param name Connection point name
     * @param machineName Machine name where the connection point needs to be created
     */
    public void createConnectionPoint(String name, String machineName)
    {
        createConnectionPoint(name, "socket", machineName);
    }

    /**
     * Creates a connection point for this service container with the given name, type on the given machine
     * 
     * @param name Connection point name
     * @param type Connection point type
     * @param machineName Machine name where the connection point needs to be created
     */
    public void createConnectionPoint(String name, String type, String machineName)
    {
        createConnectionPoint(name, type, machineName, null, null);
    }

    /**
     * Creates a connection point for this service container with the given name, type on the given machine.
     * 
     * @param name Connection point name
     * @param type Connection point type
     * @param machineName Machine name where the connection point needs to be created
     * @param description The description
     * @param labeledURI The labeled uri
     */
    public void createConnectionPoint(String name, String type, String machineName, String description, String labeledURI)
    {
        if (StringUtil.isEmptyOrNull(type))
        {
            type = "socket";
        }

        if ("socket".equals(type))
        {
            if (StringUtil.isEmptyOrNull(description))
            {
                description = name;
            }

            if (StringUtil.isEmptyOrNull(labeledURI))
            {
                labeledURI = type + "://" + machineName + ":" + getAvailablePort();
            }
        }

        XmlNode newEntry = newEntryXml("", name, "busconnectionpoint");
        newEntry.add("description").add("string").setText(description);
        newEntry.add("labeleduri").add("string").setText(labeledURI);

        createInLdap(newEntry);
        connectionPoints.clear();
    }

    /**
     * Updates this service container with the new configuration as provided
     * 
     * @param newEntry XmlNode representing the new configuration of the service container
     */
    public void updateEntry(XmlNode newEntry)
    {
        updateLdap(newEntry);
        myclear();
        getSystem().removeLdap(getDn());
    }

    private Machine getMachine()
    {
        // TODO This hack only works on single machine installs
        return getSystem().machines.get(0);
    }

    private int getAvailablePort()
    {
        // TODO: check all know connection points to avoid some clashes
        // Note that the official Cordys wizard does not seem to check this either
        int port = random.nextInt(64 * 1024 - 10000) + 10000;
        return port;
    }
}
