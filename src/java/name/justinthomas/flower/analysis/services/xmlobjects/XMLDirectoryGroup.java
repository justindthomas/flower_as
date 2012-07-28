package name.justinthomas.flower.analysis.services.xmlobjects;

import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLDirectoryGroup {
    public String name;
    public Boolean privileged = false;
}