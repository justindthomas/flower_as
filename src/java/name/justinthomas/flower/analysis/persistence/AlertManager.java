package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.ForwardCursor;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.ArrayList;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.manager.services.Customer;

public class AlertManager {

    private Customer customer;
    private static final Integer DEBUG = 2;
    private static GlobalConfigurationManager configurationManager;
    
    public AlertManager(Customer customer) {
        this.customer = customer;
    }

    public void addAlert(SnortAlert alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        System.out.println("Adding alert: " + alert.toString());
        accessor.snortAlertById.put(alert);

        closeStore(entityStore);
        closeEnvironment(environment);
    }

    public void addAlert(ModSecurityAlert alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        System.out.println("Adding alert: " + alert.toString());
        accessor.modSecurityAlertById.put(alert);

        closeStore(entityStore);
        closeEnvironment(environment);
    }

    public Boolean deleteSnortAlert(Long alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        accessor.snortAlertById.delete(alert);

        closeStore(entityStore);
        closeEnvironment(environment);
        return true;
    }

    public Boolean deleteModSecurityAlert(Long alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        accessor.modSecurityAlertById.delete(alert);

        closeStore(entityStore);
        closeEnvironment(environment);
        return true;
    }

    public ArrayList<SnortAlert> getSnortAlerts(Constraints constraints) {
        ArrayList<SnortAlert> alerts = new ArrayList();

        System.out.println("Getting alerts from: " + constraints.startTime + " to: " + constraints.endTime);

        Long start = constraints.startTime.getTime() / 1000;
        Long end = constraints.endTime.getTime() / 1000;

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        ForwardCursor<SnortAlert> cursor = accessor.snortAlertsByDate.entities(start, true, end, true);

        for (SnortAlert alert : cursor) {
            alerts.add(alert);
        }

        cursor.close();
        closeStore(entityStore);
        closeEnvironment(environment);

        return alerts;
    }

    public ArrayList<ModSecurityAlert> getModSecurityAlerts(Constraints constraints) {
        ArrayList<ModSecurityAlert> alerts = new ArrayList();

        System.out.println("Getting alerts from: " + constraints.startTime + " to: " + constraints.endTime);

        Long start = constraints.startTime.getTime() / 1000;
        Long end = constraints.endTime.getTime() / 1000;

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        ForwardCursor<ModSecurityAlert> cursor = accessor.modSecurityAlertsByDate.entities(start, true, end, true);

        for (ModSecurityAlert alert : cursor) {
            alerts.add(alert);
        }

        cursor.close();
        closeStore(entityStore);
        closeEnvironment(environment);

        return alerts;
    }

    private Environment setupEnvironment() {
        if (configurationManager == null) {
            try {
                configurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }
        
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/customers/" + customer.getDirectory() + "/alerts");

        try {
            if (!environmentHome.exists()) {
                if (!environmentHome.mkdirs()) {
                    throw new Exception("Could not open or create base alerts directory.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();

        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);

        Environment environment = new Environment(environmentHome, environmentConfig);
        return environment;
    }

    private void closeEnvironment(Environment environment) {
        if (environment != null) {
            try {
                environment.close();
            } catch (DatabaseException e) {
                System.err.println("Error closing environment: " + e.toString());
            } catch (IllegalStateException e) {
                System.err.println("Error closing environment: " + e.toString());
            }
        }
    }

    private StoreConfig getStoreConfig(Boolean readOnly) {
        StoreConfig storeConfig = new StoreConfig();
        storeConfig.setAllowCreate(true);
        if (readOnly) {
            storeConfig.setReadOnly(true);
        } else {
            storeConfig.setReadOnly(false);
        }

        return storeConfig;
    }

    private void closeStore(EntityStore store) {
        try {
            store.close();
        } catch (DatabaseException e) {
            System.err.println("Error closing EntityStore: " + e.getMessage());
        }
    }

    private void cleanLog(Environment environment) {
        try {
            environment.cleanLog();
        } catch (DatabaseException e) {
            System.err.println("Error running cleanLog: " + e.getMessage());
        }
    }

    private void checkpoint(Environment environment) {
        CheckpointConfig checkpointConfig = new CheckpointConfig();
        try {
            environment.checkpoint(checkpointConfig);
        } catch (DatabaseException e) {
            System.err.println("Error running checkpoint: " + e.getMessage());
        }
    }
}
