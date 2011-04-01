/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
public class XMLFlowSet {
    @XmlElement
    public List<PersistentFlow> flows = new ArrayList();
    @XmlElement
    public String tracker;
    @XmlElement
    public Boolean finished = false;
}
