package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Persistent;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Persistent
@XmlType
public class AnomalyEvent {
    public String source;
    public String destination;
    public Anomaly anomaly;

    @XmlEnum
    public static enum Anomaly {

        EWMA_INCREASE,
        EWMA_DECREASE,
        NEW_MAXIMUM,
        NEW_MINIMUM,
        NEW_FLOW
    }
    
    public AnomalyEvent() {
        
    }
    
    public AnomalyEvent(String source, String destination, Anomaly anomaly) {
        this.source = source;
        this.destination = destination;
        this.anomaly = anomaly;
    }
}
