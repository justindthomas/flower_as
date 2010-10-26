/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package name.justinthomas.flower.analysis.persistence;

import com.sleepycat.persist.model.Entity;
import com.sleepycat.persist.model.PrimaryKey;
import name.justinthomas.flower.analysis.services.xmlobjects.XMLUser;

/**
 *
 * @author justin
 */
@Entity
public class PersistentUser {

    @PrimaryKey
    public String username;
    public String hashedPassword;
    public String fullName;
    public Boolean administrator;

    private PersistentUser() { }

    public PersistentUser(String username, String hashedPassword, String fullName, Boolean administrator) {
        this.username = username;
        this.hashedPassword = hashedPassword;
        this.fullName = fullName;
        this.administrator = administrator;
    }

    public XMLUser toXmlUser(Boolean withPassword) {
        XMLUser xuser = new XMLUser();

        xuser.user = this.username;
        if(withPassword) xuser.password = this.hashedPassword;
        xuser.fullName = fullName;
        xuser.administrator = administrator;
        
        return xuser;
    }
}
