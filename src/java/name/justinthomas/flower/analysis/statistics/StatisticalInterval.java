package name.justinthomas.flower.analysis.statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.statistics.AnomalyEvent.Anomaly;

/**
 *
 * @author justin
 */

@Entity
@XmlType
public class StatisticalInterval implements Serializable {

    @EmbeddedId
    protected IntervalKey key;
    private HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows = new HashMap();
    private ArrayList<Long> flowIDs = new ArrayList();
    private ArrayList<AnomalyEvent> anomalies = new ArrayList();

    public void clear() {
        key = null;
        flows.clear();
    }
    
    public StatisticalInterval addAnomaly(StatisticalFlowIdentifier id, Anomaly anomaly, Integer basis) {
        anomalies.add(new AnomalyEvent(id.getSource(), id.getDestination(), anomaly, basis));
        return this;
    }

    public StatisticalInterval addFlow(StatisticalFlow flow, Long flowID) {
        if (flows.containsKey(flow.id())) {
            flows.put(flow.id(), flow.addFlow(flow));
        } else {
            flows.put(flow.id(), flow);
        }

        flowIDs.add(flowID);

        return this;
    }

    public StatisticalInterval addInterval(StatisticalInterval statisticalInterval) {
        for (Entry<StatisticalFlowIdentifier, StatisticalFlow> entry : statisticalInterval.flows.entrySet()) {
            if (flows.containsKey(entry.getKey())) {
                flows.put(entry.getKey(), flows.get(entry.getKey()).addFlow(entry.getValue()));
            } else {
                flows.put(entry.getKey(), entry.getValue());
            }

            for(Long flowID : statisticalInterval.flowIDs) {
                flowIDs.add(flowID);
            }
        }

        return this;
    }

    public ArrayList<AnomalyEvent> getAnomalies() {
        return anomalies;
    }

    public void setAnomalies(ArrayList<AnomalyEvent> anomalies) {
        this.anomalies = anomalies;
    }

    public ArrayList<Long> getFlowIDs() {
        return flowIDs;
    }

    public void setFlowIDs(ArrayList<Long> flowIDs) {
        this.flowIDs = flowIDs;
    }

    public HashMap<StatisticalFlowIdentifier, StatisticalFlow> getFlows() {
        return flows;
    }

    public void setFlows(HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows) {
        this.flows = flows;
    }

    public IntervalKey getKey() {
        return key;
    }

    public void setKey(IntervalKey key) {
        this.key = key;
    }
}
