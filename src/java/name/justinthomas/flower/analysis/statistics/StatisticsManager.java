package name.justinthomas.flower.analysis.statistics;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpSession;
import javax.transaction.*;
import name.justinthomas.flower.analysis.element.Flow;
import name.justinthomas.flower.analysis.element.InetNetwork;
import name.justinthomas.flower.analysis.element.Network;
import name.justinthomas.flower.analysis.persistence.Constraints;
import name.justinthomas.flower.analysis.services.MapDataResponse;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import name.justinthomas.flower.utility.AddressAnalysis;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

/**
 *
 * @author justin
 */
@PersistenceContext(name = "persistence/Analysis", unitName = "AnalysisPU")
public class StatisticsManager {

    private static final Logger log = Logger.getLogger(StatisticsManager.class.getName());
    private static FileAppender fileAppender;
    private GlobalConfigurationManager globalConfigurationManager;
    private EntityManager em;
    private Customer customer;

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

    public StatisticsManager(Customer customer) {
        this.customer = customer;
        this.globalConfigurationManager = StatisticsManager.getGlobalConfigurationManager();
        this.em = StatisticsManager.getEntityManager();

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

    public ArrayList<Long> cleanStatisticalIntervals() {
        /*
         * HashMap<IntervalKey, Boolean> keys = identifyExpiredIntervals();
         *
         * log.debug("Deleting expired intervals...");
         *
         * Environment environment; EntityStore entityStore = new
         * EntityStore(environment = setupEnvironment(), "Statistics",
         * this.getStoreConfig(false)); StatisticsAccessor dataAccessor = new
         * StatisticsAccessor(entityStore); int nullIDs = 0; ArrayList<Long>
         * flowIDs = new ArrayList(); for (Entry<IntervalKey, Boolean> key :
         * keys.entrySet()) { if (key.getValue()) { for (Long flowID :
         * dataAccessor.intervalByKey.get(key.getKey()).getFlowIDs()) { if
         * (flowID != null) { flowIDs.add(flowID); } else { nullIDs++; } } }
         * dataAccessor.intervalByKey.delete(key.getKey()); }
         *
         * log.error("Null flow IDs found in StatisticalIntervals: " + nullIDs);
         *
         * closeStore(entityStore); //recordEnvironmentStatistics(environment);
         * cleanLog(environment); checkpoint(environment);
         * closeEnvironment(environment);
         *
         * return flowIDs;
         *
         */
        return null;
    }

    private HashMap<IntervalKey, StatisticalInterval> flowToInterval(Flow flow, Long resolution, Long flowID) {
        HashMap<IntervalKey, StatisticalInterval> normalized = new HashMap<IntervalKey, StatisticalInterval>();

        Long startSecond = (flow.startTimeStamp.getTime() / resolution);
        Long endSecond = (flow.lastTimeStamp.getTime() / resolution);
        Long spread = endSecond - startSecond;

        for (long interval = startSecond; interval <= endSecond; interval++) {
            StatisticalInterval sInterval = new StatisticalInterval();
            sInterval.setAccountId(customer.getAccount());
            sInterval.setResolution(resolution);
            sInterval.setStatisticalInterval(interval);
            normalized.put(new IntervalKey(interval, resolution), sInterval.addFlow(new StatisticalFlow(spread, flow), flowID));
        }

        return normalized;
    }

    public StatisticalInterval getStatisticalInterval(Long resolution, Long interval) {
        System.out.println("Retrieving interval: " + resolution + ":" + interval + " from storage.");
        StatisticalInterval statisticalInterval = null;

        List<StatisticalInterval> statisticalIntervals = (List<StatisticalInterval>) em.createQuery(
                "SELECT s FROM StatisticalInterval s WHERE s.accountId LIKE :accountid AND s.resolution = :resolution AND s.statisticalInterval = :interval")
                    .setParameter("accountid", customer.getAccount())
                    .setParameter("resolution", resolution)
                    .setParameter("interval", interval)
                    .getResultList();

        if (statisticalIntervals.size() > 1) {
            System.err.println("StatisticsManager: too many results");
        } else if (!statisticalIntervals.isEmpty()) {
            statisticalInterval = statisticalIntervals.get(0);
        }

        return statisticalInterval;
    }

    public void storeStatisticalIntervals(ArrayList<StatisticalInterval> intervals) {
        System.out.println("Persisting " + intervals.size() + " intervals to long-term storage.");

        try {
            Context initCtx = new InitialContext();
            UserTransaction utx = (UserTransaction) initCtx.lookup("java:comp/UserTransaction");

            System.out.println("Beginning transaction...");

            for (StatisticalInterval interval : intervals) {
                if (this.getStatisticalInterval(interval.getResolution(), interval.getStatisticalInterval()) != null) {
                    System.out.println("interval merge");
                    StatisticalInterval stored = this.getStatisticalInterval(interval.getResolution(), interval.getStatisticalInterval());
                    stored.addInterval(interval);
                    utx.begin();
                    em.merge(stored);
                    utx.commit();
                } else {
                    System.out.println("interval persist");
                    utx.begin();
                    em.persist(interval);
                    utx.commit();
                }
            }

            System.out.println("Transaction completed...");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addStatisticalSeconds(Flow flow, Long flowID, InetAddress collector) {
        CachedStatistics cachedStatistics = null;

        synchronized (globalConfigurationManager) {
            cachedStatistics = globalConfigurationManager.getCachedStatistics(customer.getAccount());

            if (cachedStatistics == null) {
                globalConfigurationManager.setCachedStatistics(customer.getAccount(), new CachedStatistics(customer));
                cachedStatistics = globalConfigurationManager.getCachedStatistics(customer.getAccount());
            }
        }

        try {
            if (cachedStatistics.hasOtherRepresentation(flow.getSourceAddress(), flow.getDestinationAddress(), collector)) {
                log.debug("Ignoring duplicate flow from: " + collector.getHostAddress()
                        + ", already represented by: " + cachedStatistics.getRepresentation(flow.getSourceAddress(), flow.getDestinationAddress()).getHostAddress());
                return;
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        for (Long resolution : globalConfigurationManager.getResolutionMap().keySet()) {
            HashMap<IntervalKey, StatisticalInterval> normalized = flowToInterval(flow, resolution, flowID);

            for (Entry<IntervalKey, StatisticalInterval> entry : normalized.entrySet()) {
                cachedStatistics.put(entry.getKey(), entry.getValue());
            }
            //log.debug("Adding " + normalized.size() + " intervals to resolution " + resolution);
        }

        //globalConfigurationManager.setCachedStatistics(customer.getId(), cachedStatistics);
    }

    private Long getResolution(long duration, Integer bins) {
        List<Long> resolutions = new ArrayList(globalConfigurationManager.getResolutionMap().keySet());

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

        log.debug("Setting resolution to: " + returnValue);
        return returnValue;
    }

    public StatisticalCube getCube() {
        /*
         * Environment environment; EntityStore store = new
         * EntityStore(environment = setupEnvironment(), "Statistics",
         * this.getStoreConfig(false));
         *
         * try { StatisticsAccessor accessor = new StatisticsAccessor(store);
         * StatisticalCube cube =
         * accessor.cubesByCustomer.get(customer.getAccount()); if (cube !=
         * null) { log.debug("Retrieved cubes for customer: " + customer.getId()
         * + " with " + cube.getStatistics().size() + " mappings"); } return
         * cube; } finally { closeStore(store); closeEnvironment(environment); }
         *
         */
        return null;
    }

    public void storeCube(StatisticalCube cube) {
        /*
         * Environment environment; EntityStore store = new
         * EntityStore(environment = setupEnvironment(), "Statistics",
         * this.getStoreConfig(false));
         *
         * log.debug("Beginning cube storage routine"); if (cube.getStatistics()
         * != null) { try { StatisticsAccessor accessor = new
         * StatisticsAccessor(store); log.debug("Storing cubes for customer: " +
         * customer.getId() + " with " + cube.getStatistics().size() + "
         * mappings"); accessor.cubesByCustomer.put(cube); } catch (Throwable t)
         * { log.error(t.toString()); } finally { closeStore(store);
         * closeEnvironment(environment); } } else { log.debug("Cube has no
         * statistics yet."); } log.debug("Cube storage complete.");
         *
         */
    }

    public List<StatisticalInterval> getStatisticalIntervals(HttpSession session, Constraints constraints, Integer resolution) {
        List<StatisticalInterval> intervals = null;

        Long duration = constraints.endTime.getTime() - constraints.startTime.getTime();
        Long r = (resolution != null) ? resolution : getResolution(duration, null);

        Long start = constraints.startTime.getTime() / r;
        IntervalKey startKey = new IntervalKey(start, r);
        Long end = constraints.endTime.getTime() / r;
        IntervalKey endKey = new IntervalKey(end, r);

        try {
            intervals = (List<StatisticalInterval>) em.createQuery(
                    "SELECT s FROM StatisticalInterval s WHERE s.accountId LIKE :accountid AND s.key >= :start AND s.key <= :end").setParameter("accountid", customer.getAccount()).setParameter("resolution", r).setParameter("start", startKey).setParameter("end", endKey).getResultList();
        } catch (NoResultException nre) {
            System.err.println("StatisticsManager: no result");
            nre.printStackTrace();
        }
        /*
         * LinkedList<StatisticalInterval> intervals = new LinkedList();
         *
         * Long duration = constraints.endTime.getTime() -
         * constraints.startTime.getTime(); Environment environment; Long r =
         * (resolution != null) ? resolution : getResolution(duration, null);
         *
         * EntityStore store = new EntityStore(environment = setupEnvironment(),
         * "Statistics", this.getStoreConfig(true)); StatisticsAccessor accessor
         * = new StatisticsAccessor(store); Long start =
         * constraints.startTime.getTime() / r; IntervalKey startKey = new
         * IntervalKey(start, r); Long end = constraints.endTime.getTime() / r;
         * IntervalKey endKey = new IntervalKey(end, r);
         *
         * EntityCursor<StatisticalInterval> cursor =
         * accessor.intervalByKey.entities(startKey, true, endKey, true);
         *
         * for (StatisticalInterval interval : cursor) {
         * intervals.add(interval); }
         *
         * cursor.close(); closeStore(store); closeEnvironment(environment);
         *
         * return intervals;
         *
         */
        return intervals;
    }

    public LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>> getVolumeByTime(Constraints constraints, Integer bins)
            throws ClassNotFoundException {

        /*
         * LinkedHashMap<Date, HashMap<String, HashMap<Integer, Long>>>
         * consolidated = new LinkedHashMap();
         *
         * if ((constraints.startTime == null) || (constraints.endTime == null)
         * || (bins == null) || (constraints.startTime.getTime() >=
         * constraints.endTime.getTime())) { return consolidated; }
         *
         * DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd
         * HH:mm:ss.SSS"); log.debug("Requesting volume data from " +
         * dateFormat.format(constraints.startTime) + " to " +
         * dateFormat.format(constraints.endTime)); log.debug("Populating
         * consolidated map"); Long duration = constraints.endTime.getTime() -
         * constraints.startTime.getTime(); log.debug("Duration: " + duration);
         *
         * Long interval = duration / bins;
         *
         * // Populate consolidated with an entry for each minute that we want
         * to graph int j; for (j = 0; j < bins;) { HashMap<String,
         * HashMap<Integer, Long>> interim = new HashMap();
         * interim.put("versions", new HashMap<Integer, Long>());
         * interim.put("types", new HashMap<Integer, Long>());
         * interim.put("tcp", new HashMap<Integer, Long>()); interim.put("udp",
         * new HashMap<Integer, Long>());
         *
         * consolidated.put(new Date(constraints.startTime.getTime() + (j++ *
         * interval)), interim); }
         *
         * log.debug("Iterating over flow volumes in database");
         *
         * Long resolution = getResolution(duration, bins);
         *
         * if (resolution == null) { log.error("automatically selected
         * resolution is null; no resolution is granular enough to support the
         * number of bins selected over the selected time period"); return null;
         * }
         *
         * Long start = constraints.startTime.getTime() / resolution;
         * IntervalKey startKey = new IntervalKey(start, resolution); Long end =
         * constraints.endTime.getTime() / resolution; IntervalKey endKey = new
         * IntervalKey(end, resolution); //log.debug("Start: " + start + ", End:
         * " + end); Integer flowsProcessed = 0;
         *
         * while (startKey.getStatisticalInterval().longValue() <
         * endKey.getStatisticalInterval().longValue()) { log.debug("start: " +
         * startKey.getStatisticalInterval() + ", end: " +
         * endKey.getStatisticalInterval()); Environment environment;
         * EntityStore store = new EntityStore(environment = setupEnvironment(),
         * "Statistics", this.getStoreConfig(true)); StatisticsAccessor accessor
         * = new StatisticsAccessor(store);
         *
         * EntityCursor<StatisticalInterval> cursor =
         * accessor.intervalByKey.entities(startKey, true, endKey, true);
         *
         * log.debug("Iterating over volume query results.");
         *
         * boolean stopped = false;
         *
         * try { try { try { for (StatisticalInterval second : cursor) {
         * startKey = second.key;
         *
         * if (second.key.getResolution().longValue() != resolution.longValue())
         * { log.debug("skipping resolution: " + second.key.getResolution());
         * continue; }
         *
         * if (++flowsProcessed % 500 == 0) { log.debug("StatisticalSeconds
         * processed: " + flowsProcessed); stopped = true; break; }
         *
         * stopped = false;
         *
         * HashMap<Integer, Long> versions = new HashMap(); HashMap<Integer,
         * Long> types = new HashMap(); HashMap<Integer, Long> tcp = new
         * HashMap(); HashMap<Integer, Long> udp = new HashMap();
         *
         * for (StatisticalFlow entry : second.getFlows().values()) { try { for
         * (Entry<StatisticalFlowDetail, Long> volume :
         * entry.getCount().entrySet()) { if
         * (volume.getKey().getType().equals(StatisticalFlowDetail.Count.BYTE))
         * {
         *
         * if (types.containsKey(volume.getKey().getProtocol())) {
         * types.put(volume.getKey().getProtocol(),
         * types.get(volume.getKey().getProtocol()) + volume.getValue()); } else
         * { types.put(volume.getKey().getProtocol(), volume.getValue()); }
         *
         * if (volume.getKey().getProtocol() == 6) { if
         * (tcp.containsKey(volume.getKey().getDestination())) {
         * tcp.put(volume.getKey().getDestination(),
         * tcp.get(volume.getKey().getDestination()) + volume.getValue()); }
         * else { tcp.put(volume.getKey().getDestination(), volume.getValue());
         * } }
         *
         * if (volume.getKey().getProtocol() == 17) { if
         * (udp.containsKey(volume.getKey().getDestination())) {
         * udp.put(volume.getKey().getDestination(),
         * udp.get(volume.getKey().getDestination()) + volume.getValue()); }
         * else { udp.put(volume.getKey().getDestination(), volume.getValue());
         * } }
         *
         * if
         * (volume.getKey().getVersion().equals(StatisticalFlowDetail.Version.IPV4))
         * { if (versions.containsKey(4)) { versions.put(4, versions.get(4) +
         * volume.getValue()); } else { versions.put(4, volume.getValue()); } }
         * else if
         * (volume.getKey().getVersion().equals(StatisticalFlowDetail.Version.IPV6))
         * { if (versions.containsKey(6)) { versions.put(6, versions.get(6) +
         * volume.getValue()); } else { versions.put(6, volume.getValue()); } }
         * }
         *
         * }
         * } catch (NullPointerException e) { log.error("Unexpected NULL field
         * encountered. This is probably a result of bad data in the database
         * (perhaps an interrupted insert?)"); continue; } }
         *
         * // Iterate over the bins in consolidated for (Date bin :
         * consolidated.keySet()) { if (Thread.currentThread().isInterrupted())
         * { throw new InterruptedException(); }
         *
         * if (new Date(second.getKey().getStatisticalInterval() *
         * resolution).after(new Date(bin.getTime())) && new
         * Date(second.getKey().getStatisticalInterval() *
         * resolution).before(new Date(bin.getTime() + interval))) { if
         * (!types.isEmpty()) { for (Integer type : types.keySet()) { if
         * (consolidated.get(bin).get("types").containsKey(type)) {
         * consolidated.get(bin).get("types").put(type,
         * consolidated.get(bin).get("types").get(type) + types.get(type)); }
         * else { consolidated.get(bin).get("types").put(type, types.get(type));
         * } } }
         *
         * if (!tcp.isEmpty()) { for (Integer destination : tcp.keySet()) { if
         * (consolidated.get(bin).get("tcp").containsKey(destination)) {
         * consolidated.get(bin).get("tcp").put(destination,
         * consolidated.get(bin).get("tcp").get(destination) +
         * tcp.get(destination)); } else {
         * consolidated.get(bin).get("tcp").put(destination,
         * tcp.get(destination)); } } }
         *
         * if (!udp.isEmpty()) { for (Integer destination : udp.keySet()) { if
         * (consolidated.get(bin).get("udp").containsKey(destination)) {
         * consolidated.get(bin).get("udp").put(destination,
         * consolidated.get(bin).get("udp").get(destination) +
         * udp.get(destination)); } else {
         * consolidated.get(bin).get("udp").put(destination,
         * udp.get(destination)); } } }
         *
         * if (!versions.isEmpty()) { for (Integer version : versions.keySet())
         * { if (consolidated.get(bin).get("versions").containsKey(version)) {
         * consolidated.get(bin).get("versions").put(version,
         * consolidated.get(bin).get("versions").get(version) +
         * versions.get(version)); } else {
         * consolidated.get(bin).get("versions").put(version,
         * versions.get(version)); } } } } }
         *
         * if (Thread.currentThread().isInterrupted()) {
         * log.debug("StatisticsManager was interrupted"); throw new
         * InterruptedException(); } }
         *
         * if (!stopped) { endKey = startKey; } } catch (DatabaseException e) {
         * log.error("Database error: " + e.getMessage()); } finally {
         * cursor.close(); } } catch (DatabaseException e) { log.error("Database
         * error: " + e.getMessage()); } finally { closeStore(store); } } catch
         * (InterruptedException ie) { log.error("Stopping StatisticsManager
         * during Volume build..."); } finally { closeEnvironment(environment);
         * } System.gc(); }
         *
         * log.debug("Returning from FlowManager:getVolumeByTime"); return
         * consolidated;
         *
         */
        return null;
    }

    public StatisticalFlow setDefault(ArrayList<Network> networks, StatisticalFlow flow) throws UnknownHostException {
        ArrayList<InetNetwork> iNetworks = new ArrayList();
        for (Network network : networks) {
            iNetworks.add(network.getNetwork());
        }

        return setDefault(iNetworks, flow, "0.0.0.0");
    }

    public StatisticalFlow setDefault(ArrayList<InetNetwork> networks, StatisticalFlow flow, String untracked) throws UnknownHostException {
        Boolean sourceManaged = false;
        Boolean destinationManaged = false;

        for (InetNetwork network : networks) {
            if (!sourceManaged && AddressAnalysis.isMember(InetAddress.getByName(flow.getSource()), network)) {
                sourceManaged = true;
            }
            if (!destinationManaged && AddressAnalysis.isMember(InetAddress.getByName(flow.getDestination()), network)) {
                destinationManaged = true;
            }
            if (sourceManaged && destinationManaged) {
                break;
            }
        }

        if (!sourceManaged) {
            flow.setSource(untracked);
        }

        if (!destinationManaged) {
            flow.setDestination(untracked);
        }

        return flow;
    }

    public MapDataResponse getMapData(HttpSession session, Constraints constraints) throws InterruptedException {
        /*
         * LinkedHashMap<String, InetNetwork> managedNetworks = new
         * ManagedNetworks(customer).getNetworks();
         *
         * String untracked = "untracked"; MapDataResponse response = new
         * MapDataResponse(untracked); ArrayList<InetNetwork> networks = new
         * ArrayList();
         *
         * for (Entry<String, InetNetwork> entry : managedNetworks.entrySet()) {
         * String version = null; if (entry.getValue().getAddress() instanceof
         * Inet4Address) { version = "ipv4"; } else if
         * (entry.getValue().getAddress() instanceof Inet6Address) { version =
         * "ipv6"; } response.networks.add(new
         * MapDataResponse.Network(entry.getKey(), version,
         * entry.getValue().getAddress().getHostAddress() + "/" +
         * entry.getValue().getMask())); networks.add(entry.getValue()); }
         *
         * long duration = constraints.endTime.getTime() -
         * constraints.startTime.getTime(); long resolution =
         * getResolution(duration, null); Environment environment; EntityStore
         * readOnlyEntityStore = new EntityStore(environment =
         * setupEnvironment(), "Statistics", this.getStoreConfig(true));
         * StatisticsAccessor accessor = new
         * StatisticsAccessor(readOnlyEntityStore); IntervalKey startKey = new
         * IntervalKey(constraints.startTime.getTime() / resolution,
         * resolution); IntervalKey endKey = new
         * IntervalKey(constraints.endTime.getTime() / resolution, resolution);
         * EntityCursor<StatisticalInterval> cursor =
         * accessor.intervalByKey.entities(startKey, true, endKey, true);
         *
         * // We only want to do this query once, so make it count.
         *
         * log.debug("Iterating over query results.");
         *
         * Integer secondsProcessed = 0; try { for (StatisticalInterval second :
         * cursor) { for (StatisticalFlow flow : second.getFlows().values()) {
         * try { flow = setDefault(networks, flow, untracked); } catch
         * (UnknownHostException e) { log.error("UnknownHostException while
         * attempting to setDefault: " + e.getMessage()); }
         *
         * if (flow.getSource() != null) { try { Boolean sourceCaptured = false;
         * Boolean destinationCaptured = false;
         *
         * if (flow.getSource().equals(untracked)) { sourceCaptured = true; }
         *
         * if (flow.getDestination().equals(untracked)) { destinationCaptured =
         * true; }
         *
         * InetAddress sourceAddress = null; InetAddress destinationAddress =
         * null;
         *
         * if (!sourceCaptured) { sourceAddress =
         * InetAddress.getByName(flow.getSource()); }
         *
         * if (!destinationCaptured) { destinationAddress =
         * InetAddress.getByName(flow.getDestination()); }
         *
         *
         * // Iterate through managed networks and add the Nodes (complete with
         * Flows) // as they are encountered for (MapDataResponse.Network
         * network : response.networks) { InetNetwork iNetwork = new
         * InetNetwork();
         * iNetwork.setAddress(InetAddress.getByName(network.address.split("/")[0]));
         * iNetwork.setMask(Integer.parseInt(network.address.split("/")[1])); if
         * (!sourceCaptured || !destinationCaptured) { if (!sourceCaptured) { if
         * (AddressAnalysis.isMember(sourceAddress, iNetwork)) { if
         * (!resolver.containsKey(sourceAddress.getHostAddress())) {
         * resolver.put(sourceAddress.getHostAddress(), new
         * NameResolution(sourceAddress.getHostName())); }
         *
         * network.nodes.add(new
         * MapDataResponse.Node(sourceAddress.getHostAddress(),
         * resolver.get(sourceAddress.getHostAddress()).name)); sourceCaptured =
         * true; } }
         *
         * if (!destinationCaptured) { if
         * (AddressAnalysis.isMember(destinationAddress, iNetwork)) { if
         * (!resolver.containsKey(destinationAddress.getHostAddress())) {
         * resolver.put(destinationAddress.getHostAddress(), new
         * NameResolution(destinationAddress.getHostName())); }
         *
         * network.nodes.add(new
         * MapDataResponse.Node(destinationAddress.getHostAddress(),
         * resolver.get(destinationAddress.getHostAddress()).name));
         * destinationCaptured = true; } }
         *
         * if (Thread.currentThread().isInterrupted()) { throw new
         * InterruptedException(); } } } } catch (UnknownHostException e) {
         * log.error("UnknownHostException while setting networks: " +
         * e.getMessage()); }
         *
         *
         * for (Entry<StatisticalFlowDetail, Long> entry :
         * flow.getCount().entrySet()) { StatisticalFlowDetail detail =
         * entry.getKey(); if (detail.getType() ==
         * StatisticalFlowDetail.Count.BYTE) { Boolean reversed = false; if
         * (detail.getSource() > 0 && detail.getDestination() > 0) { if
         * (globalConfigurationManager.getFrequency(customer,
         * detail.getProtocol(), detail.getSource()) >
         * globalConfigurationManager.getFrequency(customer,
         * detail.getProtocol(), detail.getDestination())) { reversed = true; }
         * }
         *
         * MapDataResponse.Flow responseFlow = null; if (!reversed) {
         * responseFlow = new MapDataResponse.Flow(flow.getSource(),
         * flow.getDestination(), String.valueOf(detail.getProtocol()),
         * String.valueOf(detail.getDestination()),
         * String.valueOf(entry.getValue()), "0"); } else { responseFlow = new
         * MapDataResponse.Flow(flow.getDestination(), flow.getSource(),
         * String.valueOf(detail.getProtocol()),
         * String.valueOf(detail.getSource()), "0",
         * String.valueOf(entry.getValue())); }
         *
         * response.addFlow(responseFlow); } } }
         *
         * if (Thread.currentThread().isInterrupted()) { //log.debug("Stopping
         * FlowManager..."); throw new InterruptedException(); } }
         *
         * if (++secondsProcessed % 10000 == 0) { log.debug(secondsProcessed + "
         * StatisticalSeconds processed."); } } } catch (DatabaseException e) {
         * log.error("Database error: " + e.getMessage()); } finally {
         * cursor.close(); }
         *
         * response.ready = true;
         *
         * return response;
         *
         */
        return null;
    }

    public ArrayList<Network> getNetworks(HttpSession session, Constraints constraints) {
        /*
         * LinkedHashMap<String, InetNetwork> managedNetworks = new
         * ManagedNetworks(customer).getNetworks();
         *
         * ArrayList<Network> networks = new ArrayList<Network>();
         *
         * for (InetNetwork network : managedNetworks.values()) {
         * networks.add(new Network(network)); }
         *
         * Integer g = 0, n = 0;
         *
         * Network defaultNetwork = null;
         *
         * try { defaultNetwork = new Network(InetAddress.getByName("0.0.0.0"),
         * 0, "DEFAULT"); } catch (UnknownHostException uhe) { log.error("Could
         * not parse network for DEFAULT: " + uhe.getMessage()); }
         *
         * DefaultNode defaultNode = new DefaultNode();
         *
         * long duration = constraints.endTime.getTime() -
         * constraints.startTime.getTime(); long resolution =
         * getResolution(duration, null); Environment environment; EntityStore
         * readOnlyEntityStore = new EntityStore(environment =
         * setupEnvironment(), "Statistics", this.getStoreConfig(true));
         * StatisticsAccessor accessor = new
         * StatisticsAccessor(readOnlyEntityStore); IntervalKey startKey = new
         * IntervalKey(constraints.startTime.getTime() / resolution,
         * resolution); IntervalKey endKey = new
         * IntervalKey(constraints.endTime.getTime() / resolution, resolution);
         * EntityCursor<StatisticalInterval> cursor =
         * accessor.intervalByKey.entities(startKey, true, endKey, true);
         *
         * // We only want to do this query once, so make it count.
         *
         * log.debug("Iterating over query results.");
         *
         * Integer secondsProcessed = 0; try { try { try {
         *
         * for (StatisticalInterval second : cursor) { for (StatisticalFlow flow
         * : second.getFlows().values()) { flow = setDefault(networks, flow);
         *
         * if (flow.getSource() == null) { n++; } else { InetAddress
         * sourceAddress = InetAddress.getByName(flow.getSource()); InetAddress
         * destinationAddress = InetAddress.getByName(flow.getDestination());
         * Node sourceNode = new Node(flow.getSource()); Node destinationNode =
         * new Node(flow.getDestination());
         *
         * //log.debug("."); // Associate the flow with the source (flows are
         * never associated with the destination) sourceNode.addFlow(customer,
         * flow); // On that last point, just kidding
         * destinationNode.addFlow(customer, flow);
         *
         * Boolean sourceCaptured = false; Boolean destinationCaptured = false;
         * // Iterate through managed networks and add the Nodes (complete with
         * Flows) // as they are encountered for (Network network : networks) {
         * if (!sourceCaptured || !destinationCaptured) { // If the source
         * address belongs to a managed network, add it to the map //
         * De-duplication is handled by the "addNode" method in the Network
         * object if (AddressAnalysis.isMember(sourceAddress,
         * network.getNetwork())) { network.addNode(sourceNode); sourceCaptured
         * = true; }
         *
         * if (AddressAnalysis.isMember(destinationAddress,
         * network.getNetwork())) { network.addNode(destinationNode);
         * destinationCaptured = true; }
         *
         * if (Thread.currentThread().isInterrupted()) { throw new
         * InterruptedException(); } } }
         *
         * if (!sourceCaptured) { if (!flow.getDestination().equals("0.0.0.0"))
         * { defaultNode.addFlow(customer, flow); } } else if
         * (!destinationCaptured) { defaultNode.addFlow(customer, flow); }
         *
         * // If the flow was not captured, add it to the default node //if
         * (!sourceCaptured || !destinationCaptured) { //
         * defaultNode.addFlow(flow); //} }
         *
         * if (Thread.currentThread().isInterrupted()) { //log.debug("Stopping
         * FlowManager..."); throw new InterruptedException(); } }
         *
         * if (++secondsProcessed % 10000 == 0) { log.debug(secondsProcessed + "
         * StatisticalSeconds processed."); } } } catch (DatabaseException e) {
         * log.error("Database error: " + e.getMessage()); } finally {
         * cursor.close(); } } catch (DatabaseException e) { log.error("Database
         * error: " + e.getMessage()); } finally {
         * closeStore(readOnlyEntityStore); } } catch (UnknownHostException e) {
         * log.error("UnknownHostException caught in " +
         * e.getStackTrace()[0].getMethodName() + ": " + e.getMessage()); }
         * catch (InterruptedException ie) { log.error("Stopped FlowManager
         * during network build"); } catch (Exception e) { log.error("Exception
         * caught in " + e.getStackTrace()[0].getMethodName() + ": " +
         * e.getMessage()); } finally { closeEnvironment(environment); }
         *
         * log.debug(secondsProcessed + " total StatisticalSeconds analyzed.");
         * log.debug("Found " + n + " null flows."); log.debug("Found " + g + "
         * non-IP flows."); defaultNetwork.addNode(defaultNode);
         *
         * // Add the default network to the bottom of the network list
         * networks.add(defaultNetwork);
         *
         * //createTraceFile(networks);
         *
         * return networks;
         *
         */
        return null;
    }

    private HashMap<IntervalKey, Boolean> identifyExpiredIntervals() {
        /*
         * log.debug("Identifying expired intervals...");
         *
         * Environment environment; EntityStore entityStore = new
         * EntityStore(environment = setupEnvironment(), "Statistics",
         * this.getStoreConfig(true)); StatisticsAccessor dataAccessor = new
         * StatisticsAccessor(entityStore);
         *
         * Date now = new Date(); Date start = new Date(); start.setTime(0l);
         *
         * HashMap<IntervalKey, Boolean> expiredIntervals = new HashMap();
         *
         * for (Entry<Long, Boolean> resolution :
         * globalConfigurationManager.getResolutionMap().entrySet()) {
         * IntervalKey startKey = new IntervalKey();
         * startKey.setResolution(resolution.getKey());
         * startKey.setStatisticalInterval(start.getTime() /
         * resolution.getKey());
         *
         * IntervalKey endKey = new IntervalKey();
         * endKey.setResolution(resolution.getKey());
         * endKey.setStatisticalInterval((now.getTime() / resolution.getKey()) -
         * 100000); // The above line keeps 100000 of any resolution around. At
         * it's most fine (10000 ms), this is about a week and a half. // At
         * it's most coarse (10000000 ms), this is about 30 years.
         *
         * EntityCursor<StatisticalInterval> cursor =
         * dataAccessor.intervalByKey.entities(startKey, true, endKey, true);
         *
         * Integer intervalsDeleted = 0;
         *
         * for (StatisticalInterval statisticalInterval : cursor) {
         * expiredIntervals.put(statisticalInterval.key, resolution.getValue());
         *
         * if (++intervalsDeleted % 1000 == 0) { log.debug(intervalsDeleted + "
         * intervals marked for deletion at a resolution of " + resolution + "
         * milliseconds..."); } }
         *
         * log.debug(intervalsDeleted + " total seconds marked for deletion at a
         * resolution of " + resolution + " milliseconds..."); cursor.close(); }
         *
         * closeStore(entityStore); closeEnvironment(environment);
         *
         * return expiredIntervals;
         *
         */
        return null;
    }

    class NameResolution {

        public String name;
        public Date created = new Date();

        public NameResolution(String name) {
            this.name = name;
        }
    }
}
