package org.kisst.cordys.caas.comp;

import java.util.List;

import org.kisst.cordys.caas.CordysSystem;
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
}
