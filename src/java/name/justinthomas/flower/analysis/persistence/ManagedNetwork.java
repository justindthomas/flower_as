/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class ManagedNetwork {

    @PrimaryKey
    public String address;
    public String description;

    public ManagedNetwork() {

    }

    public ManagedNetwork(String address, String description) throws UnknownHostException {
        InetAddress.getByName(address.split("/")[0]);
        this.address = address;
        this.description = description;
    }
}
