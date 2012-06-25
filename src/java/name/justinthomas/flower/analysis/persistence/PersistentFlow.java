/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;
import java.util.Date;
import javax.persistence.*;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.element.Flow;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class PersistentFlow implements Serializable {

    private Long id;
    private Date startTimeStamp;
    private Date lastTimeStamp;
    private String reportedBy;
    private String source;
    private String destination;
    private Integer sourcePort;
    private Integer destinationPort;
    private Integer protocol;
    private Integer packetCount;
    private Integer flags;
    private Long byteSize;
    private String accountId;

    public PersistentFlow(Flow flow) {
        try {
            this.source = flow.getUnfixedSourceAddress().getHostAddress();
            this.destination = flow.getUnfixedDestinationAddress().getHostAddress();
            this.sourcePort = flow.getUnfixedSourcePort();
            this.destinationPort = flow.getUnfixedDestinationPort();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.lastTimeStamp = flow.lastTimeStamp;
        this.startTimeStamp = flow.startTimeStamp;
        this.packetCount = flow.getPacketsSent();
        this.reportedBy = flow.reportedBy;

        this.byteSize = flow.getBytesSent().longValue();
        this.protocol = flow.protocol;
        this.flags = flow.flags;
        this.accountId = flow.getCustomer().getAccount();
    }

    public PersistentFlow() {
    }
    
    @Id
    @GeneratedValue
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    @Transient
    public Long getLastTimeStampMs() {
        if (lastTimeStamp != null) {
            return lastTimeStamp.getTime();
        }

        return 0l;
    }

    public void setLastTimeStampMs(Long lastTimeStamp) {
        this.lastTimeStamp = new Date(lastTimeStamp);
    }

    @Transient
    public Long getStartTimeStampMs() {
        if (startTimeStamp != null) {
            return startTimeStamp.getTime();
        }

        return 0l;
    }

    public void setStartTimeStampMs(Long lastTimeStamp) {
        this.startTimeStamp = new Date(lastTimeStamp);
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Integer destinationPort) {
        this.destinationPort = destinationPort;
    }

    public Integer getFlags() {
        return flags;
    }

    public void setFlags(Integer flags) {
        this.flags = flags;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getLastTimeStamp() {
        return lastTimeStamp;
    }

    public void setLastTimeStamp(Date lastTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
    }

    public Integer getPacketCount() {
        return packetCount;
    }

    public void setPacketCount(Integer packetCount) {
        this.packetCount = packetCount;
    }

    public Integer getProtocol() {
        return protocol;
    }

    public void setProtocol(Integer protocol) {
        this.protocol = protocol;
    }

    public String getReportedBy() {
        return reportedBy;
    }

    public void setReportedBy(String reportedBy) {
        this.reportedBy = reportedBy;
    }

    public Long getByteSize() {
        return byteSize;
    }

    public void setByteSize(Long byteSize) {
        this.byteSize = byteSize;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Integer sourcePort) {
        this.sourcePort = sourcePort;
    }

    @Temporal(javax.persistence.TemporalType.TIMESTAMP)
    public Date getStartTimeStamp() {
        return startTimeStamp;
    }

    public void setStartTimeStamp(Date startTimeStamp) {
        this.startTimeStamp = startTimeStamp;
    }
}
