package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.HashMap;
import java.util.Map.Entry;

/**
 *
 * @author justin
 */

@Entity(version = 100)
public class StatisticalInterval {

    @PrimaryKey
    public IntervalKey key;
    public HashMap<StatisticalFlowIdentifier, StatisticalFlow> flows = new HashMap();

    public void clear() {
        key = null;
        flows.clear();
    }

    public StatisticalInterval addFlow(StatisticalFlow flow) {
        if (flows.containsKey(flow.id())) {
            flows.put(flow.id(), flow.addFlow(flow));
        } else {
            flows.put(flow.id(), flow);
        }

        return this;
    }

    public StatisticalInterval addSecond(StatisticalInterval statisticalSecond) {
        for (Entry<StatisticalFlowIdentifier, StatisticalFlow> entry : statisticalSecond.flows.entrySet()) {
            if (flows.containsKey(entry.getKey())) {
                flows.put(entry.getKey(), flows.get(entry.getKey()).addFlow(entry.getValue()));
            } else {
                flows.put(entry.getKey(), entry.getValue());
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
}
