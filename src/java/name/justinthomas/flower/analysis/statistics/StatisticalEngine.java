/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import name.justinthomas.flower.analysis.element.ManagedNetworks;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.commons.math.stat.inference.TTestImpl;
import org.apache.commons.math.stat.inference.TestUtils;
import org.apache.log4j.Logger;

/**
 *
 * @author justin
 */
public class StatisticalEngine {

    private static final Logger log = Logger.getLogger(StatisticalEngine.class.getName());
    private Map<String, Map<String, Map<Long, Long>>> cube = new ConcurrentHashMap();
    private Map<String, Map<String, DescriptiveStatistics>> statistics = new ConcurrentHashMap();

    public StatisticalInterval addStatisticalInterval(Customer customer, StatisticalInterval interval) {
        if ((interval != null) && (interval.key != null) && (interval.key.resolution == 100000) && (interval.flows != null)) {
            ManagedNetworks managedNetworks = new ManagedNetworks(customer);

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

                if (!statistics.containsKey(source) || !statistics.get(source).containsKey(destination)) {
                    this.addFile(source, destination);
                }

                Long size = null;
                for (StatisticalFlowDetail detail : interval.flows.get(flow).count.keySet()) {
                    if (detail.type == StatisticalFlowDetail.Count.BYTE) {
                        size = interval.flows.get(flow).count.get(detail);
                    }
                }

                List<Double> doubleObjs = new LinkedList();
                for (Long value : cube.get(source).get(destination).values()) {
                    doubleObjs.add(new Double(value));
                }

                double[] doubles = new double[doubleObjs.size()];

                int i = 0;
                for (Double d : doubleObjs) {
                    doubles[i++] = d.doubleValue();
                }

                if (size != 0) {
                    DescriptiveStatistics stats = statistics.get(source).get(destination);
                    if (stats.getValues().length >= 2 && doubles.length >= 2) {
                        log.debug("Data for: " + source + " -> " + destination);
                        double mu = stats.getMean();
                        log.debug("\tmu: " + mu + " " + statistics.size() + "x" + statistics.get(source).size() + "x" + statistics.get(source).get(destination).getValues().length);
                        TTestImpl ttest = new TTestImpl();
                        try {
                            log.debug("\tmanual tTest: " + ttest.tTest(mu, statistics.get(source).get(destination), 0.25) + ", " + ttest.tTest(mu, statistics.get(source).get(destination)));
                            log.debug("\tstatic tTest: " + TestUtils.tTest(mu, stats, 0.25));
                            log.debug("\tmanual tTest on Cube: " + ttest.tTest(mu, doubles, 0.25) + ", " + ttest.tTest(mu, doubles));
                            log.debug("\tstatic tTest on Cube: " + TestUtils.tTest(mu, doubles));
                        } catch (MathException e) {
                            log.warn(e.getMessage());
                        }
                    }

                    statistics.get(source).get(destination).addValue(new Double(size).doubleValue());
                    cube.get(source).get(destination).put(interval.key.interval, size);
                }
            }
        }

        return interval;
    }

    private Integer addFile(String source, String destination) {

        List<Long> intervals = new ArrayList();

        for (String existingSource : cube.keySet()) {
            for (String existingDestination : cube.get(existingSource).keySet()) {
                for (Long interval : cube.get(existingSource).get(existingDestination).keySet()) {
                    intervals.add(interval);
                }
            }
        }

        if (!cube.containsKey(source)) {
            cube.put(source, new ConcurrentHashMap());
            statistics.put(source, new ConcurrentHashMap());
        }

        if (!cube.get(source).containsKey(destination)) {
            cube.get(source).put(destination, new ConcurrentHashMap());
            statistics.get(source).put(destination, new SynchronizedDescriptiveStatistics());
        }

        for (Long interval : intervals) {
            cube.get(source).get(destination).put(interval, 0l);
            statistics.get(source).get(destination).addValue(0);
        }

        return intervals.size();

    }
}
