/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.authentication;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.util.HashMap;
import java.util.Map;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class DirectoryDomain {

    @PrimaryKey
    public String domain;
    // <group name, privileged>
    public Map<String, Boolean> groups;

    public DirectoryDomain() {

    }

    public DirectoryDomain(String domain, String group, Boolean privileged) {
        this.domain = domain;
        this.groups = new HashMap();
        this.groups.put(group, privileged);
    }
}
