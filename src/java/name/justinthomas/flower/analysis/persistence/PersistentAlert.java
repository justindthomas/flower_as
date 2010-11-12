/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class PersistentAlert {

    @PrimaryKey(sequence="ID")
    long id;

    @XmlElement
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
    @XmlElement
    public String alert;
    @XmlElement
    public String packet;

    protected PersistentAlert() {

    }
    
    public PersistentAlert(Long date, Long usec, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String alert, String packet) {
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

        Date now = new Date();
        now.setTime(date);

        builder.append(date);
        builder.append(":");
        builder.append(sourceAddress);
        builder.append(":");
        builder.append(destinationAddress);
        builder.append(":");
        builder.append(alert);

        return builder.toString();
    }
}
