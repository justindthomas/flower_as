package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.je.CheckpointConfig;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.persist.EntityCursor;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.StoreConfig;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import javax.servlet.http.HttpSession;
import name.justinthomas.flower.analysis.element.DefaultNode;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.element.InetNetwork;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
import name.justinthomas.flower.analysis.element.Network;
import name.justinthomas.flower.analysis.element.Node;
import name.justinthomas.flower.analysis.persistence.ConfigurationManager;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.utility.AddressAnalysis;

/**
 *
 * @author justin
 */
public class StatisticsManager {

    private static final Integer DEBUG = 1;
    ConfigurationManager configurationManager = ConfigurationManager.getConfigurationManager();

    private Environment setupEnvironment() {
        File environmentHome = new File(configurationManager.getBaseDirectory() + "/" + configurationManager.getStatisticsDirectory());

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
        storeConfig.setDeferredWrite(true);

        return storeConfig;
    }

    private void closeStore(EntityStore store) {
        try {
            store.close();
        } catch (DatabaseException e) {
            System.err.println("Error closing EntityStore: " + e.getMessage());
        }
    }

    public ArrayList<Long> cleanStatisticalIntervals() {
        HashMap<IntervalKey, Boolean> keys = identifyExpiredIntervals();

        System.out.println("Deleting expired intervals...");

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Statistics", this.getStoreConfig(false));
        StatisticsAccessor dataAccessor = new StatisticsAccessor(entityStore);
        int nullIDs = 0;
        ArrayList<Long> flowIDs = new ArrayList();
        for (Entry<IntervalKey, Boolean> key : keys.entrySet()) {
            if (key.getValue()) {
                for (Long flowID : dataAccessor.intervalByKey.get(key.getKey()).flowIDs) {
                    if (flowID != null) {
                        flowIDs.add(flowID);
                    } else {
                        nullIDs++;
                    }
                }
            }
            dataAccessor.intervalByKey.delete(key.getKey());
        }

        System.out.println("Null flow IDs found in StatisticalIntervals: " + nullIDs);

        closeStore(entityStore);
        recordEnvironmentStatistics(environment);
        cleanLog(environment);
        checkpoint(environment);
        closeEnvironment(environment);

        return flowIDs;
    }

    private HashMap<IntervalKey, StatisticalInterval> flowToInterval(Flow flow, Long interval, Long flowID) {
        HashMap<IntervalKey, StatisticalInterval> normalized = new HashMap<IntervalKey, StatisticalInterval>();

        Long startSecond = (flow.startTimeStamp.getTime() / interval);
        Long endSecond = (flow.lastTimeStamp.getTime() / interval);
        Long spread = endSecond - startSecond;

        for (long l = startSecond; l <= endSecond; l++) {
            StatisticalInterval sInterval = new StatisticalInterval();
            sInterval.key = new IntervalKey(l, interval);
            normalized.put(new IntervalKey(l, interval), sInterval.addFlow(new StatisticalFlow(spread, flow), flowID));
        }

        return normalized;
    }

    public void storeStatisticalIntervals(ArrayList<StatisticalInterval> intervals) {
        //System.out.println("Persisting " + intervals.size() + " intervals to long-term storage.");
        EntityStore entityStore = null;
        Environment environment = null;

        try {
            environment = setupEnvironment();

            entityStore = new EntityStore(environment, "Statistics", this.getStoreConfig(false));

            try {
                StatisticsAccessor dataAccessor = new StatisticsAccessor(entityStore);

                for (StatisticalInterval interval : intervals) {
                    if (dataAccessor.intervalByKey.get(interval.key) != null) {
                        StatisticalInterval stored = dataAccessor.intervalByKey.get(interval.key);
                        stored = stored.addInterval(interval);
                        dataAccessor.intervalByKey.put(stored);
                    } else {
                        dataAccessor.intervalByKey.put(interval);
                    }
                }

            } catch (DatabaseException e1) {
                System.err.println("storeStatisticalInterval Failed: " + e1.getMessage());
            } catch (ArrayIndexOutOfBoundsException e2) {
                System.err.println("storeStatisticalInterval Failed: " + e2.getMessage());
            } finally {
                closeStore(entityStore);
            }
        } catch (DatabaseException e) {
            System.err.println("Database Error: " + e.getMessage());
        } finally {
            closeEnvironment(environment);
        }
    }

