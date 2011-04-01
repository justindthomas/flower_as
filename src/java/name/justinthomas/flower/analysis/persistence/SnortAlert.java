package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class SnortAlert {

    @XmlElement
    @PrimaryKey(sequence="ID")
    long id;

    @XmlElement
    @SecondaryKey(relate=Relationship.MANY_TO_ONE)
    public Long date;
    
    @XmlElement
    public Long usec;
    @XmlElement
    public String sourceAddress;
    @XmlElement
    public String destinationAddress;
    @XmlElement
    public Integer sourcePort;
    @XmlElement
    public Integer destinationPort;

    // Snort-specific Fields
    @XmlElement
    public String alert;
    @XmlElement
    public String packet;

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
}
