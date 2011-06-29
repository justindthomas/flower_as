package name.justinthomas.flower.analysis.statistics;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import name.justinthomas.flower.manager.services.CustomerAdministration.Customer;

/**
 *
 * @author justin
 */
public class CachedStatistics {

    private Customer customer;
    private static ScheduledThreadPoolExecutor executor;
    private Map<IntervalKey, StatisticalInterval> cache;
    private Map<IntervalKey, Date> lastUpdated;
    private Map<AddressPair, Representation> sourceMap;
    private StatisticalEngine engine = new StatisticalEngine();
    public static final long MAX_WAIT = 300000;

    public CachedStatistics(Customer customer) {
        this.customer = customer;
        System.out.println("Setting CachedStatistics Persist to run every 60 seconds.");
        //cache = Collections.synchronizedMap(new HashMap());
        cache = new ConcurrentHashMap();
        
        //lastUpdated = Collections.synchronizedMap(new HashMap());
        lastUpdated = new ConcurrentHashMap();
        
        //sourceMap = Collections.synchronizedMap(new HashMap());
        sourceMap = new ConcurrentHashMap();
        
        executor = new ScheduledThreadPoolExecutor(3);

        executor.scheduleAtFixedRate(new Task(customer), 60, 60, TimeUnit.SECONDS);
    }

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
            if (new Date().after(expiration)) {
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

    public void put(IntervalKey key, StatisticalInterval interval) {
        //System.out.println("Putting: " + key.interval + ", " + key.resolution);
        this.lastUpdated.put(key, new Date());

        if (this.cache.containsKey(key)) {
            //System.out.println("Adding: " + interval.flows.size() + " flows to: " + key.interval + ", " + key.resolution);
            this.cache.put(key, this.cache.get(key).addInterval(interval));
        } else {
            this.cache.put(key, interval);
        }
    }

    public Integer getCacheSize() {
        return this.cache.size();
    }

    public Integer getRepresentationMapSize() {
        return this.sourceMap.size();
    }

    class Task implements Runnable {

        private Customer customer;

        public Task(Customer customer) {
            this.customer = customer;
        }

        @Override
        public void run() {
            Thread thread = new Thread(new Persist(customer, false));
            thread.start();
        }
    }

    class Persist implements Runnable {

        private Boolean flush;
        private Customer customer;

        public Persist(Customer customer, Boolean flush) {
            this.flush = flush;
            this.customer = customer;
        }

        @Override
        public void run() {
            System.out.println("Statistics cache includes " + cache.size() + " entries before Persist.");
            ArrayList<IntervalKey> keys = new ArrayList();
            for (IntervalKey key : lastUpdated.keySet()) {
                if ((new Date().getTime() - lastUpdated.get(key).getTime() > MAX_WAIT) || flush) {
                    keys.add(key);
                }
            }

            ArrayList<StatisticalInterval> intervals = new ArrayList();

            System.out.println("Removing intervals from cache.");
            for (IntervalKey key : keys) {
                intervals.add(engine.addStatisticalInterval(customer, cache.remove(key)));
                lastUpdated.remove(key);
            }

            StatisticsManager statisticsManager = new StatisticsManager(customer);
            statisticsManager.storeStatisticalIntervals(intervals);

            System.out.println("Statistics cache includes " + cache.size() + " entries after Persist.");
        }
    }

    public InetAddress getRepresentation(InetAddress source, InetAddress destination) {
        return this.sourceMap.get(new AddressPair(source, destination)).collector;
    }

    public Boolean hasOtherRepresentation(InetAddress source, InetAddress destination, InetAddress collector) {
        if (this.sourceMap.containsKey(new AddressPair(source, destination))) {
            if (this.sourceMap.get(new AddressPair(source, destination)).equals(new Representation(collector))) {
                this.sourceMap.put(new AddressPair(source, destination), new Representation(collector));
                return false;
            } else {
                if (this.sourceMap.get(new AddressPair(source, destination)).isExpired()) {
                    this.sourceMap.put(new AddressPair(source, destination), new Representation(collector));
                    return false;
                }
            }
        } else {
            this.sourceMap.put(new AddressPair(source, destination), new Representation(collector));
            return false;
        }

        return true;
    }

    public void close() {
        Persist persist = new Persist(customer, true);
        persist.run();
    }
}