    public void addStatisticalSeconds(Flow flow, Long flowID) {
        for (Long resolution : configurationManager.getResolution().keySet()) {
            HashMap<IntervalKey, StatisticalInterval> normalized = flowToInterval(flow, resolution, flowID);

            for (Entry<IntervalKey, StatisticalInterval> entry : normalized.entrySet()) {
                CachedStatistics.put(entry.getKey(), entry.getValue());
            }
            //System.out.println("Adding " + normalized.size() + " intervals to resolution " + resolution);
        }
    }

    private Long getResolution(long duration, Integer bins) {
        List<Long> resolutions = new ArrayList(configurationManager.getResolution().keySet());

        // This sorts the resolutions from highest to lowest (smallest number to largest number)
        Collections.sort(resolutions);

        Long returnValue = null;

        for (Long resolution : resolutions) {
            if ((bins != null) && (bins > 0)) {
                if ((duration / resolution) >= bins) {
                    returnValue = resolution;
                }
            } else {
                if (resolution == null) {
                    // This is only triggered on the first evaluation of 'resolutions' (i.e., the smallest number,
                    // aka. highest resolution)
                    if (duration <= (resolution * 3)) {
                        // Example: if duration is 10 seconds, the (default) highest resolution is 5(*3) seconds;
                        // we just select the first (smallest number, highest resolution) and break out.
                        returnValue = resolution;
                        break;
                    }
                }

                if (duration > (resolution * 3)) {
                    // Using 3 as a multiplier is arbitrary; a different number might prove to be more efficient/effective.
                    returnValue = resolution;
                }
            }
        }

        System.out.println("Setting resolution to: " + returnValue);
        return returnValue;
    }

    public LinkedList<StatisticalInterval> getStatisticalIntervals(HttpSession session, Constraints constraints, Integer resolution) {
        LinkedList<StatisticalInterval> intervals = new LinkedList();

        Long duration = constraints.endTime.getTime() - constraints.startTime.getTime();
        Environment environment;
        Long r = (resolution != null) ? resolution : getResolution(duration, null);

        EntityStore store = new EntityStore(environment = setupEnvironment(), "Statistics", this.getStoreConfig(true));
        StatisticsAccessor accessor = new StatisticsAccessor(store);
        Long start = constraints.startTime.getTime() / r;
        IntervalKey startKey = new IntervalKey(start, r);
        Long end = constraints.endTime.getTime() / r;
        IntervalKey endKey = new IntervalKey(end, r);

        EntityCursor<StatisticalInterval> cursor = accessor.intervalByKey.entities(startKey, true, endKey, true);

        for (StatisticalInterval interval : cursor) {
            intervals.add(interval);
        }

        cursor.close();
        closeStore(store);
        closeEnvironment(environment);

        return intervals;
    }

