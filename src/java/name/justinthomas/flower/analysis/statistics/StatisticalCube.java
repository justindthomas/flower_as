package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import name.justinthomas.flower.analysis.statistics.StatisticalEngine.Cube;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math.stat.descriptive.SynchronizedDescriptiveStatistics;

/**
 *
 * @author justin
 */
@Entity
public class StatisticalCube {
    @PrimaryKey
    private String customerID;
    private HashMap<String, HashMap<String, HashMap<Cube, double[]>>> statistics;
    
    public StatisticalCube() {
        statistics = new HashMap();
    }

    public StatisticalCube(String customerID, ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> statistics) {
        this();
        
        this.customerID = customerID;
        
        for(String source : statistics.keySet()) {
            if(!this.statistics.containsKey(source)) {
                this.statistics.put(source, new HashMap());
            }
            
            for(String destination : statistics.get(source).keySet()) {
                if(!this.statistics.get(source).containsKey(destination)) {
                    this.statistics.get(source).put(destination, new HashMap());
                }
                
                for(Cube cube : statistics.get(source).get(destination).keySet()) {
                    this.statistics.get(source).get(destination).put(cube, statistics.get(source).get(destination).get(cube).getValues());
                }
            }
        }
    }
    
    
    public StatisticalCube(String customerID) {
        this();
        
        this.customerID = customerID;
    }

    public String getCustomerID() {
        return customerID;
    }

    public void setCustomerID(String customerID) {
        this.customerID = customerID;
    }

    public HashMap<String, HashMap<String, HashMap<Cube, double[]>>> getStatistics() {
        return statistics;
    }
    
    public ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> getConcurrentStatistics() {
        ConcurrentHashMap<String, ConcurrentHashMap<String, ConcurrentHashMap<Cube, DescriptiveStatistics>>> map = new ConcurrentHashMap();
        
        for(String source : statistics.keySet()) {
            if(!map.containsKey(source)) {
                map.put(source, new ConcurrentHashMap());
            }
            
            for(String destination : statistics.get(source).keySet()) {
                if(!map.get(source).containsKey(destination)) {
                    map.get(source).put(destination, new ConcurrentHashMap());
                }
                
                for(Cube cube : statistics.get(source).get(destination).keySet()) {
                    DescriptiveStatistics descriptiveStatistics = new SynchronizedDescriptiveStatistics();
                    for(double d : statistics.get(source).get(destination).get(cube)) {
                        descriptiveStatistics.addValue(d);
                    }
                    map.get(source).get(destination).put(cube, descriptiveStatistics);
                }
            }
        }
        
        return map;
    }

    public void setStatistics(HashMap<String, HashMap<String, HashMap<Cube, double[]>>> statistics) {
        this.statistics = statistics;
    }
}
