package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import name.justinthomas.flower.analysis.accounting.AccountingManager;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author justin
 */
public class FlowReceiver {

    private Customer customer;
    //private static FrequencyManager frequencyManager;
    private static GlobalConfigurationManager globalConfigurationManager;
    private static AccountingManager accountingManager;
    private Environment environment;

    public FlowReceiver(Customer customer) {
        this.customer = customer;

        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                System.err.println("Error retrieving GlobalConfigurationManager in FlowReceiver: " + e.getMessage());
            }
        }

        if (accountingManager == null) {
            try {
                accountingManager = (AccountingManager) InitialContext.doLookup("java:global/Analysis/AccountingManager");
            } catch (NamingException e) {
                System.err.println("Error retrieving AccountingManager in FlowReceiver: " + e.getMessage());
            }
        }
    }

    private void setupEnvironment() throws Exception {
        File environmentHome = new File(globalConfigurationManager.getBaseDirectory() + "/customers/" + customer.getDirectory() + "/flows");

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
            globalConfigurationManager.addFrequency(customer, flow.protocol, flow.ports);
            //frequencyManager.addPort(flow.protocol, flow.ports);
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
        } catch (Exception e) {
            System.err.println(e.getMessage());
        } finally {
            closeEnvironment();
        }

        return pflow.id;
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows) {
        return this.addFlows(sender, flows, null);
    }

    public LinkedList<Long> addFlows(String sender, LinkedList<Flow> flows, HttpServletRequest request) {
        LinkedList<Long> ids = new LinkedList();

        Boolean success = true;

        try {
            setupEnvironment();

            EntityStore entityStore = null;
            StoreConfig storeConfig = new StoreConfig();
            storeConfig.setAllowCreate(true);
            storeConfig.setReadOnly(false);

            entityStore = new EntityStore(environment, "Flow", storeConfig);
            try {
                FlowAccessor dataAccessor = new FlowAccessor(entityStore);

                for (Flow flow : flows) {
                    if ((flow.protocol == 6) || (flow.protocol == 17)) {
                        globalConfigurationManager.addFrequency(customer, flow.protocol, flow.ports);
                    }

                    PersistentFlow pflow = flow.toHashTableFlow();
                    dataAccessor.flowById.put(pflow);

                    ids.add(pflow.id);
                }
            } catch (DatabaseException e) {
                success = false;
                System.err.println("addFlows Failed: " + e.getMessage());
                if (request != null) {
                    System.err.println("Requester: " + request.getRemoteAddr());
                }
            } finally {
                entityStore.close();
            }
        } catch (DatabaseException e) {
            success = false;
            System.err.println("Database Error: " + e.getMessage());
            if (request != null) {
                System.err.println("Requester: " + request.getRemoteAddr());
            }
        } catch (Exception e) {
            success = false;
            System.err.println(e.getMessage());
        } finally {
            closeEnvironment();
        }

        if (success) {
            System.out.println("Charging " + flows.size() + " to: " + customer.getId());
            accountingManager.addFlows(customer.getId(), sender, flows.size());
        }

        return ids;
    }
}
