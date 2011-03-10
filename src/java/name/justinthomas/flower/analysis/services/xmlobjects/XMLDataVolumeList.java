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
public class XMLDataVolumeList {
    @XmlElement
    public List<XMLDataVolume> bins = new ArrayList<XMLDataVolume>();
    @XmlElement
    public Boolean ready = false;
}
