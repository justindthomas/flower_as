package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */

@Entity(version = 102)
@XmlType
public class StatisticalInterval {

    @PrimaryKey
    public IntervalKey key;
    public HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows = new HashMap();
    @XmlElement
    public LinkedList<Long> flowIDs = new LinkedList();

    public void clear() {
        key = null;
        flows.clear();
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

    public IntervalKey getSecond() {
        return key;
    }

    public void setSecond(IntervalKey second) {
        this.key = second;
    }

    public HashMap<StatisticalFlowIdentifier, StatisticalFlow> getFlows() {
        return flows;
    }
}
