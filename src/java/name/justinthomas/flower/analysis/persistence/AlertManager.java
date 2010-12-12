package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.ForwardCursor;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.ArrayList;

public class AlertManager {

    private static final Integer DEBUG = 2;
    private ConfigurationManager configurationManager;

    public AlertManager() {
        this.configurationManager = ConfigurationManager.getConfigurationManager();
    }

    public void addAlert(PersistentAlert alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        Long second = alert.date;
        PersistentAlertSecond psecond;
        if (accessor.alertsBySecond.contains(second)) {
            psecond = accessor.alertsBySecond.get(second);
        } else {
            psecond = new PersistentAlertSecond();
            psecond.second = second;
        }

        accessor.alertById.put(alert);

        System.out.println("Adding: " + alert.id + " to: " + psecond.second);
        psecond.alerts.add(alert.id);
        accessor.alertsBySecond.put(psecond);

        closeStore(entityStore);
        closeEnvironment(environment);
    }

    public Boolean deleteAlert(Long alert) {
        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        accessor.alertById.delete(alert);

        closeStore(entityStore);
        closeEnvironment(environment);
        return true;
    }

    public ArrayList<PersistentAlert> getAlerts(Constraints constraints) {
        ArrayList<PersistentAlert> alerts = new ArrayList();

        System.out.println("Getting alerts from: " + constraints.startTime + " to: " + constraints.endTime);
        System.out.println("Alert type requested: " + constraints.alertType);
        Long start = constraints.startTime.getTime() / 1000;
        Long end = constraints.endTime.getTime() / 1000;

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Alert", this.getStoreConfig(false));
        AlertAccessor accessor = new AlertAccessor(entityStore);

        ForwardCursor<PersistentAlertSecond> cursor = accessor.alertsBySecond.entities(start, true, end, true);

        for(PersistentAlertSecond psecond : cursor) {
            System.out.println("Alerts at " + psecond.getSecond() + ":" + psecond.alerts.size());
            for(Long alertID : psecond.alerts) {
                PersistentAlert alert = accessor.alertById.get(alertID);
                System.out.println("Alert type: " + alert.type);
                if((constraints.alertType == null) || (constraints.alertType == alert.type)) {
                    alerts.add(accessor.alertById.get(alertID));
                }
            }
        }

        cursor.close();
        closeStore(entityStore);
        closeEnvironment(environment);

        return alerts;
    }

    private Environment setupEnvironment() {
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/" + configurationManager.getAlertDirectory());

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
