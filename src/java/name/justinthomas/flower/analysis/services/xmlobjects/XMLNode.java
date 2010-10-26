/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.services.xmlobjects;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLNode {
    @XmlElement
    public String address;
    @XmlElement
    public String resolvedAddress;
    @XmlElement
    public List<XMLFlow> flowsOriginated = new ArrayList<XMLFlow>();
    @XmlElement
    public List<XMLFlow> flowsReceived = new ArrayList<XMLFlow>();
}
