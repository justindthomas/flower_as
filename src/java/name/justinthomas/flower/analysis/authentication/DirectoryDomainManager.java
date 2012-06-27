/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.authentication;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import java.io.IOException;
import java.util.List;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import name.justinthomas.flower.analysis.persistence.ManagedNetworkManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author justin
 */
public class DirectoryDomainManager {

    private static final Logger log = Logger.getLogger(DirectoryDomainManager.class.getName());
    private static FileAppender fileAppender;
    private Customer customer;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;
    
    private static GlobalConfigurationManager getGlobalConfigurationManager() {
        try {
            return (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }
    
    public static EntityManager getEntityManager() {
        try {
            return (EntityManager) InitialContext.doLookup("java:comp/env/persistence/Analysis");
        } catch (NamingException e) {
            log.error(e.getMessage());
        }

        return null;
    }

    public DirectoryDomainManager(Customer customer) { 
        this.customer = customer;
        this.globalConfigurationManager = DirectoryDomainManager.getGlobalConfigurationManager();
        this.em = ManagedNetworkManager.getEntityManager();
        
        if (fileAppender == null) {
            try {
                String pattern = "%d{dd MMM yyyy HH:mm:ss.SSS} - %p - %m %n";
                PatternLayout layout = new PatternLayout(pattern);
                fileAppender = new FileAppender(layout, globalConfigurationManager.getBaseDirectory() + "/statistics.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
    
    public Boolean removeGroup(String domain, String group) {
        /*
        Boolean error = false;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if (accessor.directoryDomainByName.contains(domain)) {
                if (accessor.directoryDomainByName.get(domain).getGroups().containsKey(group)) {
                    DirectoryDomain directoryDomain = accessor.directoryDomainByName.get(domain);
                    directoryDomain.getGroups().remove(group);
                    if(directoryDomain.getGroups().isEmpty()) {
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
        * 
        */
        return null;
    }
    
    public Boolean addDirectoryDomain(String domain, String group, Boolean privileged) {
        /*
        Boolean error = false;
        setupEnvironment();
        try {
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            EntityStore entityStore = new EntityStore(environment, "DirectoryDomain", storeConfig);
            CustomerConfigurationAccessor accessor = new CustomerConfigurationAccessor(entityStore);

            if (accessor.directoryDomainByName.contains(domain)) {
                if (!accessor.directoryDomainByName.get(domain).getGroups().containsKey(group)) {
                    DirectoryDomain directoryDomain = accessor.directoryDomainByName.get(domain);
                    directoryDomain.getGroups().put(group, privileged);
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
        * 
        */
        return null;
    }

    public DirectoryDomain getDirectoryDomain(String domain) {
        /*
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
        * 
        */
        return null;
    }

    public List<DirectoryDomain> getDirectoryDomains() {
        /*
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
        * 
        */
        return null;
    }
}
