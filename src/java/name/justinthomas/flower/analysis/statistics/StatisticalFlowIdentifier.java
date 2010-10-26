package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Persistent;

/**
 *
 * @author justin
 */
@Persistent
public class StatisticalFlowIdentifier {
    String source;
    String destination;

    protected StatisticalFlowIdentifier() {

    }
    
    public StatisticalFlowIdentifier(String source, String destination) {
        this.source = source;
        this.destination = destination;
    }

    @Override
    public String toString() {
        return(source + ":" + destination);
    }

    @Override
    public boolean equals(Object obj) {
        if(obj instanceof StatisticalFlowIdentifier) {
            StatisticalFlowIdentifier tuple = (StatisticalFlowIdentifier)obj;
            if(tuple.source.equals(this.source) && tuple.destination.equals(this.destination)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 37 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 43 * hash + (this.destination != null ? this.destination.hashCode() : 0);
        return hash;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}

