package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import name.justinthomas.flower.analysis.statistics.StatisticsManager;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import java.net.InetAddress;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.element.Network;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolume;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;

@Singleton
@Startup
@DependsOn("ConfigurationManager")
public class FlowManager {

    private static final Integer DEBUG = 2;
    private ConfigurationManager configurationManager;

    public static FlowManager getFlowManager() {
        FlowManager flowManager = null;
        try {
            Context context = new InitialContext();
            flowManager = (FlowManager) context.lookup("java:global/Analysis/FlowManager");
        } catch (NamingException ne) {
            ne.printStackTrace();
        }
        return flowManager;
    }

    @PostConstruct
    public void init() {
        if (FlowManager.DEBUG >= 1) {
            System.out.println("Initializing FlowManager()");
        }

        configurationManager = ConfigurationManager.getConfigurationManager();

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
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
    }

    private Environment setupEnvironment() {
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/" + configurationManager.getFlowDirectory());

        try {
            if (!environmentHome.exists()) {
                if (!environmentHome.mkdirs()) {
                    throw new Exception("Could not open or create base statistics directory.");
                }
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }

        EnvironmentConfig environmentConfig = new EnvironmentConfig();

        environmentConfig.setAllowCreate(true);
        environmentConfig.setReadOnly(false);
        environmentConfig.setLockTimeout(15, TimeUnit.SECONDS);

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
        //storeConfig.setDeferredWrite(true);

        return storeConfig;
    }

    private void closeStore(EntityStore store) {
        try {
            store.close();
        } catch (DatabaseException e) {
            e.printStackTrace();
        }
    }

    @Lock(LockType.READ)
    public void getFlows(HttpSession session, String constraintsString) {

        System.out.println("getFlows called.");
        Constraints constraints = new Constraints(constraintsString);
        SessionManager.getPackets(session).clear();

        StatisticsManager statisticsManager = new StatisticsManager();
        LinkedList<StatisticalInterval> intervals = statisticsManager.getStatisticalIntervals(session, constraints);

        LinkedHashMap<Long, Long> flowIDs = new LinkedHashMap();

        for (StatisticalInterval interval : intervals) {
            for (Long id : interval.flowIDs) {
                flowIDs.put(id, null);
            }
        }

        System.out.println("Total flagged IDs: " + flowIDs.size());
        Environment environment;
        EntityStore readOnlyEntityStore = new EntityStore(environment = setupEnvironment(), "Flow", this.getStoreConfig(true));
        FlowAccessor dataAccessor = new FlowAccessor(readOnlyEntityStore);

        if (DEBUG >= 1) {
            System.out.println("Iterating over query results.");
        }

        Integer flowsProcessed = 0;
        try {
            for (Long id : flowIDs.keySet()) {
                if (dataAccessor.flowById.contains(id)) {
                    PersistentFlow pflow = dataAccessor.flowById.get(id);
                    Boolean select = false;
                    if (constraints.sourceAddressList.isEmpty() || constraints.sourceAddressList.contains(InetAddress.getByName(pflow.source))) {
                        select = true;
                    } else if (constraints.destinationAddressList.isEmpty() || constraints.destinationAddressList.contains(InetAddress.getByName(pflow.destination))) {
                        select = true;
                    }

                    if (select) {
                        SessionManager.getPackets(session).add(new Flow(pflow));
                    }

                    if (DEBUG > 0) {
                        if (++flowsProcessed % 10000 == 0) {
                            System.out.println("Flows processed: " + flowsProcessed);
                        }
                    }

                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException();
                    }
                }
            }

        } catch (InterruptedException ie) {
            System.err.println("FlowManager interrupted during getFlows...");
            ie.printStackTrace();
        } catch (UnknownHostException uhe) {
            System.err.println("FlowManager interrupted during getFlows...");
            uhe.printStackTrace();
        } finally {
            closeStore(readOnlyEntityStore);
            closeEnvironment(environment);
        }
    }

