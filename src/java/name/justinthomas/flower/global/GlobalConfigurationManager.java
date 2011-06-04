package name.justinthomas.flower.global;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import name.justinthomas.flower.analysis.statistics.CachedStatistics;

/**
 *
 * @author JustinThomas
 */
@Singleton
public class GlobalConfigurationManager {

    private String baseDirectory;
    private Map<Long, Boolean> resolutionMap;
    // <customer_id, CachedStatistics>
    private Map<String, CachedStatistics> cachedStatisticsMap;
    private Properties properties;
    private Boolean unsafeLdap;
    private String manager;

    @PostConstruct
    public void init() {
        InputStream inputStream = GlobalConfigurationManager.class.getResourceAsStream("resource.properties");
        this.properties = new Properties();
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {
            System.err.println("Could not load properties.");
            return;
        }

        this.baseDirectory = properties.getProperty("base");

        resolutionMap = new HashMap();
        String[] resolutions = properties.getProperty("resolutions").split(",");
        for(String resolution : resolutions) {
            resolutionMap.put(Long.valueOf(resolution.trim()), true);
        }
        
        cachedStatisticsMap = Collections.synchronizedMap(new HashMap());

        unsafeLdap = Boolean.parseBoolean(properties.getProperty("unsafeLdap").trim());
        manager = properties.getProperty("manager");
    }

    public String getBaseDirectory() {
        return baseDirectory;
    }

    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    public Boolean getUnsafeLdap() {
        return unsafeLdap;
    }

    public void setUnsafeLdap(Boolean unsafeLdap) {
        this.unsafeLdap = unsafeLdap;
    }

    public Map<Long, Boolean> getResolutionMap() {
        return resolutionMap;
    }

    public void setResolutionMap(Map<Long, Boolean> resolutionMap) {
        this.resolutionMap = resolutionMap;
    }

    public String getManager() {
        return manager;
    }

    public void setManager(String manager) {
        this.manager = manager;
    }

    public Map<String, CachedStatistics> getCachedStatisticsMap() {
        return cachedStatisticsMap;
    }

    public void setCachedStatisticsMap(Map<String, CachedStatistics> cachedStatisticsMap) {
        this.cachedStatisticsMap = cachedStatisticsMap;
    }
    
    public CachedStatistics getCachedStatistics(String customerID) {
        return this.cachedStatisticsMap.get(customerID);
    }
    
    public void setCachedStatistics(String customerID, CachedStatistics cachedStatistics) {
        this.cachedStatisticsMap.put(customerID, cachedStatistics);
    }
}
