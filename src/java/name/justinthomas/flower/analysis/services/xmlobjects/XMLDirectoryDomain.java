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
public class XMLDirectoryDomain {
    @XmlElement
    public String domain;
    @XmlElement
    public List<XMLDirectoryGroup> groups = new ArrayList();
}