    @Lock(LockType.READ)
    public XMLDataVolumeList getXMLDataVolumes(HttpSession session, String constraints, Integer nmb_bins) {
        XMLDataVolumeList volumeList = new XMLDataVolumeList();

        Boolean cancelVolume = false;
        StatisticsManager statisticsManager = new StatisticsManager();

        try {
            LinkedHashMap<Date, HashMap<String, Long>> bins = statisticsManager.getVolumeByTime(session, new Constraints(constraints), nmb_bins);

            for (Date date : bins.keySet()) {
                XMLDataVolume volume = new XMLDataVolume();

                volume.date = date;
                volume.total = bins.get(date).get("total");
                if (DEBUG > 0) {
                    if (bins.get(date).get("total") < 0) {
                        System.err.println("Total: " + bins.get(date).get("total"));
                    }
                }
                volume.tcp = bins.get(date).get("tcp");
                volume.udp = bins.get(date).get("udp");
                volume.icmp = bins.get(date).get("icmp");
                volume.ipsec = bins.get(date).get("ipsec");
                volume.ipv4 = bins.get(date).get("ipv4");
                volume.ipv6 = bins.get(date).get("ipv6");

                volumeList.bins.add(volume);

                if (cancelVolume) {
                    break;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            System.err.println("ClassNotFoundException caught in " + cnfe.getStackTrace()[0].getMethodName() + ": " + cnfe.getMessage());
            cnfe.printStackTrace();
        }

        if (!cancelVolume) {
            volumeList.ready = true;
            SessionManager.setVolumes(session, volumeList);
            SessionManager.isHistogramBuilding(session, null);
        } else {
            System.out.println("Canceled volume build");
        }

        return volumeList;
    }

    @Lock(LockType.READ)
    public XMLNetworkList getXMLNetworks(HttpSession session, String constraints) {
        XMLNetworkList networkList = new XMLNetworkList();
        StatisticsManager statisticsManager = new StatisticsManager();
        ArrayList<Network> networks = statisticsManager.getNetworks(session, new Constraints(constraints));

        Boolean cancelNetworks = false;

        for (Network network : networks) {
            networkList.networks.add(network.toXMLNetwork());

            if (cancelNetworks) {
                break;
            }
        }

        if (!cancelNetworks) {
            networkList.ready = true;
            SessionManager.setNetworks(session, networkList);
            SessionManager.isMapBuilding(session, null);
        } else {
            System.out.println("Canceled map build");
        }

        return networkList;
    }

    public void cleanFlows(ArrayList<Long> flowIDs) {
        System.out.println("Deleting expired flows...");

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Flow", this.getStoreConfig(false));
        FlowAccessor dataAccessor = new FlowAccessor(entityStore);

        int deletedCount = 0;
        for (Long flowID : flowIDs) {
            if (dataAccessor.flowById.contains(flowID)) {
                dataAccessor.flowById.delete(flowID);

                if (++deletedCount % 1000 == 0) {
                    System.out.println("Flows deleted: " + deletedCount);
                }
            }
        }

        System.out.println("Total flows deleted: " + deletedCount);

        closeStore(entityStore);
        recordEnvironmentStatistics(environment);
        cleanLog(environment);
        checkpoint(environment);
        closeEnvironment(environment);
    }

    private void cleanLog(Environment environment) {
        try {
            environment.cleanLog();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkpoint(Environment environment) {
        CheckpointConfig checkpointConfig = new CheckpointConfig();
        try {
            environment.checkpoint(checkpointConfig);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void recordEnvironmentStatistics(Environment environment) {
        try {
            FileWriter writer = new FileWriter("/traces/databasestatistics.txt", true);
            writer.append("Date: " + new Date() + "\n");
            StatsConfig config = new StatsConfig();
            config.setClear(true);
            writer.append(environment.getStats(config).toStringVerbose());
            writer.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }
}
