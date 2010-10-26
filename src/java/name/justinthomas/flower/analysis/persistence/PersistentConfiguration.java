/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import com.sleepycat.persist.model.Relationship;
import com.sleepycat.persist.model.SecondaryKey;
import java.util.HashMap;

/**
 *
 * @author justin
 */
@Entity
public class PersistentConfiguration {

    @PrimaryKey(sequence = "ID")
    public Long id;
    @SecondaryKey(relate = Relationship.MANY_TO_ONE)
    public Boolean selected = true;
    public String flowDirectory = "flows";
    public String frequencyDirectory = "frequency";
    public String statisticsDirectory = "statistics";
    public String userDirectory = "users";

    // <Network Address/CIDR, Description>
    public HashMap<String, String> managedNetworks = new HashMap();

    // <domain, <group name, privileged boolean>>
    public HashMap<String, HashMap<String, Boolean>> directoryDomains = new HashMap();

    // permits the use of unencrypted LDAP for Active Directory authentication
    public Boolean unsafeLdap = false;

    public HashMap<String, Long> resolution = new HashMap();

    public PersistentConfiguration() {
        resolution.put("High",         10000l);
        resolution.put("MediumHigh",   100000l);
        resolution.put("MediumLow",    1000000l);
        resolution.put("Low",          10000000l);
    }
}
