package name.justinthomas.flower.analysis.statistics;

import com.sleepycat.persist.model.Persistent;
import java.util.HashMap;
import java.util.Map.Entry;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
@Persistent
public class StatisticalFlow {

    String source;
    String destination;
    HashMap<StatisticalFlowDetail, Long> count = new HashMap<StatisticalFlowDetail, Long>();
   
    public StatisticalFlowIdentifier id() {
        return new StatisticalFlowIdentifier(source, destination);
    }

    public StatisticalFlow() {

    }

    public StatisticalFlow(Long spread, Flow flow) {  // Flow should be oriented to its original direction before it arrives here - I'M RETHINKING THIS
        if(spread == 0) spread = 1l;

        try {
            source = flow.getUnfixedSourceAddress().getHostAddress();
            destination = flow.getUnfixedDestinationAddress().getHostAddress();

            StatisticalFlowDetail bytes = new StatisticalFlowDetail(StatisticalFlowDetail.Count.BYTE, flow.protocol, flow.getUnfixedSourcePort(), flow.getUnfixedDestinationPort());
            StatisticalFlowDetail packets = new StatisticalFlowDetail(StatisticalFlowDetail.Count.PACKET, flow.protocol, flow.getUnfixedSourcePort(), flow.getUnfixedDestinationPort());

            if (count.containsKey(bytes)) {
                count.put(bytes, (flow.bytesSent.longValue() / spread) + count.get(bytes));
            } else {
                count.put(bytes, (flow.bytesSent.longValue() / spread));
            }

            if (count.containsKey(packets)) {
                count.put(packets, (flow.packetsSent.longValue() / spread) + count.get(packets));
            } else {
                count.put(packets, (flow.packetsSent.longValue() / spread));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public StatisticalFlow addFlow(StatisticalFlow flow) {
        if(this.destination.equals(flow.destination) && this.source.equals(flow.source)) {
            for(Entry<StatisticalFlowDetail, Long> entry : flow.count.entrySet()) {
                if(this.count.containsKey(entry.getKey())) {
                    this.count.put(entry.getKey(), entry.getValue() + this.count.get(entry.getKey()));
                } else {
                    this.count.put(entry.getKey(), entry.getValue());
                }
            }
            return this;
        }
        return null;
    }

    public String getDestination() {
        return destination;
    }

    public String getSource() {
        return source;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public HashMap<StatisticalFlowDetail, Long> getCount() {
        return count;
    }

    public void setCount(HashMap<StatisticalFlowDetail, Long> count) {
        this.count = count;
    }
}
