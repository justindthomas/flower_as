/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.authentication;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import javax.xml.bind.annotation.XmlType;

/**
 *
 * @author justin
 */
@Entity
@XmlType
public class User {

    @PrimaryKey
    public String username;
    public String hashedPassword;
    public String fullName;
    public Boolean administrator;
    public String timeZone;

    private User() { }

    public User(String username, String hashedPassword, String fullName, Boolean administrator, String timeZone) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.administrator = administrator;
        this.timeZone = timeZone;
    }
    
    public User sanitize() {
        hashedPassword = null;
        return this;
    }
}
