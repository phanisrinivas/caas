package org.kisst.cordys.caas;

/**
 * This class holds the different statusses that a package can have.
 */
public enum EPackageStatus
{
    loaded("DEPLOYED"), incomplete("INCOMPLETE"), not_loaded("NEW"), partial("PARTIAL");

    /** Holds the corresponding cluster status */
    private String m_clusterStatus;

    /**
     * Instantiates a new package status.
     * 
     * @param clusterStatus The CAP cluster status
     */
    EPackageStatus(String clusterStatus)
    {
        m_clusterStatus = clusterStatus;
    }

    /**
     * This method returns the CAP cluster status that is returned.
     * 
     * @return The CAP cluster status that is returned.
     */
    public String clusterStatus()
    {
        return m_clusterStatus;
    }

    /**
     * This method parses the cluster status into a package status.
     * 
     * @param clusterStatus The CAP cluster status
     * @return The package status.
     */
    public static EPackageStatus parse(String clusterStatus)
    {
        EPackageStatus retVal = not_loaded;

        for (EPackageStatus ps : EPackageStatus.values())
        {
            if (ps.clusterStatus().equalsIgnoreCase(clusterStatus))
            {
                retVal = ps;
                break;
            }
        }

        return retVal;
    }
}