package name.justinthomas.flower.analysis.persistence;

import name.justinthomas.flower.global.GlobalConfigurationManager;
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
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpSession;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolume;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLDataVolumeList;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLNetworkList;
import name.justinthomas.flower.analysis.statistics.StatisticalInterval;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

public class FlowManager {

    private Customer customer;
    private static final Integer DEBUG = 2;
    private GlobalConfigurationManager configurationManager;

    public FlowManager(Customer customer) {
        this.customer = customer;
        
        try {
            configurationManager = InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    private Environment setupEnvironment() {
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/customers/" + customer.getDirectory() + "/flows");

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
            System.err.println("Error closing EntityStore: " + e.getMessage());
        }
    }

    public void getFlows(HttpSession session, String constraintsString, String tracker) {

        System.out.println("getFlows called.");
        Constraints constraints = new Constraints(constraintsString);
        SessionManager.getFlows(session, tracker).clear();

        StatisticsManager statisticsManager = new StatisticsManager(customer);
        LinkedList<StatisticalInterval> intervals = statisticsManager.getStatisticalIntervals(session, constraints, null);

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
                        SessionManager.getFlows(session, tracker).add(new Flow(customer, pflow));
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
            System.err.println("FlowManager interrupted during getFlows: " + ie.getMessage());
        } catch (UnknownHostException uhe) {
            System.err.println("FlowManager interrupted during getFlows: " + uhe.getMessage());
        } finally {
            closeStore(readOnlyEntityStore);
            closeEnvironment(environment);
        }
    }

    public XMLDataVolumeList getXMLDataVolumes(HttpSession session, String constr, Integer nmb_bins) {
        XMLDataVolumeList volumeList = new XMLDataVolumeList();

        Boolean cancelVolume = false;
        StatisticsManager statisticsManager = new StatisticsManager(customer);

        Constraints constraints = new Constraints(constr);
        Long intervalDuration = (constraints.endTime.getTime() - constraints.startTime.getTime()) / nmb_bins;

        try {
            LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>> bins = statisticsManager.getVolumeByTime(constraints, nmb_bins);

            for (Date date : bins.keySet()) {
                XMLDataVolume volume = new XMLDataVolume();

                volume.date = date;
                volume.duration = intervalDuration;
                volume.total = 0l;
                volume.tcp = 0l;
                volume.udp = 0l;
                volume.icmp = 0l;
                volume.icmpv6 = 0l;
                volume.ipv4 = 0l;
                volume.ipv6 = 0l;
                volume.ipsec = 0l;
                volume.sixinfour = 0l;

                for (Integer version : bins.get(date).get("versions").keySet()) {
                    volume.total += bins.get(date).get("versions").get(version);
                }

                if (bins.get(date).get("types").containsKey(6)) {
                    volume.tcp = bins.get(date).get("types").get(6);
                }

                if (bins.get(date).get("types").containsKey(17)) {
                    volume.udp = bins.get(date).get("types").get(17);
                }

                if (bins.get(date).get("types").containsKey(1)) {
                    volume.icmp = bins.get(date).get("types").get(1);
                }

                if (bins.get(date).get("types").containsKey(58)) {
                    volume.icmpv6 = bins.get(date).get("types").get(58);
                }

                if (bins.get(date).get("types").containsKey(41)) {
                    volume.sixinfour = bins.get(date).get("types").get(41);
                }

                if (bins.get(date).get("types").containsKey(50)) {
                    volume.ipsec += bins.get(date).get("types").get(50);
                }

                if (bins.get(date).get("types").containsKey(51)) {
                    volume.ipsec += bins.get(date).get("types").get(51);
                }

                if (bins.get(date).get("versions").containsKey(4)) {
                    volume.ipv4 += bins.get(date).get("versions").get(4);
                }

                if (bins.get(date).get("versions").containsKey(6)) {
                    volume.ipv6 += bins.get(date).get("versions").get(6);
                }

                volumeList.bins.add(volume);

                if (cancelVolume) {
                    break;
                }
            }
        } catch (ClassNotFoundException cnfe) {
            System.err.println("ClassNotFoundException caught: " + cnfe.getMessage());
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

    public XMLNetworkList getXMLNetworks(HttpSession session, String constraints) {
        XMLNetworkList networkList = new XMLNetworkList();
        StatisticsManager statisticsManager = new StatisticsManager(customer);
        ArrayList<Network> networks = statisticsManager.getNetworks(session, new Constraints(constraints));

        Boolean cancelNetworks = false;

        for (Network network : networks) {
            networkList.networks.add(network.toXMLNetwork());

            if (cancelNetworks) {
                break;
            }
        }

        if (!cancelNetworks) {
            System.out.println("Completed XMLNetworkList creation and writing to session...");
            networkList.ready = true;
            SessionManager.setNetworks(session, networkList);
            SessionManager.isMapBuilding(session, null);
        } else {
            System.out.println("Canceled map build");
        }

        System.out.println("Returning XMLNetworkList to caller...");
        return networkList;
    }

    public void cleanFlows(ArrayList<Long> flowIDs) {
        System.out.println("Deleting expired flows...");

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Flow", this.getStoreConfig(false));
        FlowAccessor dataAccessor = new FlowAccessor(entityStore);

        int deletedCount = 0;
        for (Long flowID : flowIDs) {
            if ((flowID != null) && dataAccessor.flowById.contains(flowID)) {
                dataAccessor.flowById.delete(flowID);

                if (++deletedCount % 1000 == 0) {
                    System.out.println("Flows deleted: " + deletedCount);
                }
            }
        }

        System.out.println("Total flows deleted: " + deletedCount);

        closeStore(entityStore);
        //recordEnvironmentStatistics(environment);
        cleanLog(environment);
        checkpoint(environment);
        closeEnvironment(environment);
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

    private void recordEnvironmentStatistics(Environment environment) {
        try {
            FileWriter writer = new FileWriter("/traces/databasestatistics.txt", true);
            writer.append("Date: " + new Date() + "\n");
            StatsConfig config = new StatsConfig();
            config.setClear(true);
            writer.append(environment.getStats(config).toStringVerbose());
            writer.close();
        } catch (IOException ioe) {
            System.err.println("Error writing database statistics: " + ioe.getMessage());
        }
    }
}
