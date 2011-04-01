package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import java.util.LinkedList;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class ModSecurityAlert {
    @XmlElement
    @PrimaryKey(sequence="ID")
    long id;

    @XmlElement
    @SecondaryKey(relate=Relationship.MANY_TO_ONE)
    public Long date;

    @XmlElement
    public String sourceAddress;
    @XmlElement
    public String destinationAddress;
    @XmlElement
    public Integer sourcePort;
    @XmlElement
    public Integer destinationPort;

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

    public ModSecurityAlert() {

    }

    public ModSecurityAlert(Long date, String sourceAddress, String destinationAddress, Integer sourcePort, Integer destinationPort, String auditLogHeader, String requestHeaders, String requestBody, String intermediateResponseBody, String finalResponseHeaders, String auditLogTrailer, String requestBodyNoFiles, LinkedList<String> matchedRules) {
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
        builder.append(date);
        builder.append(":");
        builder.append(sourceAddress);
        builder.append(":");
        builder.append(auditLogHeader);
        return builder.toString();
    }
}
