package org.kisst.cordys.caas;

/**
 * This interface holds the details for a loaded package. It abstracts the CAP and ISV packages.
 * 
 * @author pgussow
 */
public interface IDeployedPackageInfo
{
    /**
     * This method gets the full version of the package. This is a concatenation of the version and the build number.
     * 
     * @return The full version of the package. This is a concatenation of the version and the build number.
     */
    String getFullVersion();

    /**
     * This method sets the full version of the package. This is a concatenation of the version and the build number.
     * 
     * @param fullVersion The full version of the package. This is a concatenation of the version and the build number.
     */
    void setFullVersion(String fullVersion);

    /**
     * This method gets the package name.
     * 
     * @return The package name.
     */
    String getPackageName();

    /**
     * This method sets the package name.
     * 
     * @param packageName The package name.
     */
    void setPackageName(String packageName);

    /**
     * This method gets the name of the vendor.
     * 
     * @return The name of the vendor.
     */
    String getVendor();

    /**
     * This method sets the name of the vendor.
     * 
     * @param vendor The name of the vendor.
     */
    void setVendor(String vendor);

    /**
     * This method gets the version of the package.
     * 
     * @return The version of the package.
     */
    String getVersion();

    /**
     * This method sets the version of the package.
     * 
     * @param version The version of the package.
     */
    void setVersion(String version);

    /**
     * This method gets the build number.
     * 
     * @return The build number.
     */
    String getBuildNumber();

    /**
     * This method sets the build number.
     * 
     * @param buildNumber The build number.
     */
    void setBuildNumber(String buildNumber);
}
