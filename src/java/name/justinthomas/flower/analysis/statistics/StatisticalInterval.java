package name.justinthomas.flower.analysis.statistics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.statistics.AnomalyEvent.Anomaly;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class StatisticalInterval implements Serializable {

    //@EmbeddedId
    //protected IntervalKey key;
    private Long id;
    private Long statisticalInterval;
    private Long resolution;
    private String accountId;
    private HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows;
    private ArrayList<Long> flowIDs;
    private ArrayList<AnomalyEvent> anomalies;

    @Id
    @GeneratedValue
    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StatisticalInterval addAnomaly(StatisticalFlowIdentifier id, Anomaly anomaly, Integer basis) {
        this.getAnomalies().add(new AnomalyEvent(id.getSource(), id.getDestination(), anomaly, basis));
        return this;
    }

    public StatisticalInterval addFlow(StatisticalFlow flow, Long flowID) {
        if (this.getFlows().containsKey(flow.id())) {
            this.getFlows().put(flow.id(), flow.addFlow(flow));
        } else {
            this.getFlows().put(flow.id(), flow);
        }

        this.getFlowIDs().add(flowID);

        return this;
    }

    public StatisticalInterval addInterval(StatisticalInterval statisticalInterval) {
        for (Entry<StatisticalFlowIdentifier, StatisticalFlow> entry : statisticalInterval.getFlows().entrySet()) {
            if (this.getFlows().containsKey(entry.getKey())) {
                this.getFlows().put(entry.getKey(), this.getFlows().get(entry.getKey()).addFlow(entry.getValue()));
            } else {
                this.getFlows().put(entry.getKey(), entry.getValue());
            }

            for (Long flowID : statisticalInterval.getFlowIDs()) {
                this.getFlowIDs().add(flowID);
            }
        }

        return this;
    }

    public ArrayList<AnomalyEvent> getAnomalies() {
        if(anomalies == null) {
            anomalies = new ArrayList();
        }
        
        return anomalies;
    }

    public void setAnomalies(ArrayList<AnomalyEvent> anomalies) {
        this.anomalies = anomalies;
    }

    public ArrayList<Long> getFlowIDs() {
        if(flowIDs == null) {
            flowIDs = new ArrayList();
        }
        return flowIDs;
    }

    public void setFlowIDs(ArrayList<Long> flowIDs) {
        this.flowIDs = flowIDs;
    }

    public HashMap<StatisticalFlowIdentifier, StatisticalFlow> getFlows() {
        if (flows == null) {
            flows = new HashMap();
        }
        
        return flows;
    }

    public void setFlows(HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows) {
        this.flows = flows;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Long getResolution() {
        return resolution;
    }

    public void setResolution(Long resolution) {
        this.resolution = resolution;
    }

    public Long getStatisticalInterval() {
        return statisticalInterval;
    }

    public void setStatisticalInterval(Long statisticalInterval) {
        this.statisticalInterval = statisticalInterval;
    }
}
