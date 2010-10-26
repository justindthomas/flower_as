package name.justinthomas.flower.analysis.services.xmlobjects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLDirectoryGroup {
    @XmlElement
    public String name;
    @XmlElement
    public Boolean privileged = false;
}
