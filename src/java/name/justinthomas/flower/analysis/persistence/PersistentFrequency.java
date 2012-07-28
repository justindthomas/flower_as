package name.justinthomas.flower.analysis.persistence;

import java.io.Serializable;
import java.util.HashMap;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 *
 * @author justin
 */
@Entity
public class PersistentFrequency implements Serializable {

    private Long id;
    private String accountId;
    private HashMap<TransportPairing, Integer> frequencyMap = new HashMap();

    public PersistentFrequency() { }

    @Id
    @GeneratedValue
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    @Column(unique = true)
    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public HashMap<TransportPairing, Integer> getFrequencyMap() {
        return frequencyMap;
    }

    public void setFrequencyMap(HashMap<TransportPairing, Integer> frequencyMap) {
        this.frequencyMap = frequencyMap;
    }
}
