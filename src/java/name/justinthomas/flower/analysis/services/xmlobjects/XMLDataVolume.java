package name.justinthomas.flower.analysis.services.xmlobjects;

import java.util.Date;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLDataVolume {
    @XmlElement
    public Date date;
    @XmlElement
    public Long duration;
    @XmlElement
    public Long total;
    @XmlElement
    public Long tcp;
    @XmlElement
    public Long udp;
    @XmlElement
    public Long icmp;
    @XmlElement
    public Long icmpv6;
    @XmlElement
    public Long ipsec;
    @XmlElement
    public Long sixinfour;
    @XmlElement
    public Long ipv4;
    @XmlElement
    public Long ipv6;
}
