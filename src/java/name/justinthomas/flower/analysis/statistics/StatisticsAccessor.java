/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.persist.EntityStore;
import com.sleepycat.persist.PrimaryIndex;

/**
 *
 * @author justin
 */
public class StatisticsAccessor {

    public StatisticsAccessor(EntityStore store) throws DatabaseException {
        intervalByKey = store.getPrimaryIndex(IntervalKey.class, StatisticalInterval.class);
        cubesByCustomer = store.getPrimaryIndex(String.class, StatisticalCube.class);
    }

    public PrimaryIndex<IntervalKey, StatisticalInterval> intervalByKey;
    public PrimaryIndex<String, StatisticalCube> cubesByCustomer;
}
