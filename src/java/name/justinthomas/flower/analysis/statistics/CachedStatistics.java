/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.statistics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;

/**
 *
 * @author justin
 */
@Singleton
@Startup
public class CachedStatistics {
    private static CachedStatistics instance;
    private static ScheduledThreadPoolExecutor executor;

    private static Map<IntervalKey, StatisticalInterval> cache = Collections.synchronizedMap(new HashMap());
    private static Map<IntervalKey, Date> lastUpdated = Collections.synchronizedMap(new HashMap());
    public static final long MAX_WAIT = 300000;

    public CachedStatistics getInstance() {
        if(instance == null) {
            instance = new CachedStatistics();
        }

        return instance;
    }

    @PostConstruct
    protected void setup() {
        System.out.println("Setting CachedStatistics Persist to run every 30 seconds.");

        instance = new CachedStatistics();
        executor = new ScheduledThreadPoolExecutor(1);
        executor.scheduleAtFixedRate(new Persist(), 60, 60, TimeUnit.SECONDS);
    }

    public static StatisticalInterval get(IntervalKey key) {
        if(cache.containsKey(key)) return cache.get(key);
        return null;
    }

    public static void put(IntervalKey key, StatisticalInterval interval) {
        lastUpdated.put(key, new Date());

        if(cache.containsKey(key)) {
            cache.put(key, cache.get(key).addInterval(interval));
        } else {
            cache.put(key, interval);
        }
    }

    class Persist implements Runnable {
        @Override
        public void run() {
            System.out.println("Statistics cache includes " + cache.size() + " entries before Persist.");
            ArrayList<IntervalKey> keys = new ArrayList();
            for(IntervalKey key : lastUpdated.keySet()) {
                if(new Date().getTime() - lastUpdated.get(key).getTime() > MAX_WAIT) {
                    keys.add(key);
                }
            }

            ArrayList<StatisticalInterval> intervals = new ArrayList();

            System.out.println("Removing intervals from cache.");
            for(IntervalKey key : keys) {
                intervals.add(cache.get(key));
                cache.remove(key);
                lastUpdated.remove(key);
            }

            StatisticsManager manager = new StatisticsManager();
            manager.storeStatisticalIntervals(intervals);

            System.out.println("Statistics cache includes " + cache.size() + " entries after Persist.");
        }
    }
}