    public LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>> getVolumeByTime(Constraints constraints, Integer bins)
            throws ClassNotFoundException {

        LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>> consolidated = new LinkedHashMap();

        if ((constraints.startTime == null) || (constraints.endTime == null) || (bins == null) || (constraints.startTime.getTime() >= constraints.endTime.getTime())) {
            return consolidated;
        }

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        if (StatisticsManager.DEBUG >= 1) {
            System.out.println("Requesting volume data from " + dateFormat.format(constraints.startTime) + " to " + dateFormat.format(constraints.endTime));
        }

        if (StatisticsManager.DEBUG >= 1) {
            System.out.println("Populating consolidated map");
        }
        Long duration = constraints.endTime.getTime() - constraints.startTime.getTime();
        System.out.println("Duration: " + duration);

        Long interval = duration / bins;

        // Populate consolidated with an entry for each minute that we want to graph
        int j;
        for (j = 0; j < bins;) {
            HashMap<String, HashMap<Integer, Long>> interim = new HashMap();
            interim.put("versions", new HashMap<Integer, Long>());
            interim.put("types", new HashMap<Integer, Long>());
            interim.put("tcp", new HashMap<Integer, Long>());
            interim.put("udp", new HashMap<Integer, Long>());

            consolidated.put(new Date(constraints.startTime.getTime() + (j++ * interval)), interim);
        }

        if (StatisticsManager.DEBUG >= 1) {
            System.out.println("Iterating over flow volumes in database");
        }

        Environment environment;
        Long resolution = getResolution(duration, bins);

        EntityStore store = new EntityStore(environment = setupEnvironment(), "Statistics", this.getStoreConfig(true));
        StatisticsAccessor accessor = new StatisticsAccessor(store);
        Long start = constraints.startTime.getTime() / resolution;
        IntervalKey startKey = new IntervalKey(start, resolution);
        Long end = constraints.endTime.getTime() / resolution;
        IntervalKey endKey = new IntervalKey(end, resolution);
        //System.out.println("Start: " + start + ", End: " + end);
        EntityCursor<StatisticalInterval> cursor = accessor.intervalByKey.entities(startKey, true, endKey, true);

        // We only want to do this query once, so make it count.
        if (StatisticsManager.DEBUG >= 1) {
            System.out.println("Iterating over query results.");
        }

        Integer flowsProcessed = 0;
        try {
            try {
                try {
                    for (StatisticalInterval second : cursor) {
                        HashMap<Integer, Long> versions = new HashMap();
                        HashMap<Integer, Long> types = new HashMap();
                        HashMap<Integer, Long> tcp = new HashMap();
                        HashMap<Integer, Long> udp = new HashMap();

                        for (StatisticalFlow entry : second.flows.values()) {
                            try {
                                for (Entry<StatisticalFlowDetail, Long> volume : entry.getCount().entrySet()) {
                                    if (volume.getKey().getType().equals(StatisticalFlowDetail.Count.BYTE)) {

                                        if (types.containsKey(volume.getKey().getProtocol())) {
                                            types.put(volume.getKey().getProtocol(), types.get(volume.getKey().getProtocol()) + volume.getValue());
                                        } else {
                                            types.put(volume.getKey().getProtocol(), volume.getValue());
                                        }

                                        if (volume.getKey().getProtocol() == 6) {
                                            if (tcp.containsKey(volume.getKey().getDestination())) {
                                                tcp.put(volume.getKey().getDestination(), tcp.get(volume.getKey().getDestination()) + volume.getValue());
                                            } else {
                                                tcp.put(volume.getKey().getDestination(), volume.getValue());
                                            }
                                        }

                                        if (volume.getKey().getProtocol() == 17) {
                                            if (udp.containsKey(volume.getKey().getDestination())) {
                                                udp.put(volume.getKey().getDestination(), udp.get(volume.getKey().getDestination()) + volume.getValue());
                                            } else {
                                                udp.put(volume.getKey().getDestination(), volume.getValue());
                                            }
                                        }

                                        if (volume.getKey().getVersion().equals(StatisticalFlowDetail.Version.IPV4)) {
                                            if (versions.containsKey(4)) {
                                                versions.put(4, versions.get(4) + volume.getValue());
                                            } else {
                                                versions.put(4, volume.getValue());
                                            }
                                        } else if (volume.getKey().getVersion().equals(StatisticalFlowDetail.Version.IPV6)) {
                                            if (versions.containsKey(6)) {
                                                versions.put(6, versions.get(6) + volume.getValue());
                                            } else {
                                                versions.put(6, volume.getValue());
                                            }
                                        }
                                    }

                                }
                            } catch (NullPointerException e) {
                                System.err.println("Unexpected NULL field encountered.  This is probably a result of bad data in the database (perhaps an interrupted insert?)");
                                continue;
                            }
                        }

                        // Iterate over the bins in consolidated
                        for (Date bin : consolidated.keySet()) {
                            if (Thread.currentThread().isInterrupted()) {
                                throw new InterruptedException();
                            }

                            if (new Date(second.getSecond().interval * resolution).after(new Date(bin.getTime())) && new Date(second.getSecond().interval * resolution).before(new Date(bin.getTime() + interval))) {
                                if(!types.isEmpty()) {
                                    for(Integer type : types.keySet()) {
                                        if(consolidated.get(bin).get("types").containsKey(type)) {
                                            consolidated.get(bin).get("types").put(type, consolidated.get(bin).get("types").get(type) + types.get(type));
                                        } else {
                                            consolidated.get(bin).get("types").put(type, types.get(type));
                                        }
                                    }
                                }

                                if(!tcp.isEmpty()) {
                                    for(Integer destination : tcp.keySet()) {
                                        if(consolidated.get(bin).get("tcp").containsKey(destination)) {
                                            consolidated.get(bin).get("tcp").put(destination, consolidated.get(bin).get("tcp").get(destination) + tcp.get(destination));
                                        } else {
                                            consolidated.get(bin).get("tcp").put(destination, tcp.get(destination));
                                        }
                                    }
                                }

                                if(!udp.isEmpty()) {
                                    for(Integer destination : udp.keySet()) {
                                        if(consolidated.get(bin).get("udp").containsKey(destination)) {
                                            consolidated.get(bin).get("udp").put(destination, consolidated.get(bin).get("udp").get(destination) + udp.get(destination));
                                        } else {
                                            consolidated.get(bin).get("udp").put(destination, udp.get(destination));
                                        }
                                    }
                                }

                                if(!versions.isEmpty()) {
                                    for(Integer version : versions.keySet()) {
                                        if(consolidated.get(bin).get("versions").containsKey(version)) {
                                            consolidated.get(bin).get("versions").put(version, consolidated.get(bin).get("versions").get(version) + versions.get(version));
                                        } else {
                                            consolidated.get(bin).get("versions").put(version, versions.get(version));
                                        }
                                    }
                                }
                            }
                        }

                        if (DEBUG > 0) {
                            if (++flowsProcessed % 1000 == 0) {
                                System.out.println("StatisticalSeconds processed: " + flowsProcessed);
                            }
                        }

                        if (Thread.currentThread().isInterrupted()) {
                            System.out.println("StatisticsManager was interrupted");
                            throw new InterruptedException();
                        }
                    }
                } catch (DatabaseException e) {
                    System.err.println("Database error: " + e.getMessage());
                } finally {
                    cursor.close();
                }
            } catch (DatabaseException e) {
                System.err.println("Database error: " + e.getMessage());
            } finally {
                closeStore(store);
            }
        } catch (InterruptedException ie) {
            System.err.println("Stopping StatisticsManager during Volume build...");
        } finally {
            closeEnvironment(environment);
        }

        //System.out.println("Returning from FlowManager:getVolumeByTime");
        return consolidated;
    }

