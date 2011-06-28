/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.statistics;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.commons.math.MathException;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;
import org.apache.commons.math.stat.inference.TestUtils;

/**
 *
 * @author justin
 */

public class StatisticalEngine {
    // <source, <destination, <interval, sizes>>>

    private Map<String, Map<String, Map<Long, Long>>> cube = new ConcurrentHashMap();
    private Map<String, Map<String, DescriptiveStatistics>> statistics = new ConcurrentHashMap();

    public StatisticalInterval addStatisticalInterval(StatisticalInterval interval) {
        for (StatisticalFlowIdentifier flow : interval.flows.keySet()) {
            if (!cube.containsKey(flow.source) || !cube.get(flow.source).containsKey(flow.destination)) {
                this.addFile(flow.source, flow.destination);
            }

            Long size = null;
            for (StatisticalFlowDetail detail : interval.flows.get(flow).count.keySet()) {
                if (detail.type == StatisticalFlowDetail.Count.BYTE) {
                    size = interval.flows.get(flow).count.get(detail);
                }
            }

            List<Double> doubleObjs = new LinkedList();
            for(Long value : cube.get(flow.source).get(flow.destination).values()) {
                doubleObjs.add(new Double(value));
            }
            
            double[] doubles = new double[doubleObjs.size()];

            System.arraycopy(doubleObjs.toArray(), 0, doubles, 0, doubleObjs.size());
            
            try {
                System.out.println("t-test for " + flow.source + " to " + flow.destination + ": " + TestUtils.tTest(size, doubles, 0.25));
            } catch (MathException e) {
                e.printStackTrace();
            }
            
            statistics.get(flow.source).get(flow.destination).addValue(size);
            cube.get(flow.source).get(flow.destination).put(interval.key.interval, size);    
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
