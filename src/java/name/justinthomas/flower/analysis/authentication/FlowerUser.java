package name.justinthomas.flower.analysis.authentication;

import java.io.Serializable;
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
public class FlowerUser implements Serializable {

    private Long id;
    private String accountId;
    private String username;
    private String hashedPassword;
    private String fullName;
    private Boolean administrator;
    private String timeZone;

    protected FlowerUser() { }

    public FlowerUser(String accountId, String username, String hashedPassword, String fullName, Boolean administrator, String timeZone) {
        this.accountId = accountId;
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.administrator = administrator;
        this.timeZone = timeZone;
    }
    
    @Id
    @GeneratedValue
    public Long getId() {
        return this.id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }

    public Boolean getAdministrator() {
        return administrator;
    }

    public void setAdministrator(Boolean administrator) {
        this.administrator = administrator;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getHashedPassword() {
        return hashedPassword;
    }

    public void setHashedPassword(String hashedPassword) {
        this.hashedPassword = hashedPassword;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }
    
    public FlowerUser sanitize() {
        hashedPassword = null;
        return this;
    }
}
