/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.log4j.Logger;

/**
 *
 * @author justin
 */
public class StatisticalEngine {

    private static final Logger log = Logger.getLogger(StatisticalEngine.class.getName());
    private final Integer HISTORY = 1000;
    private Map<String, Map<String, DescriptiveStatistics>> statistics = new ConcurrentHashMap();

    public StatisticalInterval addStatisticalInterval(Customer customer, StatisticalInterval interval) {
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

                if (size != 0) {
                    normalized.get(source).put(destination, size);
                }
            }

            process(normalized);
            this.add(normalized, interval.key.interval);
        }

        return interval;
    }

    private void process(Map<String, Map<String, Long>> normalized) {
        for (String source : normalized.keySet()) {
            for (String destination : normalized.get(source).keySet()) {

                DescriptiveStatistics stats = statistics.get(source).get(destination);
                if (stats.getValues().length >= 2) {
                    log.debug("Data for: " + source + " -> " + destination);
                    double mu = stats.getMean();
                    double[] sValues = statistics.get(source).get(destination).getValues();
                    StringBuilder builder = new StringBuilder();
                    for (double value : sValues) {
                        builder.append(value);
                        builder.append(", ");
                    }
                    log.debug("\tvalues: " + builder.toString());
                    log.debug(stats.toString());
                }
            }
        }
    }

    private void add(Map<String, Map<String, Long>> normalized, Long interval) {
        for (String source : normalized.keySet()) {
            for (String destination : normalized.get(source).keySet()) {
                Long size = normalized.get(source).get(destination);
                statistics.get(source).get(destination).addValue(new Double(size).doubleValue());
            }
        }
    }

    private void addFile(String source, String destination) {

        if (!statistics.containsKey(source)) {
            statistics.put(source, new ConcurrentHashMap());
        }

        if (!statistics.get(source).containsKey(destination)) {
            DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics();
            descriptiveStatistics.setWindowSize(HISTORY);
            statistics.get(source).put(destination, descriptiveStatistics);
        }
    }
}
