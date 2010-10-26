/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Persistent;
import java.util.Date;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
@Persistent
public class PersistentFlow {

    public Date startTimeStamp = null;
    public Date lastTimeStamp = null;
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
            System.err.println("Error in HashTableFlow(Flow): " + e.getMessage());
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

    public void flip() {
        String temp = this.source;
        this.source = this.destination;
        this.destination = temp;

        Integer tempPort = this.sourcePort;
        this.sourcePort = this.destinationPort;
        this.destinationPort = tempPort;
    }
    private PersistentFlow() {
    }
}
