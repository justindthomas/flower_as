package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
public class FlowReceiver {

    private static FrequencyManager frequencyManager;
    private static GlobalConfigurationManager globalConfigurationManager;
    private Environment environment;

    public FlowReceiver() {
        if (frequencyManager == null) {
            frequencyManager = FrequencyManager.getFrequencyManager();
        }
    }

    private void setupEnvironment() {
        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                e.printStackTrace();
            }
        }

        File environmentHome = new File(globalConfigurationManager.getBaseDirectory() + "/flows");

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

    public LinkedList<Long> addFlows(LinkedList<Flow> flows) {
        return this.addFlows(flows, null);
    }

    public LinkedList<Long> addFlows(LinkedList<Flow> flows, HttpServletRequest request) {
        LinkedList<Long> ids = new LinkedList();

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
                for (Flow flow : flows) {
                    if ((flow.protocol == 6) || (flow.protocol == 17)) {
                        frequencyManager.addPort(flow.protocol, flow.ports);
                    }

                    PersistentFlow pflow = flow.toHashTableFlow();
                    dataAccessor.flowById.put(pflow);

                    ids.add(pflow.id);
                }
            } catch (DatabaseException e) {
                System.err.println("addFlows Failed: " + e.getMessage());
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

        return ids;
    }
}
