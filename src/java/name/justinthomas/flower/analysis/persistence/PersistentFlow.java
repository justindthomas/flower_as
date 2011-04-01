/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.Date;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
@XmlType
@Entity(version = 11082010)
public class PersistentFlow {

    @PrimaryKey(sequence = "ID")
    Long id;
    private Date startTimeStamp = null;
    private Date lastTimeStamp = null;
    public String ethernetType = null;
    public String reportedBy = null;
    public String source = null;
    public String destination = null;
    public Integer sourcePort = null;
    public Integer destinationPort = null;
    public Integer protocol = null;
    public Integer packetCount = null;
    public Integer flags = null;
    public Long size = null;

    public PersistentFlow(Flow flow) {
        try {
            this.source = flow.getUnfixedSourceAddress().getHostAddress();
            this.destination = flow.getUnfixedDestinationAddress().getHostAddress();
            this.sourcePort = flow.getUnfixedSourcePort();
            this.destinationPort = flow.getUnfixedDestinationPort();
        } catch (Exception e) {
            e.printStackTrace();
        }


        this.ethernetType = flow.ethernetType;
        this.lastTimeStamp = flow.lastTimeStamp;
        this.startTimeStamp = flow.startTimeStamp;
        this.packetCount = flow.getPacketsSent();
        this.reportedBy = flow.reportedBy;

        this.size = flow.getBytesSent().longValue();
        this.protocol = flow.protocol;
        this.flags = flow.flags;
    }

    public PersistentFlow() {
    }

    public Long getLastTimeStampMs() {
        if (lastTimeStamp != null) {
            return lastTimeStamp.getTime();
        }

        return 0l;
    }

    public void setLastTimeStampMs(Long lastTimeStamp) {
        this.lastTimeStamp = new Date(lastTimeStamp);
    }

    public Long getStartTimeStampMs() {
        if (startTimeStamp != null) {
            return startTimeStamp.getTime();
        }

        return 0l;
    }

    public void setStartTimeStampMs(Long lastTimeStamp) {
        this.startTimeStamp = new Date(lastTimeStamp);
    }
}
