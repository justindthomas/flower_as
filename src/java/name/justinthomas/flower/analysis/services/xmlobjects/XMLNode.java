package name.justinthomas.flower.analysis.services.xmlobjects;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import name.justinthomas.flower.analysis.persistence.PersistentFlow;

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
    public List<PersistentFlow> flowsOriginated = new ArrayList<PersistentFlow>();
    @XmlElement
    public List<PersistentFlow> flowsReceived = new ArrayList<PersistentFlow>();
}
