package name.justinthomas.flower.global;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import name.justinthomas.flower.analysis.persistence.FrequencyManager;
import name.justinthomas.flower.analysis.statistics.CachedStatistics;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author JustinThomas
 */
@Singleton
public class GlobalConfigurationManager {

    private String baseDirectory;
    private Map<Long, Boolean> resolutionMap;
    
    // <customer_id, managed object>
    private Map<String, CachedStatistics> cachedStatisticsMap;
    private Map<String, FrequencyManager> frequencyMap;
    
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
        
        cachedStatisticsMap = new ConcurrentHashMap();
        //cachedStatisticsMap = Collections.synchronizedMap(new HashMap());
        
        frequencyMap = new ConcurrentHashMap();
        //frequencyMap = Collections.synchronizedMap(new HashMap());

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
    
    public CachedStatistics getCachedStatistics(String customerID) {
        return this.cachedStatisticsMap.get(customerID);
    }
    
    public void setCachedStatistics(String customerID, CachedStatistics cachedStatistics) {
        this.cachedStatisticsMap.put(customerID, cachedStatistics);
    }
    
    public FrequencyManager getFrequencyManager(Customer customer) {
        if(!this.frequencyMap.containsKey(customer.getId())) {
            this.frequencyMap.put(customer.getId(), new FrequencyManager(customer));
        }
        
        return this.frequencyMap.get(customer.getId());
    }
    
    public void setFrequencyManager(String customerID, FrequencyManager frequencyManager) {
        this.frequencyMap.put(customerID, frequencyManager);
    }
    
    public Integer getFrequency(Customer customer, Integer protocol, Integer port) {
        return this.frequencyMap.get(customer.getId()).getFrequency(protocol, port);
    }
    
    public void addFrequency(Customer customer, Integer protocol, Integer[] ports) {
        if(!this.frequencyMap.containsKey(customer.getId())) {
            this.frequencyMap.put(customer.getId(), new FrequencyManager(customer));
        }
        
        frequencyMap.get(customer.getId()).addPort(protocol, ports);
    }
}
