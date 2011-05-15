package name.justinthomas.flower.analysis.services;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import javax.ejb.EJB;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import name.justinthomas.flower.analysis.authentication.ActiveDirectory;
import name.justinthomas.flower.analysis.authentication.AuthenticationToken;
import name.justinthomas.flower.analysis.persistence.PersistentUser;
import name.justinthomas.flower.analysis.persistence.UserManager;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 *
 * @author justin
 */
@WebService()
public class UserAction {

    /**
     * Web service operation
     */
    @WebMethod(operationName = "authenticate")
    public AuthenticationToken authenticate(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = internal(username, password);
        if (token.authorized) {
            return token;
        }

        return external(username, password);
    }

    private AuthenticationToken external(String username, String password) {
        AuthenticationToken token = new AuthenticationToken();
        if ((username != null) && (password != null)) {
            ActiveDirectory activeDirectory = new ActiveDirectory(username, password);
            token = activeDirectory.authenticate();
        }

        if (token.authenticated) {
            System.out.println("Successfully authenticated against Active Directory");
        }

        if (token.authorized) {
            System.out.println("Successfully authorized against Active Directory");
        }

        return token;
    }

    private AuthenticationToken internal(String username, String password) {
        AuthenticationToken token = new AuthenticationToken();
        token.internal = true;

        UserManager userManager = new UserManager();
        
        if ((username != null) && (password != null) && (userManager != null)) {
            PersistentUser user = userManager.getUser(username);
            if (user == null) {
                System.out.println("Invalid User: " + username);
                return token;
            }

            Security.addProvider(new BouncyCastleProvider());
            MessageDigest hash = null;

            try {
                hash = MessageDigest.getInstance("SHA-256", "BC");
                hash.update(password.getBytes());
            } catch (NoSuchAlgorithmException nsae) {
                nsae.printStackTrace();
            } catch (NoSuchProviderException nspe) {
                nspe.printStackTrace();
            }


            if (user.hashedPassword.equals(new String(hash.digest()))) {
                token.authenticated = true;
                token.authorized = true;
                if (user.administrator) {
                    token.administrator = true;
                }
                token.fullName = user.fullName;
                token.timeZone = user.timeZone;
                System.out.println("Authenticated successfully against internal database: " + username);
                return token;
            }
        }
        System.out.println("Failed Authentication: " + username);
        return token;
    }

    @WebMethod(operationName = "administrator")
    public Boolean administrator(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = authenticate(username, password);
        if (token.authorized) {
            return token.administrator;
        }

        return false;
    }

    @WebMethod(operationName = "getFullName")
    public String getFullName(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {

        AuthenticationToken token = authenticate(username, password);
        if (token.authorized) {
            return token.fullName;
        }

        return null;
    }
    
    @WebMethod(operationName = "getTimeZone")
    public String getTimeZone(
            @WebParam(name = "username") String username,
            @WebParam(name = "password") String password) {
        
        AuthenticationToken token = authenticate(username, password);
        if (token.authorized) {
            return token.timeZone;
        }

        return null;  
    }

    @WebMethod(operationName = "updateUser")
    public Boolean updateUser(
            @WebParam(name = "authUser") String authUser,
            @WebParam(name = "authPassword") String authPassword,
            @WebParam(name = "updatedPassword") String updatedPassword,
            @WebParam(name = "fullName") String fullName,
            @WebParam(name = "timeZone") String timeZone) {

        AuthenticationToken token = authenticate(authUser, authPassword);
        
        UserManager userManager = new UserManager();
        if (token.authorized && token.internal && (userManager.getUser(authUser) != null)) {
            if (!userManager.updateUser(authUser, updatedPassword, fullName, false, timeZone)) {
                return false;
            }
        } else {
            return false;
        }

        return true;

    }
}
