package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpServletRequest;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
public class FlowReceiver {

    private static FrequencyManager frequencyManager;
    private static ConfigurationManager configurationManager;
    private Environment environment;

    public FlowReceiver() {
        if (configurationManager == null) {
            configurationManager = ConfigurationManager.getConfigurationManager();
        }
        if (frequencyManager == null) {
            frequencyManager = FrequencyManager.getFrequencyManager();
        }
    }

    private void setupEnvironment() {
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/" + configurationManager.getFlowDirectory());

        try {
            if (!environmentHome.exists()) {
                if (!environmentHome.mkdirs()) {
                    throw new Exception("Could not open or create base flow directory.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();

        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        environmentConfig.setLockTimeout(15, TimeUnit.SECONDS);
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

    public Long addFlow(Flow flow) {
        return this.addFlow(flow, null);
    }

    public Long addFlow(Flow flow, HttpServletRequest request) {
        if ((flow.protocol == 6) || (flow.protocol == 17)) {
            frequencyManager.addPort(flow.protocol, flow.ports);
        }

        PersistentFlow pflow = flow.toHashTableFlow();

        try {
            setupEnvironment();

            EntityStore entityStore = null;
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);
            //storeConfig.setDeferredWrite(true);

            entityStore = new EntityStore(environment, "Flow", storeConfig);
            try {
                FlowAccessor dataAccessor = new FlowAccessor(entityStore);
                //System.out.println("Putting flow with start date: " + new Date(pflow.getStartTimeStampMs()));
                dataAccessor.flowById.put(pflow);
            } catch (DatabaseException e) {
                System.err.println("addVolume Failed: " + e.getMessage());
                if (request != null) {
                    System.err.println("Requester: " + request.getRemoteAddr());
                }
            } finally {
                entityStore.close();
            }
        } catch (DatabaseException e) {
            System.err.println("Database Error: " + e.getMessage());
            if (request != null) {
                System.err.println("Requester: " + request.getRemoteAddr());
            }
        } finally {
            closeEnvironment();
        }

        return pflow.id;
    }
}
