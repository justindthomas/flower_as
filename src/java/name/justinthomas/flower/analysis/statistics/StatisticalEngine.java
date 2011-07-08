/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
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
    private final Integer HISTORY = 1000;
    private Customer customer;
    private Map<String, Map<String, Map<Cube, DescriptiveStatistics>>> statistics = new ConcurrentHashMap();

    public static enum Anomaly {

        EWMA_INCREASE,
        EWMA_DECREASE,
        NEW_MAXIMUM,
        NEW_MINIMUM,
        NEW_FLOW
    }

    private enum Cube {

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
                /*
                DescriptiveStatistics detail = statistics.get(source).get(destination).get(Cube.DETAIL);
                if (detail.getValues().length >= 2) {
                log.debug("Detail data for: " + source + " -> " + destination);
                double[] sValues = detail.getValues();
                StringBuilder builder = new StringBuilder();
                for (double value : sValues) {
                builder.append(new Double(value).intValue());
                builder.append(", ");
                }
                log.debug("\tvalues: " + builder.toString() + "\n");
                log.debug(detail.toString());
                }
                 * 
                 */

                /*
                DescriptiveStatistics mean = statistics.get(source).get(destination).get(Cube.MEAN);
                if (mean.getValues().length >= 2) {
                log.debug("Mean data for: " + source + " -> " + destination);
                double[] sValues = mean.getValues();
                StringBuilder builder = new StringBuilder();
                for (double value : sValues) {
                builder.append(new Double(value).intValue());
                builder.append(", ");
                }
                log.debug("\tvalues: " + builder.toString());
                log.debug(mean.toString());
                }
                 * 
                 */

                DescriptiveStatistics ewma = statistics.get(source).get(destination).get(Cube.EW_MEAN);
                if (ewma.getValues().length >= 2) {
                    log.debug("EWMA data for: " + source + " -> " + destination);

                    log.debug("Comparing current EWMA: "
                            + prior.get(source).get(destination).getMean()
                            + " to updated: "
                            + ewma.getMax() + "/" + ewma.getMean() + "/" + ewma.getMin());

                    double[] sValues = ewma.getValues();
                    StringBuilder builder = new StringBuilder();
                    for (double value : sValues) {
                        builder.append(new Double(value).intValue());
                        builder.append(", ");
                    }
                    log.debug("\tvalues: " + builder.toString() + "\n");
                    //log.debug(ewma.toString());

                    StatisticalFlowIdentifier id = new StatisticalFlowIdentifier(source, destination);
                    if (prior.get(source).get(destination).getMean() > ewma.getMax()) {
                        log.debug("EWMA increase");
                        interval.addAnomaly(id, Anomaly.EWMA_INCREASE);
                    } else if (prior.get(source).get(destination).getMean() < ewma.getMin()) {
                        log.debug("EWMA decrease");
                        interval.addAnomaly(id, Anomaly.EWMA_DECREASE);
                    }
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
