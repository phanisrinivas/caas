package org.kisst.cordys.caas.comp;

import java.util.LinkedList;
import java.util.List;

import org.kisst.cordys.caas.CordysSystem;
import org.kisst.cordys.caas.IDeployedPackageInfo;
import org.kisst.cordys.caas.Package;
import org.kisst.cordys.caas.PackageList;
import org.kisst.cordys.caas.soap.SoapCaller;

/**
 * This class contains the a stripped down implementation of certain operations for C3
 * 
 * @author Mark Hooijkaas
 */
public class C3DefaultCompatibilityManager implements ICompatibilityManager
{
    @Override public String getVersionDetails()  { return "C3"; }
    @Override public List<Package> getCAPPackages(SoapCaller c, CordysSystem system, PackageList packageList) { return new LinkedList<Package>(); }
    @Override public Boolean supportsCap(SoapCaller c, CordysSystem system) { return false; }

    @Override public void deployCap(SoapCaller c, CordysSystem system, String name, double timeoutInMinutes) {
        throw new RuntimeException("CAP files are not supported on a C3 system");
    }
    @Override public void revertCap(SoapCaller c, CordysSystem system, String name, long timeoutInMinutes) {
        throw new RuntimeException("CAP files are not supported on a C3 system");
    }

    @Override public void undeployCap(SoapCaller c, CordysSystem system, String name, String userInputs, Boolean deleteReferences, long timeoutInMinutes) {
        throw new RuntimeException("CAP files are not supported on a C3 system");
    }
    
    @Override public IDeployedPackageInfo loadCAPInfo(SoapCaller soapCaller, CordysSystem system, Package p) {
        throw new RuntimeException("CAP files are not supported on a C3 system");
    }
}
