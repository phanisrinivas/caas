package org.kisst.cordys.caas;

import org.kisst.cordys.caas.util.StringUtil;

/**
 * This class holds the information about a deployed package.
 */
public class DeployedPackageInfo implements IDeployedPackageInfo
{
    /** Holds the full version of the package */
    private String m_fullVersion;
    /** Holds the name of the package. */
    private String m_packageName;
    /** Holds the vendor of this package */
    private String m_vendor;
    /** Holds the version part */
    private String m_version;
    /** Holds the build number */
    private String m_buildNumber;

    /**
     * Instantiates a new deployed package info.
     * 
     * @param packageName The package name
     * @param fullVersion The full version
     * @param vendor The vendor
     * @param version The version
     * @param buildNumber The build number
     */
    public DeployedPackageInfo(String packageName, String fullVersion, String vendor, String version, String buildNumber)
    {
        m_fullVersion = fullVersion;
        m_packageName = packageName;
        m_vendor = vendor;
        m_version = version;
        m_buildNumber = buildNumber;

        if (StringUtil.isEmptyOrNull(m_fullVersion))
        {
            m_fullVersion = m_version + "." + m_buildNumber;
        }
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#getFullVersion()
     */
    @Override
    public String getFullVersion()
    {
        return m_fullVersion;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#setFullVersion(java.lang.String)
     */
    @Override
    public void setFullVersion(String fullVersion)
    {
        m_fullVersion = fullVersion;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#getPackageName()
     */
    @Override
    public String getPackageName()
    {
        return m_packageName;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#setPackageName(java.lang.String)
     */
    @Override
    public void setPackageName(String packageName)
    {
        m_packageName = packageName;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#getVendor()
     */
    @Override
    public String getVendor()
    {
        return m_vendor;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#setVendor(java.lang.String)
     */
    @Override
    public void setVendor(String vendor)
    {
        m_vendor = vendor;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#getVersion()
     */
    @Override
    public String getVersion()
    {
        return m_version;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#setVersion(java.lang.String)
     */
    @Override
    public void setVersion(String version)
    {
        m_version = version;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#getBuildNumber()
     */
    @Override
    public String getBuildNumber()
    {
        return m_buildNumber;
    }

    /**
     * @see org.kisst.cordys.caas.IDeployedPackageInfo#setBuildNumber(java.lang.String)
     */
    @Override
    public void setBuildNumber(String buildNumber)
    {
        m_buildNumber = buildNumber;
    }
    
    /**
     * This method gets the identifier for the package.
     * 
     * @return The identifier for the package.
     */
    public String getIdentifier()
    {
        return "ver" + m_version + "build" + m_buildNumber;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return getPackageName() + "(" + getVendor() + "; " + getFullVersion() + ")";
    }
}