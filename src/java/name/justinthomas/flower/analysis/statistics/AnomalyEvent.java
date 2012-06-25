package name.justinthomas.flower.analysis.statistics;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlEnum;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class AnomalyEvent implements Serializable {
    public String source;
    public String destination;
    public Anomaly anomaly;
    public Integer basis;
    
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
    
    public AnomalyEvent(String source, String destination, Anomaly anomaly, Integer basis) {
        this.source = source;
        this.destination = destination;
        this.anomaly = anomaly;
        this.basis = basis;
    }
}
