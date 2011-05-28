/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.authentication;

import name.justinthomas.flower.analysis.authentication.DirectoryDomain;
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
import name.justinthomas.flower.analysis.persistence.CustomerConfigurationAccessor;

/**
 *
 * @author justin
 */
public class DirectoryDomainManager {

    private Environment environment;
    private static GlobalConfigurationManager configurationManager;

    public Boolean removeGroup(String domain, String group) {
        Boolean error = false;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if (accessor.directoryDomainByName.contains(domain)) {
                if (accessor.directoryDomainByName.get(domain).groups.containsKey(group)) {
                    DirectoryDomain directoryDomain = accessor.directoryDomainByName.get(domain);
                    directoryDomain.groups.remove(group);
                    if(directoryDomain.groups.isEmpty()) {
                        accessor.directoryDomainByName.delete(domain);
                    } else {
                        accessor.directoryDomainByName.put(directoryDomain);
                    }
                } else {
                    System.err.println("DirectoryDomain group entry does not exist.");
                    error = true;
                }
            } else {
                System.err.println("DirectoryDomain entry does not exist.");
                error = true;
            }

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in DirectoryDomainManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return !error;
    }
    public Boolean addDirectoryDomain(String domain, String group, Boolean privileged) {
        Boolean error = false;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if (accessor.directoryDomainByName.contains(domain)) {
                if (!accessor.directoryDomainByName.get(domain).groups.containsKey(group)) {
                    DirectoryDomain directoryDomain = accessor.directoryDomainByName.get(domain);
                    directoryDomain.groups.put(group, privileged);
                    accessor.directoryDomainByName.put(directoryDomain);
                } else {
                    System.err.println("DirectoryDomain entry already exists.");
                    error = true;
                }
            } else {
                DirectoryDomain directoryDomain = new DirectoryDomain(domain, group, privileged);
                accessor.directoryDomainByName.put(directoryDomain);
            }

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return !error;
    }

    public DirectoryDomain getDirectoryDomain(String domain) {
        DirectoryDomain directoryDomain = null;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            directoryDomain = accessor.directoryDomainByName.get(domain);

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return directoryDomain;
    }

    public List<DirectoryDomain> getDirectoryDomains() {
        List<DirectoryDomain> directoryDomains = new ArrayList();
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            EntityCursor<DirectoryDomain> cursor = accessor.directoryDomainByName.entities();
            for (DirectoryDomain directoryDomain : cursor) {
                directoryDomains.add(directoryDomain);
            }
            cursor.close();

            entityStore.close();
        } catch (DatabaseException e) {
            System.err.println("DatabaseException in ManagedNetworkManager: " + e.getMessage());
        } finally {
            closeEnvironment();
        }

        return directoryDomains;
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
                System.out.println("Created configuration directory: " + environmentHome);
            } else {
                System.err.println("Configuration directory '" + environmentHome + "' does not exist and could not be created (permissions?)");
            }
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();
        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
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
