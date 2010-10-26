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
public class XMLNetworkList {
    @XmlElement
    public List<XMLNetwork> networks = new ArrayList();
    @XmlElement
    public Boolean ready = false;
    @XmlElement
    public Integer flowsProcessed = 0;

}
