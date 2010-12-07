package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.Date;
import java.util.LinkedList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class PersistentAlert {

    public enum SourceType {
        SNORT,
        MODSECURITY
    }

    @XmlElement
    public SourceType type;

    @XmlElement
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

    // Snort-specific Fields
    @XmlElement
    public String alert;
    @XmlElement
    public String packet;

    // mod_security-specific Fields
    @XmlElement
    public String auditLogHeader;
    @XmlElement
    public String requestHeaders;
    @XmlElement
    public String requestBody;
    @XmlElement
    public String intermediateResponseBody;
    @XmlElement
    public String finalResponseHeaders;
    @XmlElement
    public String auditLogTrailer;
    @XmlElement
    public String requestBodyNoFiles;
    @XmlElement
    public LinkedList<String> matchedRules;

    public PersistentAlert() {

    }
    
    public PersistentAlert(Long date, Long usec, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String alert, String packet) {
        this.type = SourceType.SNORT;
        this.date = date;
        this.usec = usec;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.alert = alert;
        this.packet = packet;
    }

    public PersistentAlert(Long date, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String auditLogHeader, String requestHeaders, String requestBody, String intermediateResponseBody, String finalResponseHeaders, String auditLogTrailer, String requestBodyNoFiles, LinkedList<String> matchedRules) {
        this.type = SourceType.MODSECURITY;
        this.date = date;
        this.sourceAddress = sourceAddress;
        this.destinationAddress = destinationAddress;
        this.sourcePort = sourcePort;
        this.destinationPort = destinationPort;
        this.auditLogHeader = auditLogHeader;
        this.requestHeaders = requestHeaders;
        this.requestBody = requestBody;
        this.intermediateResponseBody = intermediateResponseBody;
        this.finalResponseHeaders = finalResponseHeaders;
        this.auditLogTrailer = auditLogTrailer;
        this.requestBodyNoFiles = requestBodyNoFiles;
        this.matchedRules = matchedRules;
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
