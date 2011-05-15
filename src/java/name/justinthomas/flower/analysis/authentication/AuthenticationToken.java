package name.justinthomas.flower.analysis.authentication;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class AuthenticationToken {
    @XmlElement
    public boolean internal = false;
    @XmlElement
    public boolean authenticated = false;
    @XmlElement
    public boolean authorized = false;
    @XmlElement
    public boolean administrator = false;
    @XmlElement
    public String distinguishedName = null;
    @XmlElement
    public String fullName = null;
    @XmlElement
    public String timeZone = null;
}
