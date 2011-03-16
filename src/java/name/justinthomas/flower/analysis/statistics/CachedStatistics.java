package name.justinthomas.flower.analysis.statistics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
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
    private static Map<AddressPair, Representation> sourceMap = Collections.synchronizedMap(new HashMap());
    public static final long MAX_WAIT = 300000;

    static class Representation {
        InetAddress collector;
        Date expiration = new Date();

        public Representation(InetAddress collector) {
            this.collector = collector;
            expiration.setTime(expiration.getTime() + 300000);
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Representation other = (Representation) obj;
            if (this.collector != other.collector && (this.collector == null || !this.collector.equals(other.collector))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 43 * hash + (this.collector != null ? this.collector.hashCode() : 0);
            return hash;
        }

        public Boolean isExpired() {
            if(new Date().after(expiration)) {
                return true;
            }

            return false;
        }
    }

    static class AddressPair {
        InetAddress source, destination;

        public AddressPair(InetAddress source, InetAddress destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (getClass() != obj.getClass()) {
                return false;
            }

            final AddressPair other = (AddressPair) obj;
            if (this.source != other.source && (this.source == null || !this.source.equals(other.source))) {
                return false;
            }

            if (this.destination != other.destination && (this.destination == null || !this.destination.equals(other.destination))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 73 * hash + (this.source != null ? this.source.hashCode() : 0);
            hash = 73 * hash + (this.destination != null ? this.destination.hashCode() : 0);
            return hash;
        }
    }

    public static CachedStatistics getInstance() {
        if(instance == null) {
            instance = new CachedStatistics();
        }

        return instance;
    }

    @PostConstruct
    protected void setup() {
        System.out.println("Setting CachedStatistics Persist to run every 60 seconds.");

        instance = new CachedStatistics();
        executor = new ScheduledThreadPoolExecutor(3);
        executor.scheduleAtFixedRate(new Task(), 60, 60, TimeUnit.SECONDS);
    }

    public static void put(IntervalKey key, StatisticalInterval interval) {
        //System.out.println("Putting: " + key.interval + ", " + key.resolution);
        lastUpdated.put(key, new Date());

        if(cache.containsKey(key)) {
            //System.out.println("Adding: " + interval.flows.size() + " flows to: " + key.interval + ", " + key.resolution);
            cache.put(key, cache.get(key).addInterval(interval));
        } else {
            cache.put(key, interval);
        }
    }

    public static Integer getCacheSize() {
        return cache.size();
    }

    public static Integer getRepresentationMapSize() {
        return sourceMap.size();
    }

    class Task implements Runnable {
        @Override
        public void run() {
            Thread thread = new Thread(new Persist(false));
            thread.start();
        }
    }

    class Persist implements Runnable {
        private Boolean flush;

        public Persist(Boolean flush) {
            this.flush = flush;
        }

        @Override
        public void run() {
            //System.out.println("Statistics cache includes " + cache.size() + " entries before Persist.");
            ArrayList<IntervalKey> keys = new ArrayList();
            for(IntervalKey key : lastUpdated.keySet()) {
                if((new Date().getTime() - lastUpdated.get(key).getTime() > MAX_WAIT) || flush) {
                    keys.add(key);
                }
            }

            ArrayList<StatisticalInterval> intervals = new ArrayList();

            //System.out.println("Removing intervals from cache.");
            for(IntervalKey key : keys) {
                intervals.add(cache.get(key));
                cache.remove(key);
                lastUpdated.remove(key);
            }

            StatisticsManager manager = new StatisticsManager();
            manager.storeStatisticalIntervals(intervals);

            //System.out.println("Statistics cache includes " + cache.size() + " entries after Persist.");
        }
    }

    public static InetAddress getRepresentation(InetAddress source, InetAddress destination) {
        return sourceMap.get(new AddressPair(source, destination)).collector;
    }
    
    public static Boolean hasOtherRepresentation(InetAddress source, InetAddress destination, InetAddress collector) {
        if(sourceMap.containsKey(new AddressPair(source, destination))) {
            if(sourceMap.get(new AddressPair(source, destination)).equals(new Representation(collector))) {
                sourceMap.put(new AddressPair(source, destination), new Representation(collector));
                return false;
            } else {
                if(sourceMap.get(new AddressPair(source, destination)).isExpired()) {
                    sourceMap.put(new AddressPair(source, destination), new Representation(collector));
                    return false;
                }
            }
        } else {
            sourceMap.put(new AddressPair(source, destination), new Representation(collector));
            return false;
        }
        
        return true;
    }

    @PreDestroy
    public void cleanup() {
        Persist persist = new Persist(true);
        persist.run();
    }
}
