package name.justinthomas.flower.analysis.statistics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
import name.justinthomas.flower.analysis.statistics.AnomalyEvent.Anomaly;
import name.justinthomas.flower.global.GlobalConfigurationManager;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.SimpleLayout;

/**
 *
 * @author justin
 */
public class StatisticalEngine {

    private static final Logger log = Logger.getLogger(StatisticalEngine.class.getName());
    private static GlobalConfigurationManager globalConfigurationManager;
    private static FileAppender fileAppender;
    private static ScheduledThreadPoolExecutor executor;
    private final Integer HISTORY = 1000;
    private Customer customer;
    private ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> statistics;

    //private Map<String, Map<String, Map<Cube, DescriptiveStatistics>>> statistics = new ConcurrentHashMap();
    protected enum Cube {

        DETAIL,
        MEAN,
        EW_MEAN
    }

    public StatisticalEngine(Customer customer) {
        this.customer = customer;

        if (globalConfigurationManager == null) {
            try {
                globalConfigurationManager = (GlobalConfigurationManager) InitialContext.doLookup("java:global/Analysis/GlobalConfigurationManager");
            } catch (NamingException e) {
                log.error(e.getMessage());
            }
        }

        if (fileAppender == null) {
            try {
                fileAppender = new FileAppender(new SimpleLayout(), globalConfigurationManager.getBaseDirectory() + "/statistics.log");
                log.addAppender(fileAppender);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }

        StatisticsManager statisticsManager = new StatisticsManager(customer);
        StatisticalCube storedCube = statisticsManager.getCube();

        if (storedCube != null) {
            statistics = storedCube.getConcurrentStatistics();
        } else {
            statistics = new ConcurrentHashMap();
        }

        if (executor == null) {
            executor = new ScheduledThreadPoolExecutor(3);
        }

        executor.scheduleAtFixedRate(new Task(customer, statistics), 5, 5, TimeUnit.MINUTES);
    }

    class Task implements Runnable {

        private ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> statistics;
        private Customer customer;

        public Task(Customer customer, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> statistics) {
            this.statistics = statistics;
            this.customer = customer;
        }

        @Override
        public void run() {
            try {
                StatisticalCube cube = new StatisticalCube(customer.getId(), statistics);
                StatisticsManager statisticsManager = new StatisticsManager(customer);
                statisticsManager.storeCube(cube);
            } catch (Throwable t) {
                log.error("Scheduled Task Failed: " + t.toString());
                for(StackTraceElement element : t.getStackTrace()) {
                    log.error(element.getClassName() + ", line: " + element.getLineNumber());
                }
            }
        }
    }

    public StatisticalInterval addStatisticalInterval(StatisticalInterval interval) {
        if ((interval != null) && (interval.key != null) && (interval.key.resolution == 1000000) && (interval.flows != null)) {
            ManagedNetworks managedNetworks = new ManagedNetworks(customer);

            Map<String, Map<String, Long>> normalized = new HashMap();

            for (StatisticalFlowIdentifier flow : interval.flows.keySet()) {
                String source = flow.source;
                String destination = flow.destination;

                try {
                    InetAddress sourceAddress = InetAddress.getByName(flow.source);
                    InetAddress destinationAddress = InetAddress.getByName(flow.destination);

                    if (!managedNetworks.isManaged(sourceAddress)) {
                        if (sourceAddress instanceof Inet4Address) {
                            source = "untracked-v4";
                        } else if (sourceAddress instanceof Inet6Address) {
                            source = "untracked-v6";
                        }
                    }

                    if (!managedNetworks.isManaged(destinationAddress)) {
                        if (destinationAddress instanceof Inet4Address) {
                            destination = "untracked-v4";
                        } else if (destinationAddress instanceof Inet6Address) {
                            destination = "untracked-v6";
                        }
                    }
                } catch (UnknownHostException e) {
                    log.warn(e.getMessage());
                }

                if (!normalized.containsKey(source)) {
                    normalized.put(source, new HashMap());
                }

                if (!normalized.get(source).containsKey(destination)) {
                    normalized.get(source).put(destination, 0l);
                }

                if (!statistics.containsKey(source) || !statistics.get(source).containsKey(destination)) {
                    StatisticalFlowIdentifier id = new StatisticalFlowIdentifier(source, destination);
                    interval.addAnomaly(id, Anomaly.NEW_FLOW);
                    this.addFile(source, destination);
                }

                Long size = 0l;
                for (StatisticalFlowDetail detail : interval.flows.get(flow).count.keySet()) {
                    if (detail.type == StatisticalFlowDetail.Count.BYTE) {
                        size += interval.flows.get(flow).count.get(detail);
                    }
                }

                normalized.get(source).put(destination, size);
            }

            return this.process(normalized, interval);
        }

        return interval;
    }

    /**
     * Performs statistical analysis of the data contained in the new interval
     * as compared to the historical data in the statistics cube. This section
     * is a work in progress.
     * 
     * @param normalized  the normalized interval
     */
    private StatisticalInterval process(Map<String, Map<String, Long>> normalized, StatisticalInterval interval) {
        Map<String, Map<String, DescriptiveStatistics>> prior = new HashMap();
        for (String source : statistics.keySet()) {
            if (!prior.containsKey(source)) {
                prior.put(source, new HashMap());
            }

            for (String destination : statistics.get(source).keySet()) {
                prior.get(source).put(destination, new DescriptiveStatistics(statistics.get(source).get(destination).get(Cube.EW_MEAN)));
            }
        }

        this.add(normalized);

        for (String source : normalized.keySet()) {
            for (String destination : normalized.get(source).keySet()) {
                DescriptiveStatistics ewma = statistics.get(source).get(destination).get(Cube.EW_MEAN);
                if (ewma.getValues().length >= 2) {
                    log.debug("EWMA data for: " + source + " -> " + destination);

                    log.debug("Comparing current EWMA: "
                            + prior.get(source).get(destination).getValues()[prior.get(source).get(destination).getValues().length - 1]
                            + " to updated: "
                            + ewma.getMax() + "/" + ewma.getValues()[ewma.getValues().length - 1] + "/" + ewma.getMin());

                    double[] sValues = ewma.getValues();
                    StringBuilder builder = new StringBuilder();
                    for (double value : sValues) {
                        builder.append(new Double(value).intValue());
                        builder.append(", ");
                    }
                    log.debug("values: " + builder.toString());
                    //log.debug(ewma.toString());

                    StatisticalFlowIdentifier id = new StatisticalFlowIdentifier(source, destination);
                    if (ewma.getValues()[ewma.getValues().length - 1] > prior.get(source).get(destination).getMax()) {
                        log.debug("EWMA increase");
                        interval.addAnomaly(id, Anomaly.EWMA_INCREASE);
                    } else if (ewma.getValues()[ewma.getValues().length - 1] < prior.get(source).get(destination).getMin()) {
                        log.debug("EWMA decrease");
                        interval.addAnomaly(id, Anomaly.EWMA_DECREASE);
                    }
                    log.debug("\n");
                }
            }
        }

        return interval;
    }

    /**
     * Loops through the statistics cube, updating entries with data in the 
     * normalized table. Assumes that empty entries exist as a function of the 
     * addFile(source, destination) method being run previously. Adds 0-length
     * entries for pairings not represented in the normalized table.
     * 
     * @param normalized  the normalized interval
     */
    private void add(Map<String, Map<String, Long>> normalized) {
        for (String source : statistics.keySet()) {
            for (String destination : statistics.get(source).keySet()) {
                Long size = 0l;
                if (normalized.containsKey(source) && normalized.get(source).containsKey(destination)) {
                    size = normalized.get(source).get(destination);
                }
                DescriptiveStatistics detail = statistics.get(source).get(destination).get(Cube.DETAIL);
                detail.addValue(new Double(size).doubleValue());

                DescriptiveStatistics ewma = statistics.get(source).get(destination).get(Cube.EW_MEAN);

                double alpha = 0.1;

                if (detail.getN() > 0) {
                    Long last_detail = detail.getN() - 1;
                    double yt_neg1 = detail.getValues()[last_detail.intValue()];
                    if (ewma.getN() == 0) {
                        ewma.addValue(yt_neg1);
                    } else {
                        Long last_ewma = ewma.getN() - 1;
                        double st_neg1 = ewma.getValues()[last_ewma.intValue()];
                        ewma.addValue((alpha * yt_neg1) + ((1 - alpha) * st_neg1));
                    }
                } else {
                    if (ewma.getN() == 0) {
                        ewma.addValue(new Double(size).doubleValue());
                    }
                }

                statistics.get(source).get(destination).get(Cube.MEAN).addValue(detail.getMean());
            }
        }
    }

    /**
     * Creates an entry in the statistics cube for a new source and destination.
     * Initializes a new SynchronizedDescriptiveStatistics instance for the new
     * source/destination pairing.
     * 
     * @param source  the source address string (e.g., 192.168.1.1 or fe80::1)
     * @param destination  the destination address string
     */
    private void addFile(String source, String destination) {
        if (!statistics.containsKey(source)) {
            statistics.put(source, new ConcurrentHashMap());
        }

        if (!statistics.get(source).containsKey(destination)) {
            statistics.get(source).put(destination, new ConcurrentHashMap());
        }

        if (!statistics.get(source).get(destination).containsKey(Cube.DETAIL)) {
            DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics();
            descriptiveStatistics.setWindowSize(HISTORY);
            statistics.get(source).get(destination).put(Cube.DETAIL, descriptiveStatistics);
        }

        if (!statistics.get(source).get(destination).containsKey(Cube.MEAN)) {
            DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics();
            descriptiveStatistics.setWindowSize(HISTORY);
            statistics.get(source).get(destination).put(Cube.MEAN, descriptiveStatistics);
        }

        if (!statistics.get(source).get(destination).containsKey(Cube.EW_MEAN)) {
            DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics();
            descriptiveStatistics.setWindowSize(HISTORY);
            statistics.get(source).get(destination).put(Cube.EW_MEAN, descriptiveStatistics);
        }
    }
}
