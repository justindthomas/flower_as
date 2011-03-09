/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
    public Date date = null;
    @XmlElement
    public Long total = null;
    @XmlElement
    public Long tcp = null;
    @XmlElement
    public Long udp = null;
    @XmlElement
    public Long icmp = null;
    @XmlElement
    public Long icmpv6 = null;
    @XmlElement
    public Long ipsec = null;
    @XmlElement
    public Long sixinfour = null;
    @XmlElement
    public Long ipv4 = null;
    @XmlElement
    public Long ipv6 = null;
}
