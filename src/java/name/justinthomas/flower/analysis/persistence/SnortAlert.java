package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class SnortAlert implements Serializable {

    private Long id;
    private String accountId;
    private Long date;
    private Long usec;
    private String sourceAddress;
    private String destinationAddress;
    private Integer sourcePort;
    private Integer destinationPort;

    // Snort-specific Fields
    private String alert;
    private String packet;

    public SnortAlert() {

    }
    
    public SnortAlert(Long date, Long usec, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String alert, String packet) {
        this.date = date;
        this.usec = usec;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.alert = alert;
        this.packet = packet;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(date);
        builder.append(":");
        builder.append(sourceAddress);
        builder.append(":");
        builder.append(alert);
        return builder.toString();
    }
    
    @Id
    @GeneratedValue
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public String getAlert() {
        return alert;
    }

    public void setAlert(String alert) {
        this.alert = alert;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public Integer getDestinationPort() {
        return destinationPort;
    }

    public void setDestinationPort(Integer destinationPort) {
        this.destinationPort = destinationPort;
    }

    public String getPacket() {
        return packet;
    }

    public void setPacket(String packet) {
        this.packet = packet;
    }

    public String getSourceAddress() {
        return sourceAddress;
    }

    public void setSourceAddress(String sourceAddress) {
        this.sourceAddress = sourceAddress;
    }

    public Integer getSourcePort() {
        return sourcePort;
    }

    public void setSourcePort(Integer sourcePort) {
        this.sourcePort = sourcePort;
    }

    public Long getUsec() {
        return usec;
    }

    public void setUsec(Long usec) {
        this.usec = usec;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
