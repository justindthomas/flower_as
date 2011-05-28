/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 *
 * @author justin
 */
public class ManagedNetworkManager {

    private static GlobalConfigurationManager configurationManager;
    private Environment environment;

    public List<ManagedNetwork> getManagedNetworks() {
        ArrayList<ManagedNetwork> networks = new ArrayList();

        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "ManagedNetwork", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            EntityCursor<ManagedNetwork> cursor = accessor.managedNetworkByAddress.entities();
            
            for(ManagedNetwork network : cursor) {
                networks.add(network);
            }

            cursor.close();
            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } catch (Throwable t) {
            t.printStackTrace();
        } finally {
            closeEnvironment();
        }

        return networks;
    }

    public Boolean addManagedNetwork(ManagedNetwork network) {
        Boolean error = false;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "ManagedNetwork", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if(!accessor.managedNetworkByAddress.contains(network.address)) {
                accessor.managedNetworkByAddress.put(network);
            } else {
                System.err.println("Attempted to add managed network that already exists.");
                error = true;
            }

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return !error;
    }

    public Boolean deleteManagedNetwork(String address) {
        Boolean error = false;

        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "ManagedNetwork", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if(accessor.managedNetworkByAddress.contains(address)) {
                accessor.managedNetworkByAddress.delete(address);
            } else {
                System.err.println("Attempted to delete managed network that does not exist.");
                error = true;
            }

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return !error;
    }
    
    private void setupEnvironment() {
        if (configurationManager == null) {
            try {
                configurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        File environmentHome = new File(configurationManager.getBaseDirectory() + "/configuration");

        if (!environmentHome.exists()) {
            if (environmentHome.mkdirs()) {
                System.out.println("Created directory: " + environmentHome);
            } else {
                System.err.println("Configuration directory '" + environmentHome + "' does not exist and could not be created (permissions?)");
            }
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        //environmentConfig.setConfigParam(EnvironmentConfig.CLEANER_EXPUNGE, "false");
        environment = new Environment(environmentHome, environmentConfig);
    }

    private void closeEnvironment() {
        if (environment != null) {
            try {
                environment.close();
            } catch (DatabaseException e) {
                System.err.println("Error closing environment: " + e.toString());
            }
        }
    }
}
