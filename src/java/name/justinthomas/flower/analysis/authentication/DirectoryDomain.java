/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.authentication;

import java.io.Serializable;
import java.util.HashMap;
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
public class DirectoryDomain implements Serializable {

    private Long id;
    private String accountId;
    private String domain;
    // <group name, privileged>
    private HashMap<String, Boolean> groups;

    public DirectoryDomain() {

    }

    public DirectoryDomain(String domain, String group, Boolean privileged) {
        this.domain = domain;
        this.groups = new HashMap();
        this.groups.put(group, privileged);
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public HashMap<String, Boolean> getGroups() {
        return groups;
    }

    public void setGroups(HashMap<String, Boolean> groups) {
        this.groups = groups;
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
