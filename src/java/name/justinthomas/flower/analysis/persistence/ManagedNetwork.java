package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class ManagedNetwork implements Serializable {

    private Long id;
    private String accountId;
    private String address;
    private String description;

    public ManagedNetwork() {

    }

    public ManagedNetwork(String address, String description) throws UnknownHostException {
        InetAddress.getByName(address.split("/")[0]);
        this.address = address;
        this.description = description;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Id
    @GeneratedValue
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
}
