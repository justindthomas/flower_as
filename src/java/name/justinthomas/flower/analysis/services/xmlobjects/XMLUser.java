/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.services.xmlobjects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLUser {
    @XmlElement
    public String       user = null;
    @XmlElement
    public String       password = null;
    @XmlElement
    public String       fullName = null;
    @XmlElement
    public Boolean      administrator = false;
}
