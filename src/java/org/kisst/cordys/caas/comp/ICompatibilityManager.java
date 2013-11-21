package org.kisst.cordys.caas.comp;

import java.util.List;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.soap.SoapCaller;

/**
 * This interface describes the things for which new APIs have been made.
 * 
 * @author pgussow
 */
public interface ICompatibilityManager
{
    /**
     * This method returns the specific version information for with the class provides compatibility implementations.
     * 
     * @return The version details
     */
    String getVersionDetails();

    /**
     * This method returns the list of packages that are both deployed and which can be deployed. From 4.3 the SOAP API has
     * changed.
     * 
     * @param c The connection to Cordys to use.
     * @param system The system to connect to.
     * @param packageList The package list object. This is needed to be able to set the 'supportsCap' property.
     * @return The list of packages that are either deployed or can be deployed on the system.
     */
    List<Package> getCAPPackages(SoapCaller c, CordysSystem system, PackageList packageList);

    /**
     * This method returns whether or not the system supports CAP packages.
     * 
     * @param c The connection to Cordys to use.
     * @param system The system to connect to.
     * @return true if the system supports CAP packages. Otherwise false.
     */
    Boolean supportsCap(SoapCaller c, CordysSystem system);

    /**
     * This method will deploy the latest version of the given CAP package name. It will first check to see if there is a package
     * to deploy and whether it's an upgrade or a fresh load.
     * 
     * @param c The connection to Cordys to use.
     * @param system The system to connect to.
     * @param name The name
     * @param timeoutInMinutes The timeout in minutes.
     */
    void deployCap(SoapCaller c, CordysSystem system, String name, double timeoutInMinutes);

    /**
     * This method will revert the given incomplete cap package.
     * 
     * @param c The connection to Cordys to use.
     * @param system The system to connect to.
     * @param name The package DN of the package.
     * @param timeoutInMinutes The timeout in minutes
     */
    void revertCap(SoapCaller c, CordysSystem system, String name, long timeoutInMinutes);

    /**
     * This method will undeploy the given cap package.
     * 
     * @param c The connection to Cordys to use.
     * @param system The system to connect to.
     * @param name The package DN of the package.
     * @param userInputs The user inputs XML.
     * @param deleteReferences Whether or not to delete the references of the package
     * @param timeoutInMinutes The timeout in minutes
     */
    void undeployCap(SoapCaller c, CordysSystem system, String name, String userInputs, Boolean deleteReferences,
            long timeoutInMinutes);

    /**
     * This method loads the CAP details.
     * 
     * @param soapCaller The soap caller
     * @param system The system
     * @param package1 The package1
     * @return The i deployed package info
     */
    IDeployedPackageInfo loadCAPInfo(SoapCaller soapCaller, CordysSystem system, Package package1);
}