    public StatisticalFlow setDefault(ArrayList<Network> networks, StatisticalFlow flow) throws UnknownHostException {
        Boolean sourceManaged = false;
        Boolean destinationManaged = false;

        for (Network network : networks) {
            if (!sourceManaged && AddressAnalysis.isMember(InetAddress.getByName(flow.getSource()), network.getNetwork())) {
                sourceManaged = true;
            }
            if (!destinationManaged && AddressAnalysis.isMember(InetAddress.getByName(flow.getDestination()), network.getNetwork())) {
                destinationManaged = true;
            }
            if (sourceManaged && destinationManaged) {
                break;
            }
        }

        if (!sourceManaged) {
            flow.setSource("0.0.0.0");
        }

        if (!destinationManaged) {
            flow.setDestination("0.0.0.0");
        }

        return flow;
    }

    public ArrayList<Network> getNetworks(HttpSession session, Constraints constraints) {
        LinkedHashMap<String, InetNetwork> managedNetworks = new ManagedNetworks().getNetworks();

        ArrayList<Network> networks = new ArrayList<Network>();

        for (InetNetwork network : managedNetworks.values()) {
            networks.add(new Network(network));
        }

        Integer g = 0, n = 0;

        Network defaultNetwork = null;

        try {
            defaultNetwork = new Network(InetAddress.getByName("0.0.0.0"), 0, "DEFAULT");
        } catch (UnknownHostException uhe) {
            System.err.println("Could not parse network for DEFAULT: " + uhe.getMessage());
        }

        DefaultNode defaultNode = new DefaultNode();

        long duration = constraints.endTime.getTime() - constraints.startTime.getTime();
        long resolution = getResolution(duration, null);
        Environment environment;
        EntityStore readOnlyEntityStore = new EntityStore(environment = setupEnvironment(), "Statistics", this.getStoreConfig(true));
        StatisticsAccessor accessor = new StatisticsAccessor(readOnlyEntityStore);
        IntervalKey startKey = new IntervalKey(constraints.startTime.getTime() / resolution, resolution);
        IntervalKey endKey = new IntervalKey(constraints.endTime.getTime() / resolution, resolution);
        EntityCursor<StatisticalInterval> cursor = accessor.intervalByKey.entities(startKey, true, endKey, true);

        // We only want to do this query once, so make it count.

        System.out.println("Iterating over query results.");

        Integer secondsProcessed = 0;
        try {
            try {
                try {

                    for (StatisticalInterval second : cursor) {
                        for (StatisticalFlow flow : second.flows.values()) {
                            flow = setDefault(networks, flow);

                            if (flow.getSource() == null) {
                                n++;
                            } else {
                                InetAddress sourceAddress = InetAddress.getByName(flow.getSource());
                                InetAddress destinationAddress = InetAddress.getByName(flow.getDestination());
                                Node sourceNode = new Node(flow.getSource());
                                Node destinationNode = new Node(flow.getDestination());

                                //System.out.println(".");
                                // Associate the flow with the source (flows are never associated with the destination)
                                sourceNode.addFlow(flow);
                                // On that last point, just kidding
                                destinationNode.addFlow(flow);

                                Boolean sourceCaptured = false;
                                Boolean destinationCaptured = false;
                                // Iterate through managed networks and add the Nodes (complete with Flows)
                                // as they are encountered
                                for (Network network : networks) {
                                    if (!sourceCaptured || !destinationCaptured) {
                                        // If the source address belongs to a managed network, add it to the map
                                        // De-duplication is handled by the "addNode" method in the Network object
                                        if (AddressAnalysis.isMember(sourceAddress, network.getNetwork())) {
                                            network.addNode(sourceNode);
                                            sourceCaptured = true;
                                        }

                                        if (AddressAnalysis.isMember(destinationAddress, network.getNetwork())) {
                                            network.addNode(destinationNode);
                                            destinationCaptured = true;
                                        }

                                        if (Thread.currentThread().isInterrupted()) {
                                            throw new InterruptedException();
                                        }
                                    }
                                }

                                if (!sourceCaptured) {
                                    if (!flow.getDestination().equals("0.0.0.0")) {
                                        defaultNode.addFlow(flow);
                                    }
                                } else if (!destinationCaptured) {
                                    defaultNode.addFlow(flow);
                                }

                                // If the flow was not captured, add it to the default node
                                //if (!sourceCaptured || !destinationCaptured) {
                                //    defaultNode.addFlow(flow);
                                //}
                            }

                            if (Thread.currentThread().isInterrupted()) {
                                //System.out.println("Stopping FlowManager...");
                                throw new InterruptedException();
                            }
                        }

                        if (++secondsProcessed % 10000 == 0) {
                            System.out.println(secondsProcessed + " StatisticalSeconds processed.");
                        }
                    }
                } catch (DatabaseException e) {
                    System.err.println("Database error: " + e.getMessage());
                } finally {
                    cursor.close();
                }
            } catch (DatabaseException e) {
                System.err.println("Database error: " + e.getMessage());
            } finally {
                closeStore(readOnlyEntityStore);
            }
        } catch (UnknownHostException e) {
            System.err.println("UnknownHostException caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        } catch (InterruptedException ie) {
            System.err.println("Stopped FlowManager during network build");
        } catch (Exception e) {
            System.err.println("Exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        } finally {
            closeEnvironment(environment);
        }

        System.out.println(secondsProcessed + " total StatisticalSeconds analyzed.");
        System.out.println("Found " + n + " null flows.");
        System.out.println("Found " + g + " non-IP flows.");
        defaultNetwork.addNode(defaultNode);

        // Add the default network to the bottom of the network list
        networks.add(defaultNetwork);

        //createTraceFile(networks);

        return networks;
    }

    private void createTraceFile(ArrayList<Network> networks) {
        try {
            FileWriter writer = new FileWriter("/traces/analysis_out.txt");
            for (Network network : networks) {
                writer.append("Monitored Network: " + network.getNetwork().toString() + " has " + network.getAllNodes().size() + " entries.\n");
                for (Node node : network.getAllNodes()) {
                    writer.append("n:" + node.toString() + ", receivedBytes: " + node.getBytesReceived() + ", sentBytes: " + node.getBytesSent() + "\n");
                    for (Flow flow : node.getFlowsOriginated()) {
                        writer.append("s:" + flow.toString() + "\n");
                    }

                    for (Flow flow : node.getFlowsReceived()) {
                        writer.append("d:" + flow.toString() + "\n");
                    }
                }
            }        // Add the default node (with newly acquired flows) to the default network
            writer.close();
        } catch (IOException ioe) {
            System.err.println("Error creating trace file: " + ioe.getMessage());
        }
    }

    private HashMap<IntervalKey, Boolean> identifyExpiredIntervals() {
        System.out.println("Identifying expired intervals...");

        Environment environment;
        EntityStore entityStore = new EntityStore(environment = setupEnvironment(), "Statistics", this.getStoreConfig(true));
        StatisticsAccessor dataAccessor = new StatisticsAccessor(entityStore);

        Date now = new Date();
        Date start = new Date();
        start.setTime(0l);

        HashMap<IntervalKey, Boolean> expiredIntervals = new HashMap();

        for (Entry<Long, Boolean> resolution : configurationManager.getResolution().entrySet()) {
            IntervalKey startKey = new IntervalKey();
            startKey.resolution = resolution.getKey();
            startKey.interval = start.getTime() / resolution.getKey();

            IntervalKey endKey = new IntervalKey();
            endKey.resolution = resolution.getKey();
            endKey.interval = (now.getTime() / resolution.getKey()) - 100000;
            // The above line keeps 100000 of any resolution around.  At it's most fine (10000 ms), this is about a week and a half.
            // At it's most coarse (10000000 ms), this is about 30 years.

            EntityCursor<StatisticalInterval> cursor = dataAccessor.intervalByKey.entities(startKey, true, endKey, true);

            Integer intervalsDeleted = 0;

            for (StatisticalInterval statisticalInterval : cursor) {
                expiredIntervals.put(statisticalInterval.key, resolution.getValue());

                if (++intervalsDeleted % 1000 == 0) {
                    System.out.println(intervalsDeleted + " intervals marked for deletion at a resolution of " + resolution + " milliseconds...");
                }
            }

            System.out.println(intervalsDeleted + " total seconds marked for deletion at a resolution of " + resolution + " milliseconds...");
            cursor.close();
        }

        closeStore(entityStore);
        closeEnvironment(environment);

        return expiredIntervals;
    }

    private void cleanLog(Environment environment) {
        try {
            environment.cleanLog();
        } catch (DatabaseException e) {
            System.err.println("Database error running cleanLog: " + e.getMessage());
        }
    }

    private void checkpoint(Environment environment) {
        CheckpointConfig checkpointConfig = new CheckpointConfig();
        try {
            environment.checkpoint(checkpointConfig);
        } catch (DatabaseException e) {
            System.err.println("Database error running checkpoint: " + e.getMessage());
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
            System.err.println("Error recording environment statistics: " + ioe.getMessage());
        }
    }
}
