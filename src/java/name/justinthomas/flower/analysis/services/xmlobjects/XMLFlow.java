/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.services.xmlobjects;

import java.math.BigDecimal;
import java.util.Calendar;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLFlow {
    @XmlElement
    public Integer      id = null;
    @XmlElement
    public Long     startTimeStamp = null;
    @XmlElement
    public Long     lastTimeStamp = null;
    @XmlElement
    public String       ethernetType = null;
    @XmlElement
    public String       reportedBy = null;
    @XmlElement
    public String       sourceAddress = null;
    @XmlElement
    public String       destinationAddress = null;
    @XmlElement
    public Integer      sourcePort = null;
    @XmlElement
    public Integer      destinationPort = null;
    @XmlElement
    public Integer      flags = null;
    @XmlElement
    public Integer      protocol = null;
    @XmlElement
    public Integer      packetsSent = null;
    @XmlElement
    public Integer      packetsReceived = null;
    @XmlElement
    public Long         bytesSent = 0l;
    @XmlElement
    public Long         bytesReceived = 0l;
}
