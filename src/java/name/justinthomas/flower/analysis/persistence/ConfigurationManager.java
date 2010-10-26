/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author JustinThomas
 */
@Singleton
@Startup
public class ConfigurationManager {

    private String host = null;
    private String user = null;
    private String password = null;
    private String baseDirectory = null;
    private String flowDirectory = null;
    private String userDirectory = null;
    private String statisticsDirectory = null;
    private String frequencyDirectory = null;
    private HashMap<String, Long> resolution = null;
    private String configurationDirectory = "configuration";
    private Properties properties;
    private HashMap<String, String> managedNetworks = null;
    private HashMap<String, HashMap<String, Boolean>> directoryDomains = null;
    private Environment environment = null;
    private EntityStore entityStore = null;
    private EnvironmentConfig environmentConfig = null;
    private ConfigurationAccessor dataAccessor = null;
    private PersistentConfiguration configuration = null;
    private File environmentHome = null;
    private static ConfigurationManager configurationManager;

    synchronized private void setup() throws Exception {
        environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);

        environment = new Environment(environmentHome, environmentConfig);
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        entityStore = new EntityStore(environment, "Configuration", storeConfig);
        dataAccessor = new ConfigurationAccessor(entityStore);
    }

    synchronized private void shutdown() {
        entityStore.close();
        if (environment != null) {
            environment.cleanLog();
            environment.close();
        }
    }

    private void reload() {
        this.flowDirectory = configuration.flowDirectory;
        this.userDirectory = configuration.userDirectory;
        this.frequencyDirectory = configuration.frequencyDirectory;
        //System.out.println("Frequency Directory as Loaded: " + frequencyDirectory);
        this.managedNetworks = configuration.managedNetworks;
        this.statisticsDirectory = configuration.statisticsDirectory;
        this.directoryDomains = configuration.directoryDomains;
        this.resolution = configuration.resolution;
    }

    @PostConstruct
    public void init() {

        InputStream inputStream = ConfigurationManager.class.getResourceAsStream("resource.properties");
        this.properties = new Properties();
        try {
            this.properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.baseDirectory = properties.getProperty("base");

        Boolean created = false;
        environmentHome = new File(baseDirectory + "/" + configurationDirectory);
        if (!environmentHome.exists()) {
            if (!environmentHome.mkdirs()) {
                try {
                    throw new Exception("Couldn't find or create configuration directory: " + configurationDirectory);
                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }
            } else {
                created = true;
            }
        }

        if (created) {
            System.out.println("Creating configuration.");
            configuration = new PersistentConfiguration();
            updateConfiguration(configuration, true);
        } else {
            System.out.println("Loading configuration from: " + environmentHome);

            try {
                setup();
            } catch (Exception e) {
                e.printStackTrace();
            }

            EntityCursor<PersistentConfiguration> entityCursor = dataAccessor.defaultConfiguration.entities(true, true, true, true);

            configuration = entityCursor.first();
            //System.out.println("Stored Frequency Directory: " + configuration.frequencyDirectory);

            entityCursor.close();

            shutdown();
        }

        reload();
    }

    synchronized public void updateConfiguration(PersistentConfiguration hashTableConfiguration, Boolean create) {
        try {
            setup();
        } catch (Exception e) {
            System.err.println("Exception in ConfigurationManager(): " + e.getMessage());
        }

        dataAccessor = new ConfigurationAccessor(entityStore);
        if (!create) {
            EntityCursor<PersistentConfiguration> entityCursor = dataAccessor.configurationById.entities();

            PersistentConfiguration htc = null;
            for (PersistentConfiguration h : entityCursor) {
                if (h.selected) {
                    htc = h;
                }
            }

            htc.selected = false;
            entityCursor.update(htc);
            entityCursor.close();
        }

        hashTableConfiguration.selected = true;
        hashTableConfiguration.id = null;
        dataAccessor.configurationById.put(hashTableConfiguration);
        shutdown();

        configuration = hashTableConfiguration;
        reload();
    }

    public static ConfigurationManager getConfigurationManager() {
        if (configurationManager != null) {
            return configurationManager;
        }

        System.out.println("Locating ConfigurationManager by JNDI");
        try {
            Context context = new InitialContext();
            configurationManager = (ConfigurationManager) context.lookup("java:global/Analysis/ConfigurationManager");
        } catch (NamingException ne) {
            configurationManager = new ConfigurationManager();
            System.err.println("Could not locate ConfigurationManager by JNDI and started a new instance");
        }

        return configurationManager;
    }

    @Lock(LockType.READ)
    public String getBaseDirectory() {
        return baseDirectory;
    }

    @Lock(LockType.WRITE)
    public void setBaseDirectory(String baseDirectory) {
        this.baseDirectory = baseDirectory;
    }

    @Lock(LockType.READ)
    public String getConfigurationDirectory() {
        return configurationDirectory;
    }

    @Lock(LockType.WRITE)
    public void setConfigurationDirectory(String configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
    }

    @Lock(LockType.READ)
    public String getFlowDirectory() {
        return flowDirectory;
    }

    @Lock(LockType.WRITE)
    public void setFlowDirectory(String flowDirectory) {
        this.flowDirectory = flowDirectory;
    }

    @Lock(LockType.READ)
    public String getFrequencyDirectory() {
        return frequencyDirectory;
    }

    @Lock(LockType.WRITE)
    public void setFrequencyDirectory(String frequencyDirectory) {
        this.frequencyDirectory = frequencyDirectory;
    }

    @Lock(LockType.READ)
    public String getHost() {
        return host;
    }

    @Lock(LockType.WRITE)
    public void setHost(String host) {
        this.host = host;
    }

    @Lock(LockType.WRITE)
    public HashMap<String, String> getManagedNetworks() {
        return managedNetworks;
    }

    @Lock(LockType.WRITE)
    public void setManagedNetworks(HashMap<String, String> managedNetworks) {
        this.managedNetworks = managedNetworks;
    }

    @Lock(LockType.READ)
    public String getPassword() {
        return password;
    }

    @Lock(LockType.WRITE)
    public void setPassword(String password) {
        this.password = password;
    }

    @Lock(LockType.READ)
    public String getUser() {
        return user;
    }

    @Lock(LockType.WRITE)
    public void setUser(String user) {
        this.user = user;
    }

    @Lock(LockType.READ)
    public String getUserDirectory() {
        return userDirectory;
    }

    @Lock(LockType.WRITE)
    public void setUserDirectory(String userDirectory) {
        this.userDirectory = userDirectory;
    }

    @Lock(LockType.WRITE)
    public PersistentConfiguration getConfiguration() {
        return configuration;
    }

    @Lock(LockType.WRITE)
    public void setConfiguration(PersistentConfiguration configuration) {
        this.configuration = configuration;
    }

    @Lock(LockType.READ)
    public String getStatisticsDirectory() {
        return statisticsDirectory;
    }

    @Lock(LockType.WRITE)
    public void setStatisticsDirectory(String statisticsDirectory) {
        this.statisticsDirectory = statisticsDirectory;
    }

    public HashMap<String, HashMap<String, Boolean>> getDirectoryDomains() {
        return directoryDomains;
    }

    public void setDirectoryDomains(HashMap<String, HashMap<String, Boolean>> directoryDomains) {
        this.directoryDomains = directoryDomains;
    }

    public HashMap<String, Long> getResolution() {
        return resolution;
    }

    public void setResolution(HashMap<String, Long> resolution) {
        this.resolution = resolution;
    }
}
