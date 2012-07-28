package name.justinthomas.flower.analysis.services.xmlobjects;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@XmlType
public class XMLDataVolumeList {

    public static enum Error {

        INSUFFICIENT_DATA
    }
    
    public String tracker;
    public List<Error> errors = new ArrayList();
    public List<XMLDataVolume> bins = new ArrayList();  
    public Boolean ready = false;
}
