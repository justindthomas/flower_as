/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.services.xmlobjects;

import java.util.Calendar;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLPacket {
    @XmlElement
    public Calendar     timeStamp = null;
    @XmlElement
    public Integer      protocol = null;
    @XmlElement
    public String       source = null;
    @XmlElement
    public Integer      sourcePort = null;
    @XmlElement
    public String       destination = null;
    @XmlElement
    public Integer      destinationPort = null;
    @XmlElement
    public Integer      length = null;
    @XmlElement
    public String       flags = null;
    @XmlElement
    public Long         seq = null;
    @XmlElement
    public Long         ack = null;
    @XmlElement
    public String       idHash = null;
    @XmlElement
    public String       payloadHash = null;
}
