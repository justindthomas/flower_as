/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author justin
 */
@Singleton
@Startup
public class FrequencyManager {

    private static Integer DEBUG = 0;
    private File environmentHome = null;
    @EJB private ConfigurationManager configurationManager;
    private final Map<String, Integer> map = Collections.synchronizedMap(new HashMap<String, Integer>());

    @PostConstruct
    public void init() {
        setConfiguration();
        loadMap();

        if (DEBUG >= 1) {
            System.out.println("Setting UpdateDatabase to run in 60 minutes and every hour.");
        }

        ScheduledThreadPoolExecutor stpe = new ScheduledThreadPoolExecutor(3);
        UpdateDatabase ud = new UpdateDatabase(environmentHome);
        stpe.scheduleAtFixedRate(ud, 15, 15, TimeUnit.MINUTES);
    }

    private void setConfiguration() {
        if (configurationManager == null) {
            System.err.println("ConfigurationManager is null");
        }

        if (environmentHome == null) {
            environmentHome = new File(configurationManager.getBaseDirectory() + "/"
                    + configurationManager.getFrequencyDirectory());

            //System.out.println("Frequency Directory: " + environmentHome);
            try {
                if (!environmentHome.exists()) {
                    if (!environmentHome.mkdirs()) {
                        throw new Exception("Could not open or create base directory.");
                    }
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void loadMap() {
        if (DEBUG >= 1) {
            System.out.println("Populating frequency map from storage.");
        }
        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        Environment environment = new Environment(environmentHome, environmentConfig);
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        storeConfig.setReadOnly(false);
        EntityStore entityStore = new EntityStore(environment, "Frequency", storeConfig);

        FrequencyAccessor dataAccessor = new FrequencyAccessor(entityStore);
        int i = 0;
        for (Entry<String, PersistentFrequency> entry : dataAccessor.frequencyByPort.map().entrySet()) {
            i++;
            map.put(entry.getKey(), entry.getValue().frequency);
        }

        //System.out.println("Frequency records retrieved: " + i);

        if (entityStore != null) {
            entityStore.close();
        }
        if (environment != null && environment.isValid()) {
            environment.cleanLog();
            environment.close();
        }
        if (DEBUG >= 1) {
            System.out.println("Frequency map populated.");
        }
    }

    @Lock(LockType.WRITE)
    public void incrementFrequency(String protocol_port) {
        if (DEBUG >= 3) {
            System.out.println("Incrementing frequency for: " + protocol_port);
        }

        if (map.get(protocol_port) != null) {
            map.put(protocol_port, map.get(protocol_port) + 1);
        } else {
            map.put(protocol_port, 1);
        }
    }

    @Lock(LockType.READ)
    public Integer getFrequency(String protocol_port) {
        if (map.get(protocol_port) == null) {
            return 0;
        }

        return map.get(protocol_port);
    }

    @Lock(LockType.WRITE)
    public void addPort(Integer protocol, Integer port) {
        String key = protocol.toString() + "_" + port.toString();
        incrementFrequency(key);
    }

    @Lock(LockType.WRITE)
    public void addPort(Integer protocol, Integer[] port) {
        for (int i = 0; i < port.length; i++) {
            addPort(protocol, port[i]);
        }
    }

    @Lock(LockType.READ)
    public Integer getFrequency(Integer protocol, Integer port) {
        String key = protocol.toString() + "_" + port.toString();
        return getFrequency(key);
    }

    public static FrequencyManager getFrequencyManager() {
        FrequencyManager frequencyManager = null;
        //System.out.println("Locating FrequencyManager by JNDI");
        try {
            Context context = new InitialContext();
            frequencyManager = (FrequencyManager) context.lookup("java:global/Analysis/FrequencyManager");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
        return frequencyManager;
    }

    public class UpdateDatabase implements Runnable {

        File environmentHome;

        public UpdateDatabase(File environmentHome) {
            this.environmentHome = environmentHome;
        }

        private void saveMap() {
            if (DEBUG >= 1) {
                System.out.println("Saving frequency map...");
            }
            EnvironmentConfig environmentConfig = new EnvironmentConfig();
            environmentConfig.setAllowCreate(true);
            environmentConfig.setReadOnly(false);

            Environment environment = new Environment(environmentHome, environmentConfig);
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "Frequency", storeConfig);

            FrequencyAccessor dataAccessor = new FrequencyAccessor(entityStore);

            if (DEBUG >= 1) {
                System.out.println("Setting up Frequency EntrySet");
            }

            Set<Entry<String, Integer>> entries = map.entrySet();

            if (DEBUG >= 1) {
                System.out.println("Setting up Frequency Iterator");
            }

            int recordCount = 0;
            //synchronized (map) {

            if (DEBUG >= 1) {
                System.out.println("Entering save loop.");
            }
            for (Entry<String, Integer> entry : entries) {
                if (DEBUG >= 2) {
                    System.out.println("Record: " + recordCount++);
                } else if (DEBUG >= 1 && (++recordCount % 10000 == 0)) {
                    System.out.println("Record: " + recordCount);
                }
                PersistentFrequency htf = new PersistentFrequency(entry.getKey(), entry.getValue());
                dataAccessor.frequencyByPort.put(htf);
            }
            //}

            if (DEBUG >= 1) {
                System.out.println("Records saved: " + recordCount);
            }

            if (entityStore != null) {
                entityStore.close();
            }

            if (environment.isValid()) {
                environment.close();
            }

            if (DEBUG >= 1) {
                System.out.println("Frequency map update completed.");
            }
        }

        @Override
        public void run() {
            if(FrequencyManager.getFrequencyManager().getLargestFrequency() > (0.75 * Integer.MAX_VALUE)) {
                pruneMap();
            }

            saveMap();
        }
    }

    public void pruneMap() {
        int i = 0;
        for(Entry<String, Integer> frequency : map.entrySet()) {
            //if(++i % 1000 == 0) System.out.println("Reducing: " + frequency.getValue() + " to: " + new Double(0.1 * frequency.getValue()).intValue());
            map.put(frequency.getKey(), new Double(0.1 * frequency.getValue()).intValue());
        }
    }

    public Integer getMapSize() {
        return map.size();
    }

    public Integer getLargestFrequency() {
        Integer i = 0;
        for (Integer frequency : map.values()) {
            if (frequency > i) {
                i = frequency;
            }
        }
        return i;
    }
}
