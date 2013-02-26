package org.kisst.cordys.caas;

import org.kisst.cordys.caas.support.CordysObject;
import org.kisst.cordys.caas.util.XmlNode;

/**
 * This class holds the definition of a package. The packages available on a system do not need to be loaded. This class contains
 * both loaded and unloaded packages. It will also contain ISV packages, but also CAP packages.
 * 
 * @author pgussow
 */
public class PackageDefinition extends CordysObject
{
    /** Holds the package types. */
    public enum EPackageType
    {
        cap,
        isvp
    };

    /** Holds the package types. */
    public enum EPackageStatus
    {
        loaded,
        incomplete,
        not_loaded
    };

    /** Holds the reference to the Cordys system that is being used. */
    private final CordysSystem system;
    /** Holds the name of the package. This is the Package DN that would be used in case the package is loaded. */
    private String name;
    /** Holds the type of package. It defaults to CAP */
    private EPackageType type = EPackageType.cap;
    /** Holds the definition XML for this package */
    public XmlNode definition;
    /** Holds the source filename for this package */
    public String filename;
    /** Holds the owner of the package */
    public String owner;
    /** Holds the build number of the package */
    public String buildnumber;
    /** Holds the version information */
    public String version;
    /** Holds the CN (package DN) of the package */
    public String cn;
    /** Holds the status of the package (whether it is loaded or not) */
    public EPackageStatus status = EPackageStatus.not_loaded;
    /** Holds the runtime information in case the package is loaded. */
    public Package runtime;

    /**
     * Instantiates a new package definition.
     * 
     * @param system The parent Cordys system
     * @param definition The definition of this package. This can be either an ISV package definition or an CAP definition.
     */
    public PackageDefinition(CordysSystem system, XmlNode definition)
    {
        this.system = system;

        // Based on the XML definition we need to determine whether it's an ISV package or that it is a CAP package. The XML of
        // the ISV package looks like this:

        // @formatter:off
        // <ISVPackage xmlns="http://schemas.cordys.com/1.0/isvpackage" file="Cordys_CommandConnector_1.1.3" cn="Cordys CommandConnector 1.1">
        //  <description>
        //          <owner>Cordys</owner>
        //          <name>CommandConnector</name>
        //          <version>1.1 Build 1.1.3</version>
        //          <cn>Cordys CommandConnector 1.1</cn>
        //          <wcpversion>C3.001</wcpversion>
        //          <build>3</build>
        //          <eula source="" />
        //          <sidebar source="" />
        //  </description>
        //  <content />
        //  <promptset />
        // </ISVPackage>
        // @formatter:on

        if ("ISVPackage".equals(definition.getName()))
        {
            type = EPackageType.isvp;
            name = definition.getAttribute("cn");
            filename = definition.getAttribute("file");

            owner = (String) definition.get("description/owner/text()");
            buildnumber = (String) definition.get("description/build/text()");
            version = (String) definition.get("description/version/text()");
            cn = (String) definition.get("description/cn/text()");

            // Retrieve the status of the package: is it loaded yes or no? On which nodes has it been loaded?
        }
        else if ("ApplicationPackage".equals(definition.getName()))
        {
            // @formatter:off
            // <ApplicationPackage>
            //   <ApplicationName>Cordys XMLStore</ApplicationName>
            //   <ApplicationVersion>D1.002</ApplicationVersion>
            //   <ApplicationBuild>26</ApplicationBuild>
            //   <owner>Cordys</owner>
            //   <node>CNL1523</node>
            // </ApplicationPackage>
            // @formatter:on
            
            // It is a CAP file
            type = EPackageType.cap;
            name = definition.getChildText("ApplicationName");
            owner = definition.getChildText("owner");
            
            buildnumber = definition.getChildText("ApplicationBuild");
            version =  definition.getChildText("ApplicationVersion");
            
            if (owner == null || owner.isEmpty())
            {
                status = EPackageStatus.not_loaded;
            }
            else
            {
                status = EPackageStatus.loaded;
            }
        }

        this.definition = definition.detach();
    }

    /**
     * This method gets the type of this package (either CAP or ISVP).
     * 
     * @return The type of this package (either CAP or ISVP).
     */
    public EPackageType getType()
    {
        return type;
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
        return system.getVarName() + ".packages." + getName();
    }

    /**
     * @see org.kisst.cordys.caas.support.CordysObject#getKey()
     */
    @Override
    public String getKey()
    {
        return "packagedefinition:" + getSystem().getDn() + ":entry:" + name;
    }

}
